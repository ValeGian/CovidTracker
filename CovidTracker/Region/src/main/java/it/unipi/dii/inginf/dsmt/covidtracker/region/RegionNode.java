package it.unipi.dii.inginf.dsmt.covidtracker.region;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DataLog;
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
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class RegionNode implements MessageListener {
    final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";

    @EJB
    public static Producer myProducer;

    @EJB
    public static CommunicationMessage myCommunicationMessage;

    @EJB
    public static RegionConsumer myConsumer;

    @EJB
    public static HierarchyConnectionsRetriever myHierarchyConnectionsRetriever;

    private static RegionNode istance = new RegionNode();

    private static KVManager kvDB = new KVManagerImpl();

    @EJB
    private static JavaErlServicesClient myErlangClient;;

    private Map<String, List<DataLog>> dataLogs = new HashMap<String, List<DataLog>>(); //logs received from web servers, the key is the day of the dataLog (format dd/MM/yyyy)
                                                                                        //and the value is the list of logs received in that day

    private boolean registryOpened;

    public static void main(String[] args) {
        if (args.length != 1)
            return;

        try {
            String myName = myHierarchyConnectionsRetriever.getMyDestinationName(args[0]);
            String myArea = myHierarchyConnectionsRetriever.getParentDestinationName(myName);
            myConsumer.initializeParameters(myName, myArea);

            istance.setMessageListener(myName);
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

    private RegionNode(){    }

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
                        String destination = myConsumer.handleRegistryClosureRequest(cMsg);
                        if (destination != null)
                            closeRegister(destination);
                        break;
                    case AGGREGATION_REQUEST:
                        messageToSend = myConsumer.handleAggregationRequest(cMsg);
                        if (messageToSend != null) {
                            if (messageToSend.getValue().getMessageType() == MessageType.AGGREGATION_REQUEST)
                                myProducer.enqueue(messageToSend.getKey(), messageToSend.getValue());
                            else
                                handleAggregation(messageToSend.getValue());
                        }
                        break;
                    case AGGREGATION_RESPONSE:
                        messageToSend = myConsumer.handleAggregationResponse(cMsg);
                        if (messageToSend != null)
                            myProducer.enqueue(messageToSend.getKey(), messageToSend.getValue());
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
        Gson gson = new Gson();

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

            kvDB.addDailyReport(dailyReport);

            myCommunicationMessage.setMessageType(MessageType.DAILY_REPORT);
            myCommunicationMessage.setMessageBody(gson.toJson(dailyReport));
            myProducer.enqueue(destination, myCommunicationMessage);
        }
    }

    private void handleAggregation(CommunicationMessage cMsg){
        Gson gson = new Gson();
        AggregationRequest aggregationRequest = gson.fromJson(cMsg.getMessageBody(), AggregationRequest.class);

        double result;

        result = kvDB.getAggregation(aggregationRequest);

        if (result == -1) {
            List<Integer> reportsToAggregate;
            if (aggregationRequest.getStartDay() == null)
                reportsToAggregate = kvDB.getDailyReportsInAPeriod(aggregationRequest.getLastDay(), aggregationRequest.getLastDay(), aggregationRequest.getType());
            else if (aggregationRequest.getLastDay() == null)
                reportsToAggregate = kvDB.getDailyReportsInAPeriod(aggregationRequest.getStartDay(), aggregationRequest.getStartDay(), aggregationRequest.getType());
            else
                reportsToAggregate = kvDB.getDailyReportsInAPeriod(aggregationRequest.getStartDay(), aggregationRequest.getLastDay(), aggregationRequest.getType());

            try {
                result = myErlangClient.computeAggregation(aggregationRequest.getOperation(), reportsToAggregate);
                kvDB.saveAggregation(aggregationRequest, result);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        aggregationRequest.setResult(result);
        myCommunicationMessage.setMessageType(MessageType.AGGREGATION_RESPONSE);
        myCommunicationMessage.setMessageBody(gson.toJson(aggregationRequest));
        myProducer.enqueue(cMsg.getSenderName(), myCommunicationMessage);

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
