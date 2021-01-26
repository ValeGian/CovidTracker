package it.unipi.dii.inginf.dsmt.covidtracker.ejb;

import com.google.gson.Gson;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationResponse;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.*;
import it.unipi.dii.inginf.dsmt.covidtracker.log.CTLogger;
import it.unipi.dii.inginf.dsmt.covidtracker.persistence.JavaErlServicesClientImpl;
import it.unipi.dii.inginf.dsmt.covidtracker.persistence.KVManagerImpl;
import javafx.util.Pair;
import org.json.simple.parser.ParseException;

import javax.annotation.Resource;
import javax.ejb.EJB;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class GenericAreaNode {

    @SuppressWarnings({"all"})
    @Resource(mappedName = "concurrent/__defaultManagedScheduledExecutorService")
    private ManagedScheduledExecutorService scheduler;

    @Resource(mappedName = "concurrent/__defaultManagedExecutorService")
    protected ManagedExecutorService executor;

    private final static int DAILY_REPORT_TIMEOUT = 60;

    protected KVManager myKVManager;

    private final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";

    @EJB private Producer myProducer;
    protected JavaErlServicesClient myErlangClient;
    @EJB protected HierarchyConnectionsRetriever myHierarchyConnectionsRetriever;

    protected AreaConsumer myConsumer;

    private final Runnable timeout = new Runnable() {
        @Override
        public void run() {
            DailyReport dailyReport = myConsumer.getDailyReport();
            saveDailyReport(dailyReport);
            sendDailyReportToParent(dailyReport);
        }
    };

    private final Gson gson = new Gson();

    protected String myName;
    protected String myDestinationName;

    private JMSConsumer myQueueConsumer;

    public GenericAreaNode(){}

    public String readReceivedMessages() { return myKVManager.getAllClientRequest(); }

    protected void setQueueConsumer(final String QUEUE_NAME) {
        try{
            Context ic = new InitialContext();
            Queue myQueue= (Queue)ic.lookup(QUEUE_NAME);
            QueueConnectionFactory qcf = (QueueConnectionFactory)ic.lookup(QC_FACTORY_NAME);
            myQueueConsumer = qcf.createContext().createConsumer(myQueue);
        }
        catch (final NamingException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    protected void startReceivingLoop() {
        final Runnable receivingLoop = new Runnable() {
            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Message inMsg = myQueueConsumer.receive();
                        handleMessage(inMsg);
                    }
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    CTLogger.getLogger(this.getClass()).info("Eccezione: " + sw.toString());

                }
            }
        };
        executor.execute(receivingLoop);
    }


    public void handleMessage(Message msg) {
        if (msg != null) {
            try {
                CommunicationMessage cMsg = (CommunicationMessage) ((ObjectMessage) msg).getObject();
                myKVManager.addClientRequest(cMsg.toString());

                Pair<String, CommunicationMessage> messageToSend;

                switch (cMsg.getMessageType()) {

                    case REGISTRY_CLOSURE_REQUEST:
                        List<Pair<String, CommunicationMessage>> returnList = myConsumer.handleRegistryClosureRequest(cMsg);
                        if (returnList != null)
                            for (Pair<String, CommunicationMessage> messageToSendL : returnList) {
                                myProducer.enqueue(myHierarchyConnectionsRetriever.getMyDestinationName(messageToSendL.getKey()), messageToSendL.getValue());
                            }
                        CTLogger.getLogger(this.getClass()).info("Sending Registry Closure Requests to Regions: \n" + returnList);
                        scheduler.schedule(timeout, DAILY_REPORT_TIMEOUT, TimeUnit.SECONDS);
                        break;

                    case AGGREGATION_REQUEST:
                        messageToSend = myConsumer.handleAggregationRequest(cMsg);
                        if (messageToSend != null) {
                            if (!messageToSend.getKey().equals("mySelf")) {
                                myProducer.enqueue(myHierarchyConnectionsRetriever.getMyDestinationName(messageToSend.getKey()), messageToSend.getValue(), msg.getJMSReplyTo());
                            }else {
                                handleAggregation((ObjectMessage) msg);
                            }
                        }
                        break;

                    case DAILY_REPORT:
                        myConsumer.handleDailyReport(cMsg);
                        break;

                    default:
                        break;
                }

            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                CTLogger.getLogger(this.getClass()).info("Eccezione: " + sw.toString());
            }
        }
    }

    private void handleAggregation(ObjectMessage msg) {
        try {
            CommunicationMessage cMsg = (CommunicationMessage) ((ObjectMessage) msg).getObject();
            AggregationRequest request = gson.fromJson(cMsg.getMessageBody(), AggregationRequest.class);

            double result = 0.0;

            CommunicationMessage outMsg = new CommunicationMessage();
            outMsg.setMessageType(MessageType.AGGREGATION_RESPONSE);
            outMsg.setSenderName(myName);
            AggregationResponse response = new AggregationResponse(request);

            if (request.getStartDay().equals(request.getLastDay())) {
                result = myKVManager.getDailyReport(request.getLastDay(), request.getType());
                if(result == -1.0 || request.getOperation().equals("standard_deviation") || request.getOperation().equals("variance"))
                    result = 0.0;

            } else {
                result = myKVManager.getAggregation(request);
                if (result == -1.0) {
                    try {
                        result = myErlangClient.computeAggregation(
                                request.getOperation(),
                                myKVManager.getDailyReportsInAPeriod(request.getStartDay(), request.getLastDay(), request.getType())
                        );
                        myKVManager.saveAggregation(request, result);
                    } catch (Exception e) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        CTLogger.getLogger(this.getClass()).info("Eccezione: " + sw.toString());
                        result = 0.0;
                    }
                }
            }
            response.setResult(result);
            outMsg.setMessageBody(gson.toJson(response));
            myProducer.enqueue(msg.getJMSReplyTo(), outMsg);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            CTLogger.getLogger(this.getClass()).info("Eccezione: " + sw.toString());
        }
    }

    private void saveDailyReport(final DailyReport dailyReport) { myKVManager.addDailyReport(dailyReport); }

    private void sendDailyReportToParent(final DailyReport dailyReport) {
        try {
            CommunicationMessage outMsg = new CommunicationMessage();
            outMsg.setSenderName(myName);
            outMsg.setMessageType(MessageType.DAILY_REPORT);
            outMsg.setMessageBody(gson.toJson(dailyReport));

            String parent = myHierarchyConnectionsRetriever.getMyDestinationName(myHierarchyConnectionsRetriever.getNationName());

            CTLogger.getLogger(this.getClass()).info("Sending " + myName + " Daily Report to " + parent + "\nReport: " + outMsg.toString());
            myProducer.enqueue(parent, outMsg);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            CTLogger.getLogger(this.getClass()).info("Eccezione: " + sw.toString());
        }
    }
}
