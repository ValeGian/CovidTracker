package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import com.google.gson.Gson;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationResponse;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.*;
import it.unipi.dii.inginf.dsmt.covidtracker.log.CTLogger;
import it.unipi.dii.inginf.dsmt.covidtracker.persistence.JavaErlServicesClientImpl;
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
import java.io.PrintWriter;
import java.io.StringWriter;
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
    private final static int DAILY_REPORT_TIMEOUT = 100;
    private final static int DAILY_REPORT_PERIOD = 60*60*24;

    private final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";

    private final static String myName = "nation";
    private static String myDestinationName;
    private static List<String> myChildrenDestinationNames;

    @EJB private Producer myProducer;
    @EJB private HierarchyConnectionsRetriever myHierarchyConnectionsRetriever;

    private JavaErlServicesClient myErlangClient = new JavaErlServicesClientImpl();
    private NationConsumerHandler myMessageHandler = new NationConsumerHandlerImpl();
    private final KVManager myKVManager = new KVManagerImpl(myName);
    private final Gson gson = new Gson();

    private JMSConsumer myQueueConsumer;

    private final Runnable timeout = new Runnable() {
        @Override
        public void run() {
            saveDailyReport(myMessageHandler.getDailyReport());
        }
    };

    private final Runnable dailyReporter = new Runnable() {
        @Override
        public void run() {
            try {
                sendRegistryClosureRequests();
                //waits for half an hour for responses from its children, then closes its own daily registry
                timeoutHandle = scheduler.schedule(timeout, DAILY_REPORT_TIMEOUT, TimeUnit.SECONDS);
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                CTLogger.getLogger(this.getClass()).info("Eccezione: " + sw.toString());
            }
        }
    };

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

    //------------------------------------------------------------------------------------------------------------------

    public NationNodeBean() {
    }

    @PostConstruct
    public void init() {
        try {
            myDestinationName = myHierarchyConnectionsRetriever.getMyDestinationName(myName);
            myChildrenDestinationNames = myHierarchyConnectionsRetriever.getChildrenDestinationName(myName);

            setQueueConsumer(myDestinationName);
            myMessageHandler.initializeParameters(myName, myHierarchyConnectionsRetriever.getChildrenNames(myName));
            myKVManager.deleteAllClientRequest();

            restartDailyThread();
            startReceivingLoop();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            CTLogger.getLogger(this.getClass()).info("Eccezione: " + sw.toString());
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @Override
    public String readReceivedMessages() { return myKVManager.getAllClientRequest(); }

    @Override
    public void closeDailyRegistry() {
        try {
            sendRegistryClosureRequests();
            restartDailyThread();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            CTLogger.getLogger(this.getClass()).info("Eccezione: " + sw.toString());
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    public void handleMessage(Message msg) {
        if (msg instanceof ObjectMessage) {
            try {
                CommunicationMessage cMsg = (CommunicationMessage) ((ObjectMessage) msg).getObject();
                myKVManager.addClientRequest(cMsg.toString());

                switch (cMsg.getMessageType()) {
                    case AGGREGATION_REQUEST:
                        Pair<String, CommunicationMessage> messageToSend = myMessageHandler.handleAggregationRequest(cMsg);
                        if(messageToSend.getKey().equals(myName))
                            handleAggregation((ObjectMessage) msg);
                        else if(messageToSend.getKey().equals("flood"))
                            floodMessageToAreas((ObjectMessage) msg);
                        else
                            myProducer.enqueue(myHierarchyConnectionsRetriever.getMyDestinationName(messageToSend.getKey()), messageToSend.getValue(), msg.getJMSReplyTo());
                        break;

                    case DAILY_REPORT:
                        myMessageHandler.handleDailyReport(cMsg);
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

    //------------------------------------------------------------------------------------------------------------------

    private void restartDailyThread() {
        if(dailyReporterHandle != null)
            dailyReporterHandle.cancel(true);
        if(timeoutHandle != null)
            timeoutHandle.cancel(true);

        //ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)
        //Starts a thread which is scheduled to be executed each day at midnight
        dailyReporterHandle = scheduler.scheduleAtFixedRate(dailyReporter, secondsUntilMidnight(), DAILY_REPORT_PERIOD, TimeUnit.SECONDS);
    }

    private void startReceivingLoop() {
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

    private void sendRegistryClosureRequests() {
        CommunicationMessage regClosureMsg = new CommunicationMessage();
        regClosureMsg.setMessageType(MessageType.REGISTRY_CLOSURE_REQUEST);
        regClosureMsg.setSenderName(myName);

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
            outMsg.setSenderName(myName);
            AggregationResponse response = new AggregationResponse(request);

            if (request.getStartDay().equals(request.getLastDay())) {
                result = myKVManager.getDailyReport(request.getLastDay(), request.getType());
                if(result == -1.0)
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
                        CTLogger.getLogger(this.getClass()).warn("Eccezione: " + sw.toString());
                        result = 0.0;
                    }
                }
            }

            response.setResult(result);
            outMsg.setMessageBody(gson.toJson(response));

            // send the reply directly to te requester
            myProducer.enqueue(msg.getJMSReplyTo(), outMsg);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            CTLogger.getLogger(this.getClass()).warn("Eccezione: " + sw.toString());
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
