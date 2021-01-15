package it.unipi.dii.inginf.dsmt.covidtracker.nation;

import com.google.gson.Gson;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationResponse;
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
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NationNode implements MessageListener {

    private final static NationNode instance = new NationNode();
    private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private static ScheduledFuture<?> dailyReporterHandle = null;
    private static ScheduledFuture<?> timeoutHandle = null;

    private final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";
    private final static String myName = "nation";
    private static String myDestinationName;
    private static List<String> myChildrenDestinationNames;

    @EJB private Producer myProducer;
    @EJB private NationConsumerHandler myConsumer;
    @EJB private HierarchyConnectionsRetriever myHierarchyConnectionsRetriever;
    @EJB private JavaErlServicesClient myErlangClient;
    static KVManager myKVManager = new KVManagerImpl();
    private final Gson gson = new Gson();

    private NationNode() {
    }

    public static NationNode getInstance() { return instance; }

    public static void main(String[] args) throws IOException, ParseException {

        myDestinationName = instance.myHierarchyConnectionsRetriever.getMyDestinationName(myName);
        myChildrenDestinationNames = instance.myHierarchyConnectionsRetriever.getChildrenDestinationName(myName);

        instance.setMessageListener(myDestinationName);
        instance.myConsumer.initializeParameters(myDestinationName, myChildrenDestinationNames);

        instance.restartDailyThread();

        System.out.println("Type REGISTRY_CLOSURE to close the daily report: ");
        Scanner sc = new Scanner(System.in);
        while(true) {
            System.out.print("> ");
            instance.handleUserInput(sc.next());
        }
    }

    @Override
    public void onMessage(Message msg) {
        if (msg instanceof ObjectMessage) {
            try {
                CommunicationMessage cMsg = (CommunicationMessage) ((ObjectMessage) msg).getObject();
                switch (cMsg.getMessageType()) {
                    case AGGREGATION_REQUEST:
                        Pair<String, CommunicationMessage> messageToSend = myConsumer.handleAggregationRequest(cMsg);
                        if(messageToSend.getKey().equals(myDestinationName))
                            handleAggregation((ObjectMessage) msg);
                        else if(messageToSend.getKey().equals("flood"))
                            floodMessageToAreas((ObjectMessage) msg);
                        else
                            myProducer.enqueue(messageToSend.getKey(), messageToSend.getValue());
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

    //------------------------------------------------------------------------------------------------------------------


    void setMessageListener(final String QUEUE_NAME) {
        try{
            Context ic = new InitialContext();
            Queue myQueue= (Queue)ic.lookup(QUEUE_NAME);
            QueueConnectionFactory qcf = (QueueConnectionFactory)ic.lookup(QC_FACTORY_NAME);
            qcf.createContext().createConsumer(myQueue).setMessageListener(instance);
        }
        catch (final NamingException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    void saveDailyReport(DailyReport dailyReport) {
        myKVManager.addDailyReport(dailyReport);
    }

    void restartDailyThread() {
        if(dailyReporterHandle != null)
            dailyReporterHandle.cancel(true);
        if(timeoutHandle != null)
            timeoutHandle.cancel(true);

        final Runnable timeout = new Runnable() {
            @Override
            public void run() {
                saveDailyReport(myConsumer.getDailyReport());
            }
        };

        final Runnable dailyReporter = new Runnable() {
            @Override
            public void run() {
                instance.sendRegistryClosureRequests();
                timeoutHandle = scheduler.scheduleAtFixedRate(timeout, 0, 60*30, TimeUnit.SECONDS);
            }
        };

        dailyReporterHandle = scheduler.scheduleAtFixedRate(dailyReporter, secondsUntilMidnight(), 60*60*24, TimeUnit.SECONDS);

    }

    void sendRegistryClosureRequests() {
        CommunicationMessage regClosureMsg = new CommunicationMessage();
        regClosureMsg.setMessageType(MessageType.REGISTRY_CLOSURE_REQUEST);
        regClosureMsg.setSenderName(myDestinationName);

        for(String childDestinationName: myChildrenDestinationNames)
            myProducer.enqueue(childDestinationName, regClosureMsg);
    }

    void handleUserInput(String userInput) {
        if(userInput.equals("REGISTRY_CLOSURE")) {
            sendRegistryClosureRequests();
            restartDailyThread();
        } else {
            System.out.println("Command not recognized!");
        }
    }

    void handleAggregation(ObjectMessage msg) {
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

    void floodMessageToAreas(ObjectMessage outMsg) {
        try {
            CommunicationMessage oldMsg = (CommunicationMessage) ((ObjectMessage) outMsg).getObject();
            // wrap the old message in a new message with the nation as sender
            CommunicationMessage newCMsg = new CommunicationMessage();
            newCMsg.setMessageType(oldMsg.getMessageType());
            newCMsg.setSenderName(myDestinationName);
            newCMsg.setMessageBody(gson.toJson(oldMsg));
            outMsg.setObject(newCMsg);
            // flood the message to all the areas
            for (String childDestinationName : myChildrenDestinationNames)
                myProducer.enqueue(childDestinationName, outMsg);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    long secondsUntilMidnight() {
        ZoneId zone = ZoneId.of("Europe/Rome");
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(zone);
        return Duration.between(now, midnight).getSeconds();
    }

}
