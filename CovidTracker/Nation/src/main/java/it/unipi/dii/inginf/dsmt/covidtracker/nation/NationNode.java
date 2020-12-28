package it.unipi.dii.inginf.dsmt.covidtracker.nation;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.HierarchyConnectionsRetriever;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.NationConsumer;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.Producer;
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
    private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static ScheduledFuture<?> dailyReporterHandle = null;

    private final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";
    private final static String myName = "nation";
    private static String myDestinationName;
    private static List<String> myChildrenDestinationNames;

    @EJB static Producer myProducer;
    @EJB static NationConsumer myConsumer;
    @EJB public static HierarchyConnectionsRetriever myHierarchyConnectionsRetriever;

    private NationNode() {
    }

    public static NationNode getInstance() { return instance; }

    public static void main(String[] args) throws IOException, ParseException {

        myDestinationName = myHierarchyConnectionsRetriever.getMyDestinationName(myName);
        myChildrenDestinationNames = myHierarchyConnectionsRetriever.getChildrenDestinationName(myName);

        instance.setMessageListener(myDestinationName);
        myConsumer.initializeParameters(myDestinationName, myChildrenDestinationNames);

        instance.restartDailyThread();

        System.out.println("Type REGISTRY_CLOSURE to close the daily reports: ");
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
                Pair<String, CommunicationMessage> messageToSend;
                switch (cMsg.getMessageType()) {
                    case NO_ACTION_REQUEST:
                        break;

                    case CONNECTION_REQUEST:
                        messageToSend = myConsumer.handleConnectionRequest(cMsg);
                        myProducer.enqueue(messageToSend.getKey(), messageToSend.getValue());
                        break;

                    case AGGREGATION_REQUEST:
                        messageToSend = myConsumer.handleAggregationRequest(cMsg);
                        if(!messageToSend.getKey().equals("mySelf"))
                            myProducer.enqueue(messageToSend.getKey(), messageToSend.getValue());
                        else
                            handleAggregation(cMsg);
                        break;

                    case DAILY_REPORT:
                        List<DailyReport> childrenDailyReports = myConsumer.handleDailyReport(cMsg);
                        if(childrenDailyReports != null) {
                            saveDailyReport(childrenDailyReports);
                        }
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

    void saveDailyReport(List<DailyReport> childrenDailyReports) {
        DailyReport dailyReport = new DailyReport();
        for(DailyReport childDailyReport: childrenDailyReports) {
            dailyReport.addAll(childDailyReport);
        }
        //Salvare i totali giornalieri su LevelDB
    }

    void restartDailyThread() {
        if(dailyReporterHandle != null)
            dailyReporterHandle.cancel(true);

        final Runnable dailyReporter = new Runnable() {
            @Override
            public void run() {
                instance.sendRegistryClosureRequests();
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

    //------------------------------------------------------------------------------------------------------------------

    long secondsUntilMidnight() {
        ZoneId zone = ZoneId.of("Europe/Rome");
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(zone);
        return Duration.between(now, midnight).getSeconds();
    }

}
