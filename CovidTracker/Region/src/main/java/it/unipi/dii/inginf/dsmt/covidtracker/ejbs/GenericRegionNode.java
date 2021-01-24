package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import intfs.RegionConsumerHandler;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.*;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.*;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.regionInterfaces.RegionNode;
import it.unipi.dii.inginf.dsmt.covidtracker.log.CTLogger;
import it.unipi.dii.inginf.dsmt.covidtracker.persistence.KVManagerImpl;
import javafx.util.Pair;
import org.json.simple.parser.ParseException;
import com.google.gson.Gson;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateful;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.jms.*;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GenericRegionNode{
    final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";

    @Resource(mappedName = "concurrent/__defaultManagedExecutorService")
    protected ManagedExecutorService executor;

    @EJB private Producer myProducer;
    @EJB protected RegionConsumerHandler myMessageHandler;
    @EJB protected HierarchyConnectionsRetriever myHierarchyConnectionsRetriever;
    @EJB private JavaErlServicesClient myErlangClient;

    protected KVManager myKVManager;

    private Map<String, List<DataLog>> dataLogs = new HashMap<>(); //logs received from web servers, the key is the day of the dataLog (format dd/MM/yyyy)
    //and the value is the list of logs received in that day

    private boolean initialized = false;
    private boolean registryOpened;
    protected String myDestinationName;
    protected String myAreaDestinationName;

    protected JMSConsumer myQueueConsumer;

    private final Gson gson = new Gson();

    protected void setQueueConsumer(final String QUEUE_NAME) {
        try{
            Context ic = new InitialContext();
            Queue myQueue= (Queue)ic.lookup(QUEUE_NAME);
            QueueConnectionFactory qcf = (QueueConnectionFactory)ic.lookup(QC_FACTORY_NAME);
            myQueueConsumer = qcf.createContext().createConsumer(myQueue);
            //qcf.createContext().createConsumer(myQueue).setMessageListener(this);
        }
        catch (final NamingException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public String readReceivedMessages() { return myKVManager.getAllClientRequest(); }

    protected void startReceivingLoop() {
        CTLogger.getLogger(this.getClass()).info("startReceivingLoop");
        final Runnable receivingLoop = new Runnable() {
            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        CTLogger.getLogger(this.getClass()).info("startReceivingLoop: PRE Receive");
                        Message inMsg = myQueueConsumer.receive();
                        CTLogger.getLogger(this.getClass()).info("startReceivingLoop: Post Receive - " + inMsg);
                        if(inMsg != null) {
                            handleMessage(inMsg);
                        } else {
                            CTLogger.getLogger(this.getClass()).warn("startReceivingLoop: Post Receive - message null");
                        }
                    }
                } catch (Exception e) {
                    CTLogger.getLogger(this.getClass()).error("startReceivingLoop - eccezione: " + e);
                    e.printStackTrace();
                }
            }
        };
        executor.execute(receivingLoop);
    }

    public GenericRegionNode(){ }

    public void handleMessage(Message msg) {
        if (msg instanceof ObjectMessage) {
            try {
                CommunicationMessage cMsg = (CommunicationMessage) ((ObjectMessage) msg).getObject();
                myKVManager.addClientRequest(cMsg.toString());
                CTLogger.getLogger(this.getClass()).info(cMsg.toString());

                Pair<String, CommunicationMessage> messageToSend;

                switch (cMsg.getMessageType()) {
                    case NO_ACTION_REQUEST:
                        break;
                    case REGISTRY_CLOSURE_REQUEST:
                        closeRegister(myAreaDestinationName);
                        break;
                    case AGGREGATION_REQUEST:
                        messageToSend = myMessageHandler.handleAggregationRequest(cMsg);
                        if (messageToSend.getValue().getMessageType() == MessageType.AGGREGATION_REQUEST)
                            myProducer.enqueue(messageToSend.getKey(), messageToSend.getValue());
                        else
                            handleAggregation((ObjectMessage) messageToSend.getValue());
                        break;
                    case NEW_DATA:
                        saveDataLog(gson.fromJson(cMsg.getMessageBody(), DataLog.class));
                        break;
                    default:
                        break;
                }
            } catch (final JMSException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // --------------------------------------------------------

    private void closeRegister(String destination){
        if (registryOpened){
            registryOpened = false;
            DailyReport dailyReport = new DailyReport();

            for (DataLog dataLog : dataLogs.get(getCurrentDate())){
                if (dataLog.getType().equals("swab"))
                    dailyReport.addTotalSwab(dataLog.getQuantity());
                if (dataLog.getType().equals("positive"))
                    dailyReport.addTotalPositive(dataLog.getQuantity());
                if (dataLog.getType().equals("negative"))
                    dailyReport.addTotalNegative(dataLog.getQuantity());
                if (dataLog.getType().equals("dead"))
                    dailyReport.addTotalDead(dataLog.getQuantity());
            }

            myKVManager.addDailyReport(dailyReport);

            CommunicationMessage outMsg = new CommunicationMessage();
            outMsg.setMessageType(MessageType.DAILY_REPORT);
            outMsg.setMessageBody(gson.toJson(dailyReport));
            myProducer.enqueue(destination, outMsg);
        }
    }

    private void handleAggregation(ObjectMessage msg){
        try {
            CommunicationMessage cMsg = (CommunicationMessage) ((ObjectMessage) msg).getObject();
            AggregationRequest request = gson.fromJson(cMsg.getMessageBody(), AggregationRequest.class);
            double result = 0.0;

            CommunicationMessage outMsg = new CommunicationMessage();
            outMsg.setMessageType(MessageType.AGGREGATION_RESPONSE);
            outMsg.setSenderName(myDestinationName);
            AggregationResponse response = new AggregationResponse(request);

            if (request.getStartDay() == request.getLastDay()) {
                result = myKVManager.getDailyReport(request.getLastDay(), request.getType());

            } else {
                result = myKVManager.getAggregation(request);
                if (result == -1.0) {
                    try {
                        result = myErlangClient.computeAggregation(
                                request.getOperation(),
                                myKVManager.getDailyReportsInAPeriod(request.getStartDay(), request.getLastDay(), request.getType())
                        );
                        myKVManager.saveAggregation(request, result);
                    } catch (IOException e) {
                        e.printStackTrace();
                        result = 0.0;
                    }
                }
            }

            response.setResult(result);
            outMsg.setMessageBody(gson.toJson(response));
            msg.setObject(outMsg);
            myProducer.enqueue(cMsg.getSenderName(), msg);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private void saveDataLog(DataLog dataLog){
        if (registryOpened) {
            String currentDate = getCurrentDate();
            dataLogs.get(currentDate);
            if (dataLogs.get(currentDate) == null)
                dataLogs.put(currentDate, new ArrayList<DataLog>());
            dataLogs.get(currentDate).add(dataLog);
        }
    }

    private String getCurrentDate(){
        LocalDate localDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return localDate.format(formatter);
    }
}
