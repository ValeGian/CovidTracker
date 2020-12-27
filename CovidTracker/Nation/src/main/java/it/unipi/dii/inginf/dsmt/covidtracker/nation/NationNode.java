package it.unipi.dii.inginf.dsmt.covidtracker.nation;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
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
import java.util.List;

public class NationNode implements MessageListener {

    private final static NationNode instance = new NationNode();

    final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";
    final static String myName = "nation";
    static String myDestinationName;
    static List<String> myChildrenDestinationNames;

    @EJB static Producer myProducer;
    @EJB static NationConsumer myConsumer;
    @EJB public static HierarchyConnectionsRetriever myHierarchyConnectionsRetriever;

    private NationNode() {
        try {
            myDestinationName = myHierarchyConnectionsRetriever.getMyDestinationName(myName);
            myChildrenDestinationNames = myHierarchyConnectionsRetriever.getChildrenDestinationName(myName);
        } catch (final IOException|ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static NationNode getInstance() { return instance; }

    public static void main(String[] args) {
        setMessageListener(myDestinationName);
    }

    static void setMessageListener(final String QUEUE_NAME) {
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
                        if(messageToSend != null)
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
}
