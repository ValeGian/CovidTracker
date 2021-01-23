package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import intfs.RegionConsumerHandler;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.*;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.*;
import it.unipi.dii.inginf.dsmt.covidtracker.persistence.KVManagerImpl;
import javafx.util.Pair;
import org.json.simple.parser.ParseException;
import com.google.gson.Gson;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
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

@Stateless(name = "RegionNodeEJB")
public class RegionNodeBean implements RegionNode {
    final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";

    private CommunicationMessage myCommunicationMessage;

    @Resource(mappedName = "concurrent/__defaultManagedExecutorService")
    private ManagedExecutorService executor;

    @EJB private Producer myProducer;
    @EJB private RegionConsumerHandler myMessageHandler;
    @EJB private HierarchyConnectionsRetriever myHierarchyConnectionsRetriever;
    @EJB private JavaErlServicesClient myErlangClient;

    private static KVManager myKVManager;


    public int messageReceived;

    private Map<String, List<DataLog>> dataLogs = new HashMap<String, List<DataLog>>(); //logs received from web servers, the key is the day of the dataLog (format dd/MM/yyyy)
                                                                                        //and the value is the list of logs received in that day
    private boolean registryOpened;
    private String myDestinationName;
    private String myAreaDestinationName;

    private JMSConsumer myQueueConsumer;


    private final Gson gson = new Gson();

    public int getNMessages(){
        return messageReceived;
    }

    public void initialize(String myName) {
        try {
            myDestinationName = myHierarchyConnectionsRetriever.getMyDestinationName(myName);
            myAreaDestinationName = myHierarchyConnectionsRetriever.getParentDestinationName(myName);

            myKVManager = new KVManagerImpl(myName);
            myKVManager.deleteAllClientRequest();

            myMessageHandler.initializeParameters(myDestinationName, myAreaDestinationName);

            messageReceived = 0;
            setQueueConsumer(myDestinationName);
            startReceivingLoop();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

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

    @Override
    public String readReceivedMessages() { return myKVManager.getAllClientRequest(); }

    private void startReceivingLoop() {
        final Runnable receivingLoop = new Runnable() {
            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Message inMsg = myQueueConsumer.receive();
                        messageReceived++;
                        handleMessage(inMsg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        executor.execute(receivingLoop);
    }

    public RegionNodeBean(){ }

    public void handleMessage(Message msg) {
        if (msg instanceof ObjectMessage) {
            try {
                CommunicationMessage cMsg = (CommunicationMessage) ((ObjectMessage) msg).getObject();
                myKVManager.addClientRequest(cMsg.toString());

                Pair<String, CommunicationMessage> messageToSend;
                Gson gson = new Gson();

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
