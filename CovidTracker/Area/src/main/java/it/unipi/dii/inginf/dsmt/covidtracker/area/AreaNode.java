package it.unipi.dii.inginf.dsmt.covidtracker.area;

import com.google.gson.Gson;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.ejbs.DailyReport;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.*;
import javafx.util.Pair;

import javax.ejb.EJB;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

public class AreaNode implements MessageListener {

    private final static AreaNode instance = new AreaNode();
    final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";

    @EJB public static Producer myProducer;

    @EJB public static AreaConsumer myConsumer;

    public static CommunicationMessage myCommunicationMessage;

    @EJB public static HierarchyConnectionsRetriever myHierarchyConnectionsRetriever;

    public static void main(String[] args) {
        if (args.length == 1) {
            String name = args[0];
            try {
                setMessageListener(myHierarchyConnectionsRetriever.getMyDestinationName(name));

                myCommunicationMessage.setMessageType(MessageType.CONNECTION_REQUEST);

                myProducer.enqueue(myHierarchyConnectionsRetriever.getParentDestinationName(name), myCommunicationMessage);

                List<String> myRegions = myHierarchyConnectionsRetriever.getChildrenDestinationName(name);
                myConsumer.initializeParameters(name, myRegions, myHierarchyConnectionsRetriever.getParentDestinationName(name));
            }catch (IOException parseException){
                System.err.println(parseException.getMessage());
                return;
            }
        }
    }

    static void setMessageListener(final String QUEUE_NAME) {
        try {
            Context ic = new InitialContext();
            Queue myQueue = (Queue) ic.lookup(QUEUE_NAME);
            QueueConnectionFactory qcf = (QueueConnectionFactory) ic.lookup(QC_FACTORY_NAME);
            qcf.createContext().createConsumer(myQueue).setMessageListener(AreaNode.getInstance());
        } catch (NamingException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static AreaNode getInstance() { return instance; }

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
                            addDailyReport(new Gson().fromJson(messageToSend.getValue().getMessageBody(), DailyReport.class));
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

    private void addDailyReport(DailyReport dailyReport) {
        //aggiungi dailyReport al key-value;
    }

    private void handleConnectionRefused() {
        //CHIUDI TUTTO E TERMINA
    }

    private void handleAggregation(CommunicationMessage cMsg) {
        String json = cMsg.getMessageBody();
        String type = "";
        String initDate = "";
        String offset ="";
        //fai cose
    }
}
