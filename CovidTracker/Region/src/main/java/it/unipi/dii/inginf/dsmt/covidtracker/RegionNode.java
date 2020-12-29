package it.unipi.dii.inginf.dsmt.covidtracker;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DataLog;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.HierarchyConnectionsRetriever;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.Producer;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.RegionConsumer;
import javafx.util.Pair;
import org.json.simple.parser.ParseException;
import com.google.gson.Gson;

import javax.ejb.EJB;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


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

    private static RegionNode istance;

    private List<DataLog> dataLogs; //logs received from the web server

    public static void main(String[] args) {
        RegionNode.getInstance().sendDailyReport();

        if (args.length != 1)
            return;
        try {
            String myName = myHierarchyConnectionsRetriever.getMyDestinationName(args[0]);
            String myArea = myHierarchyConnectionsRetriever.getParentDestinationName(myName);
            myConsumer.initializeParameters(myName, myArea);

            setMessageListener(myName);

            //myCommunicationMessage.setMessageType(MessageType.CONNECTION_REQUEST);
            //myProducer.enqueue(myArea, myCommunicationMessage);

            //RegionNode resta in attesa della ricezione dei messaggi da parte di RegionWeb o da altri nodi
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }


    }

    static void setMessageListener(final String QUEUE_NAME) {
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
        dataLogs = new ArrayList<DataLog>();
    }

    public static RegionNode getInstance(){
        if (istance == null)
            istance = new RegionNode();
        return istance;
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
                    case REGISTRY_CLOSURE_REQUEST:
                        messageToSend = myConsumer.handleRegistryClosureRequest(cMsg);
                        if (messageToSend != null)
                            myProducer.enqueue(messageToSend.getKey(), messageToSend.getValue());
                        break;
                    case AGGREGATION_REQUEST:
                        messageToSend = myConsumer.handleAggregationRequest(cMsg);
                        if (messageToSend != null)
                            myProducer.enqueue(messageToSend.getKey(), messageToSend.getValue());
                        break;
                    case AGGREGATION_RESPONSE:
                        myConsumer.handleAggregationResponse(cMsg);
                        break;
                    case NEW_DATA:
                        dataLogs.add(myConsumer.handleNewData(cMsg));
                        break;
                    default:
                        break;
                }
            } catch (final JMSException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void sendDailyReport(){
        DailyReport dailyReport = new DailyReport();

        LocalDate localDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String currentDay = localDate.format(formatter);

        for (DataLog dataLog : dataLogs){
            if (dataLog.getDay().equals(currentDay)){
                dailyReport.addTotalDead(dataLog.getNewDead());
                dailyReport.addTotalNegative(dataLog.getNewNegative());
                dailyReport.addTotalPositive(dataLog.getNewPositive());
                dailyReport.addTotalSwab(dataLog.getNewSwab());
            }
        }
        Gson gson = new Gson();
        myCommunicationMessage.setMessageType(MessageType.DAILY_REPORT);
        myCommunicationMessage.setMessageBody(gson.toJson(dailyReport, DailyReport.class));
        //myProducer.enqueue();
    }
}
