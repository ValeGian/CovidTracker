package it.unipi.dii.inginf.dsmt.covidtracker.area;

import com.google.gson.Gson;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.*;
import it.unipi.dii.inginf.dsmt.covidtracker.persistence.KVManager;
import javafx.util.Pair;
import org.json.simple.parser.ParseException;

import javax.ejb.EJB;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.List;

public class AreaNode implements MessageListener {

    private final static AreaNode instance = new AreaNode();
    private static final KVManager myDb = new KVManager();
    final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";

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
                Pair<String, CommunicationMessage> messageToSend = myConsumer.requestConnectionToParent();
                myProducer.enqueue(messageToSend.getKey(), messageToSend.getValue());

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
                    case NO_ACTION_REQUEST:
                        break;

                    case CONNECTION_ACCEPTED:
                        myConsumer.handleAcceptedConnection();
                        break;

                    case CONNECTION_REFUSED:
                        handleConnectionRefused();
                        break;

                    case CONNECTION_REQUEST:
                        messageToSend = myConsumer.handleConnectionRequest(cMsg);
                        if(messageToSend != null)
                            myProducer.enqueue(messageToSend.getKey(), messageToSend.getValue());
                        break;

                    case REGISTRY_CLOSURE_REQUEST:
                        List<Pair<String, CommunicationMessage>> returnList= myConsumer.handleRegistryClosureRequest(cMsg);
                        if(returnList != null)
                            for(Pair<String, CommunicationMessage> messageToSendL: returnList)
                                myProducer.enqueue(messageToSendL.getKey(), messageToSendL.getValue());
                        break;

                    case AGGREGATION_REQUEST:
                        messageToSend = myConsumer.handleAggregationRequest(cMsg);
                        if(!messageToSend.getKey().equals("mySelf"))
                            myProducer.enqueue(messageToSend.getKey(), messageToSend.getValue());
                        else
                            handleAggregation(cMsg);
                        break;

                    case DAILY_REPORT:
                        messageToSend = myConsumer.handleDailyReport(cMsg);
                        if(messageToSend != null) {
                            myProducer.enqueue(messageToSend.getKey(), messageToSend.getValue());
                            myDb.addDailyReport(new Gson().fromJson(messageToSend.getValue().getMessageBody(), DailyReport.class));
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


    private void handleConnectionRefused() {
        //CHIUDI TUTTO E TERMINA
    }

    private void handleAggregation(CommunicationMessage cMsg) {

        Gson converter = new Gson();
        AggregationRequest aggregationRequested = converter.fromJson(cMsg.getMessageBody(), AggregationRequest.class);
        double result = myDb.getAggregation(aggregationRequested);
        CommunicationMessage aggregationResponse = new CommunicationMessage();

        if(result == -1.0){
            result = 0;//getAggregationFromErlang();
            myDb.saveAggregation(aggregationRequested, result);
        }

        aggregationRequested.setResult(result);
        aggregationResponse.setMessageBody(converter.toJson(aggregationRequested));
        myProducer.enqueue(cMsg.getSenderName(), aggregationResponse);
    }
}
