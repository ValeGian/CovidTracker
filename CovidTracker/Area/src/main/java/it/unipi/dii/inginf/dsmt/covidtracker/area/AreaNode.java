package it.unipi.dii.inginf.dsmt.covidtracker.area;

import com.google.gson.Gson;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.*;
import it.unipi.dii.inginf.dsmt.covidtracker.persistence.KVManagerImpl;
import javafx.util.Pair;
import org.json.simple.parser.ParseException;

import javax.ejb.EJB;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AreaNode implements MessageListener {

    private final static AreaNode instance = new AreaNode();
    private static final KVManagerImpl myDb = new KVManagerImpl();
    final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";
    private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static ScheduledFuture<?> timeoutHandle = null;

    private static final Runnable timeout = new Runnable() { public void run() { saveDailyReport(myConsumer.getDailyReport()); }};

    @EJB public static Producer myProducer;

    @EJB public static AreaConsumer myConsumer;

    @EJB public static HierarchyConnectionsRetriever myHierarchyConnectionsRetriever;

    public static void main(String[] args) {
        if (args.length == 1) {
            String name = args[0];
            try {
                String myJNDIQueue = myHierarchyConnectionsRetriever.getMyDestinationName(name);
                instance.setMessageListener(myJNDIQueue);

                myConsumer.initializeParameters(myJNDIQueue, myHierarchyConnectionsRetriever.getChildrenDestinationName(name), myHierarchyConnectionsRetriever.getParentDestinationName(name));
            }catch (IOException | ParseException parseException){
                throw new RuntimeException(parseException);
            }
        }
    }

    private AreaNode(){}

    public static AreaNode getInstance() { return instance; }

    void setMessageListener(final String QUEUE_NAME) {
        try{
            Context ic = new InitialContext();
            Queue myQueue = (Queue)ic.lookup(QUEUE_NAME);
            QueueConnectionFactory qcf = (QueueConnectionFactory)ic.lookup(QC_FACTORY_NAME);
            qcf.createContext().createConsumer(myQueue).setMessageListener(instance);
        }
        catch (final NamingException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(Message msg) {
        if (msg instanceof ObjectMessage) {
            try {
                CommunicationMessage cMsg = (CommunicationMessage) ((ObjectMessage) msg).getObject();
                Pair<String, CommunicationMessage> messageToSend;
                switch (cMsg.getMessageType()) {

                    case REGISTRY_CLOSURE_REQUEST:
                        List<Pair<String, CommunicationMessage>> returnList= myConsumer.handleRegistryClosureRequest(cMsg);
                        if(returnList != null)
                            for(Pair<String, CommunicationMessage> messageToSendL: returnList)
                                myProducer.enqueue(messageToSendL.getKey(), messageToSendL.getValue());
                        timeoutHandle = scheduler.scheduleAtFixedRate(timeout, 0, 60*25, TimeUnit.SECONDS);
                        break;

                    case AGGREGATION_REQUEST:
                        messageToSend = myConsumer.handleAggregationRequest(cMsg);
                        if(messageToSend != null) {
                            if (!messageToSend.getKey().equals("mySelf"))
                                myProducer.enqueue(messageToSend.getKey(), messageToSend.getValue());
                            else
                                handleAggregation(cMsg);
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
            }
        }
    }


    private void handleAggregation(CommunicationMessage cMsg) {

        Gson converter = new Gson();
        double result;
        AggregationRequest aggregationRequested = converter.fromJson(cMsg.getMessageBody(), AggregationRequest.class);
        CommunicationMessage aggregationResponse = new CommunicationMessage();
        aggregationResponse.setMessageType(MessageType.AGGREGATION_RESPONSE);


        if (aggregationRequested.getStartDay() == null) {
            result = myDb.getDailyReport(aggregationRequested.getLastDay(), aggregationRequested.getType());

        } else if (aggregationRequested.getLastDay() == null) {
            result = myDb.getDailyReport(aggregationRequested.getStartDay(), aggregationRequested.getType());

        } else {
            result = myDb.getAggregation(aggregationRequested);
            if(result == -1.0){
                result = 0;//getAggregationFromErlang(
                        //myDb.getDailyReportsInAPeriod(aggregationRequested.getStartDay(), aggregationRequested.getLastDay(), aggregationRequested.getType())
                //);
                myDb.saveAggregation(aggregationRequested, result);
            }
        }
        aggregationRequested.setResult(result);
        aggregationResponse.setMessageBody(converter.toJson(aggregationRequested));
        myProducer.enqueue(cMsg.getSenderName(), aggregationResponse);
    }

    private static void saveDailyReport(DailyReport dailyReport) { myDb.addDailyReport(dailyReport); }
}
