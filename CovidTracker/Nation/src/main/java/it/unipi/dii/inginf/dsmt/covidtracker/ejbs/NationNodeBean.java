package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import com.google.gson.Gson;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationResponse;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.*;
import it.unipi.dii.inginf.dsmt.covidtracker.utility.NationConsumerHandlerImpl;
import it.unipi.dii.inginf.dsmt.covidtracker.persistence.KVManagerImpl;
import javafx.util.Pair;
import org.json.simple.parser.ParseException;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.annotation.PostConstruct;
import javax.ejb.Stateful;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.jms.*;
import javax.naming.*;
import java.io.IOException;
import java.lang.IllegalStateException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.*;

@Stateful(name = "NationNodeEJB")
public class NationNodeBean implements NationNode {

    @SuppressWarnings({"all"})
    @Resource(mappedName = "concurrent/__defaultManagedScheduledExecutorService")
    private ManagedScheduledExecutorService scheduler;

    @Resource(mappedName = "concurrent/__defaultManagedExecutorService")
    private ManagedExecutorService executor;

    private ScheduledFuture<?> dailyReporterHandle = null;
    private ScheduledFuture<?> timeoutHandle = null;

    private final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";

    private final static String myName = "nation";
    private static String myDestinationName;
    private static List<String> myChildrenDestinationNames;

    @EJB private Producer myProducer;
    @EJB private Recorder myRecorder;
    @EJB private HierarchyConnectionsRetriever myHierarchyConnectionsRetriever;
    @EJB private JavaErlServicesClient myErlangClient;
    private NationConsumerHandler myMessageHandler = new NationConsumerHandlerImpl();
    private final KVManager myKVManager = new KVManagerImpl(myName);
    private final Gson gson = new Gson();

    private JMSConsumer myQueueConsumer;

    public NationNodeBean() {
    }

    @PostConstruct
    public void init() {
        try {
            myDestinationName = myHierarchyConnectionsRetriever.getMyDestinationName(myName);
            myChildrenDestinationNames = myHierarchyConnectionsRetriever.getChildrenDestinationName(myName);

            setQueueConsumer(myDestinationName);
            myMessageHandler.initializeParameters(myDestinationName, myChildrenDestinationNames);

            restartDailyThread();
            startReceivingLoop();
        } catch (IOException | ParseException ex) {
            throw new IllegalStateException(ex);
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @Override
    public String readReceivedMessages() {
            return myRecorder.readResponses();
    }

    @Override
    public void closeDailyRegistry() {
        try {
            sendRegistryClosureRequests();
            restartDailyThread();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    public void handleMessage(Message msg) {
        if (msg instanceof ObjectMessage) {
            try {
                CommunicationMessage cMsg = (CommunicationMessage) ((ObjectMessage) msg).getObject();
                myRecorder.addResponse(cMsg);

                switch (cMsg.getMessageType()) {
                    case AGGREGATION_REQUEST:
                        Pair<String, CommunicationMessage> messageToSend = myMessageHandler.handleAggregationRequest(cMsg);
                        if(messageToSend.getKey().equals(myDestinationName))
                            handleAggregation((ObjectMessage) msg);
                        else if(messageToSend.getKey().equals("flood"))
                            floodMessageToAreas((ObjectMessage) msg);
                        else
                            myProducer.enqueue(messageToSend.getKey(), messageToSend.getValue());
                        break;

                    case DAILY_REPORT:
                        myMessageHandler.handleDailyReport(cMsg);
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

    private void restartDailyThread() {
        if(dailyReporterHandle != null)
            dailyReporterHandle.cancel(true);
        if(timeoutHandle != null)
            timeoutHandle.cancel(true);

        final Runnable timeout = new Runnable() {
            @Override
            public void run() {
                saveDailyReport(myMessageHandler.getDailyReport());
            }
        };

        final Runnable dailyReporter = new Runnable() {
            @Override
            public void run() {
                try {
                    sendRegistryClosureRequests();
                    timeoutHandle = scheduler.scheduleAtFixedRate(timeout, 0, 60 * 30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        dailyReporterHandle = scheduler.scheduleAtFixedRate(dailyReporter, secondsUntilMidnight(), 60*60*24, TimeUnit.SECONDS);
    }

    private void startReceivingLoop() {
        final Runnable receivingLoop = new Runnable() {
            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Message inMsg = myQueueConsumer.receive();
                        handleMessage(inMsg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        executor.execute(receivingLoop);
    }

    //------------------------------------------------------------------------------------------------------------------

    private void setQueueConsumer(final String QUEUE_NAME) {
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

    private void saveDailyReport(DailyReport dailyReport) {
        myKVManager.addDailyReport(dailyReport);
    }

    private void sendRegistryClosureRequests() throws JMSException, NamingException {
        CommunicationMessage regClosureMsg = new CommunicationMessage();
        regClosureMsg.setMessageType(MessageType.REGISTRY_CLOSURE_REQUEST);
        regClosureMsg.setSenderName(myDestinationName);

        for(String childDestinationName: myChildrenDestinationNames) {
                myProducer.enqueue(childDestinationName, regClosureMsg);
        }
    }

    private void handleAggregation(ObjectMessage msg) {
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

    private void floodMessageToAreas(ObjectMessage outMsg) {
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

    private long secondsUntilMidnight() {
        ZoneId zone = ZoneId.of("Europe/Rome");
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(zone);
        return Duration.between(now, midnight).getSeconds();
    }
}