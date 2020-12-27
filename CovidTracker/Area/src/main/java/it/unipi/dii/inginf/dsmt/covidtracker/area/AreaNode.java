package it.unipi.dii.inginf.dsmt.covidtracker.area;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.*;
import javafx.util.Pair;

import javax.ejb.EJB;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.List;

//NOTA GENERALE: per ora gestisco tutto nel bean, ma preferirei chiamare un metodo statico di questa classe e far gestire tutto a lui
//ditemi cosa ne pensate e decidiamo (va fatto tutti uguale)

public class AreaNode implements MessageListener {

    private final static AreaNode instance = new AreaNode();
    final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";

    @EJB public static Producer myProducer;

    @EJB public static AreaConsumer myConsumer;

    @EJB public static CommunicationMessage myCommunicationMessage;

    @EJB public static HierarchyConnectionsRetriever myHierarchyConnectionsRetriever;

    public static void main(String[] args) {
        if (args.length == 1) {
            String name = args[0];

            setMessageListener(myHierarchyConnectionsRetriever.getMyDestinationName(name));

            myCommunicationMessage.setMessageType(MessageType.CONNECTION_REQUEST);

            myProducer.enqueue(myHierarchyConnectionsRetriever.getParentDestinationName(name), myCommunicationMessage);

            List<String> myRegions = myHierarchyConnectionsRetriever.getChildrenDestinationName(name);
            myConsumer.initializeParameters(name, myRegions, myHierarchyConnectionsRetriever.getParentDestinationName(name));
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
                Pair<String, CommunicationMessage> returnPair;
                switch (cMsg.getMessageType()) {
                    case NO_ACTION_REQUEST:
                        break;
                    case CONNECTION_ACCEPTED:
                        myConsumer.handleAcceptedConnection();
                        break;
                    case CONNECTION_REFUSED:
                        handleConnectionRefused();
                    case CONNECTION_REQUEST:
                        returnPair = myConsumer.handleConnectionRequest(cMsg);
                        if(returnPair != null)
                            myProducer.enqueue(returnPair.getKey(), returnPair.getValue());
                        break;
                    case REGISTRY_CLOSURE_REQUEST:
                        List<Pair<String, CommunicationMessage>> returnList= myConsumer.handleRegistryClosureRequest(cMsg);
                        break;
                    case AGGREGATION_REQUEST:
                        returnPair = myConsumer.handleAggregationRequest(cMsg);
                        if(!returnPair.getKey().equals("mySelf"))
                            myProducer.enqueue(returnPair.getKey(), returnPair.getValue());
                        else
                            handleAggregation(cMsg);
                        break;

                    case DAILY_REPORT:
                        returnPair = myConsumer.handleDailyReport(cMsg);
                        if(returnPair != null)
                            myProducer.enqueue(returnPair.getKey(), returnPair.getValue());
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
        String json = cMsg.getMessageBody();
        String type = "";
        String initDate = "";
        String offset ="";
        //fai cose
    }
}
