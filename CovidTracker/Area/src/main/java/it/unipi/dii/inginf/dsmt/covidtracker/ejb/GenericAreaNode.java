package it.unipi.dii.inginf.dsmt.covidtracker.ejb;

import com.google.gson.Gson;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationResponse;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.*;
import it.unipi.dii.inginf.dsmt.covidtracker.log.CTLogger;
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

    protected KVManagerImpl myKVManager;

    private final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";

    @EJB private Producer myProducer;
    @EJB private JavaErlServicesClient myErlangClient;
    @EJB protected HierarchyConnectionsRetriever myHierarchyConnectionsRetriever;

    //private ScheduledFuture<?> timeoutHandle = null;
    protected AreaConsumer myConsumer;

    private final Runnable timeout = () -> saveDailyReport(myConsumer.getDailyReport());

    boolean stop;
    private final Gson gson = new Gson();
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
                        CTLogger.getLogger(this.getClass()).info("MI METTO IN ASCOLTO (AREA)");
                        Message inMsg = myQueueConsumer.receive();
                        CommunicationMessage cMsg = (CommunicationMessage) ((ObjectMessage) inMsg).getObject();

                        CTLogger.getLogger(this.getClass()).info("Ho ricevuto " + cMsg.toString());
                        handleMessage(inMsg);
                    }
                } catch (Exception e) {
                    CTLogger.getLogger(this.getClass()).info("ENTRO IN ECCEZIONE" + e.getMessage() + "SPERO MEGLIO " + e);
                    e.printStackTrace();
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
                                CTLogger.getLogger(this.getClass()).info("invio a:" + messageToSendL.getKey());
                            }
                        scheduler.schedule(timeout, 60 * 25, TimeUnit.SECONDS);
                        break;

                    case AGGREGATION_REQUEST:
                        messageToSend = myConsumer.handleAggregationRequest(cMsg);
                        CTLogger.getLogger(this.getClass()).info("MESSAGGIO DOPO CONSUMER: " + messageToSend);
                        if (messageToSend != null) {
                            if (!messageToSend.getKey().equals("mySelf")) {
                                myProducer.enqueue(myHierarchyConnectionsRetriever.getMyDestinationName(messageToSend.getKey()), messageToSend.getValue());
                                CTLogger.getLogger(this.getClass()).info("invio a:" + messageToSend.getKey());
                            }else {
                                CTLogger.getLogger(this.getClass()).info("gestisco aggregazione");
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



            } catch (final JMSException e) {
                throw new RuntimeException(e);
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

    }


    private void handleAggregation(ObjectMessage msg) {
        try {

            CTLogger.getLogger(this.getClass()).info("entro in handle");
            CommunicationMessage cMsg = (CommunicationMessage) ((ObjectMessage) msg).getObject();
            AggregationRequest request = gson.fromJson(cMsg.getMessageBody(), AggregationRequest.class);

            CTLogger.getLogger(getClass()).info(cMsg.getMessageBody());
            double result = 0.0;

            CommunicationMessage outMsg = new CommunicationMessage();
            outMsg.setMessageType(MessageType.AGGREGATION_RESPONSE);
            outMsg.setSenderName(myDestinationName);
            AggregationResponse response = new AggregationResponse(request);

            if (request.getStartDay().equals(request.getLastDay())) {
                result = myKVManager.getDailyReport(request.getLastDay(), request.getType());

            } else {
                CTLogger.getLogger(this.getClass()).info("prima di cercare la request nel DB");
                result = myKVManager.getAggregation(request);
                CTLogger.getLogger(this.getClass()).info("stampo result del getAggregation (dovrebbe essere -1) " + result);
                if (result == -1.0) {
                    try {

                        if(!stop){
                            DailyReport d = new DailyReport(); d.addTotalSwab(100);
                            DailyReport d1 = new DailyReport(); d1.addTotalSwab(100);
                            myKVManager.addDailyReport(d);
                            myKVManager.addFake(d1);
                            stop = true;
                        }


                        CTLogger.getLogger(this.getClass()).info(myKVManager.getDailyReportsInAPeriod(request.getStartDay(), request.getLastDay(), request.getType()).get(1));

                        result = myErlangClient.computeAggregation(
                                request.getOperation(),
                                myKVManager.getDailyReportsInAPeriod(request.getStartDay(), request.getLastDay(), request.getType())
                        );
                        CTLogger.getLogger(this.getClass()).info("stampo il risultato " + result);
                        myKVManager.saveAggregation(request, result);
                        CTLogger.getLogger(this.getClass()).info("salvo il risultato");
                    } catch (IOException e) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        CTLogger.getLogger(this.getClass()).info("Eccezione: " + sw.toString());
                        result = 0.0;
                    }
                }
            }
            CTLogger.getLogger(this.getClass()).info("risultato che sto per inviare " + result);
            CTLogger.getLogger(this.getClass()).info("lo sto inviando a: " + msg.getJMSReplyTo());
            response.setResult(result);
            outMsg.setMessageBody(gson.toJson(response));
            msg.setObject(outMsg);
            myProducer.enqueue(msg.getJMSReplyTo(), outMsg);
        } catch (JMSException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            CTLogger.getLogger(this.getClass()).info("Eccezione: " + sw.toString());
        }
    }

    private void saveDailyReport(DailyReport dailyReport) { myKVManager.addDailyReport(dailyReport); }
}
