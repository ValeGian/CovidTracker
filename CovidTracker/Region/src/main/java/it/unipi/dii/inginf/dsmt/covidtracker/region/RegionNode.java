package it.unipi.dii.inginf.dsmt.covidtracker.region;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.*;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.*;
import it.unipi.dii.inginf.dsmt.covidtracker.persistence.KVManagerImpl;
import javafx.util.Pair;
import org.json.simple.parser.ParseException;
import com.google.gson.Gson;

import javax.ejb.EJB;
import javax.jms.*;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class RegionNode implements MessageListener {
    final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";

    private CommunicationMessage myCommunicationMessage;

    @EJB private Producer myProducer;
    @EJB private RegionConsumer myConsumer;
    @EJB private HierarchyConnectionsRetriever myHierarchyConnectionsRetriever;
    @EJB private JavaErlServicesClient myErlangClient;

    private static RegionNode istance = new RegionNode();

    private static KVManager myKVManager = new KVManagerImpl();


    private Map<String, List<DataLog>> dataLogs = new HashMap<String, List<DataLog>>(); //logs received from web servers, the key is the day of the dataLog (format dd/MM/yyyy)
                                                                                        //and the value is the list of logs received in that day

    private boolean registryOpened;

    private String myDestinationName;

    private String myAreaDestinationName;

    private final Gson gson = new Gson();

    public static void main(String[] args) {
        if (args.length != 1)
            return;

        try {
            istance.myDestinationName = istance.myHierarchyConnectionsRetriever.getMyDestinationName(args[0]);
            istance.myAreaDestinationName = istance.myHierarchyConnectionsRetriever.getParentDestinationName(args[0]);

            istance.myConsumer.initializeParameters(istance.myDestinationName, istance.myAreaDestinationName);

            istance.setMessageListener(istance.myDestinationName);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }


    }

    void setMessageListener(final String QUEUE_NAME) {
        try {
            Context ic = new InitialContext();
            Queue myQueue = (Queue) ic.lookup(QUEUE_NAME);
            QueueConnectionFactory qcf = (QueueConnectionFactory) ic.lookup(QC_FACTORY_NAME);
            qcf.createContext().createConsumer(myQueue).setMessageListener(RegionNode.getInstance());
        } catch (NamingException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private RegionNode(){

    }

    public static RegionNode getInstance(){
        return istance;
    }

    @Override
    public void onMessage(Message msg) {
        if (msg instanceof ObjectMessage) {
            try {
                CommunicationMessage cMsg = (CommunicationMessage) ((ObjectMessage) msg).getObject();
                Pair<String, CommunicationMessage> messageToSend;
                Gson gson = new Gson();

                switch (cMsg.getMessageType()) {
                    case NO_ACTION_REQUEST:
                        break;
                    case REGISTRY_CLOSURE_REQUEST:
                        closeRegister(myAreaDestinationName);
                        break;
                    case AGGREGATION_REQUEST:
                        messageToSend = myConsumer.handleAggregationRequest(cMsg);
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

            myCommunicationMessage.setMessageType(MessageType.DAILY_REPORT);
            myCommunicationMessage.setMessageBody(gson.toJson(dailyReport));
            myProducer.enqueue(destination, myCommunicationMessage);
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
