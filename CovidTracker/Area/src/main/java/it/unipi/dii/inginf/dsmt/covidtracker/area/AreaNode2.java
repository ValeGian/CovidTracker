package it.unipi.dii.inginf.dsmt.covidtracker.area;

import it.unipi.dii.inginf.dsmt.covidtracker.ejbs.AreaConsumerBean;
import it.unipi.dii.inginf.dsmt.covidtracker.ejbs.AreaConsumerBean2;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.Producer;

import javax.ejb.EJB;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.ArrayList;

//NOTA GENERALE: per ora gestisco tutto nel bean, ma preferirei chiamare un metodo statico di questa classe e far gestire tutto a lui
//ditemi cosa ne pensate e decidiamo (va fatto tutti uguale)

public class AreaNode2 implements MessageListener {

    private final static AreaNode2 instance = new AreaNode2();
    final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";

    @EJB
    public static Producer myProducer;

    @EJB
    public static AreaConsumerBean2 myConsumer;

    @EJB
    public static CommunicationMessage myCommunicationMessage;

    public static void main(String[] args) {
        if (args.length == 1) {
            String nome = args[0];

            setMessageListener(nome);

            myCommunicationMessage.setMessageType(MessageType.CONNECTION_REQUEST);
            myProducer.enqueue("jms/nationQueue", myCommunicationMessage);
            ArrayList<String> myRegions = new ArrayList<>();//getMyRegions(myName) che è un metodo della remota;

            myConsumer.initializeParameters(nome, myRegions);
        }
    }

    static void setMessageListener(final String QUEUE_NAME) {
        try {
            Context ic = new InitialContext();
            Queue myQueue = (Queue) ic.lookup(QUEUE_NAME);
            QueueConnectionFactory qcf = (QueueConnectionFactory) ic.lookup(QC_FACTORY_NAME);
            qcf.createContext().createConsumer(myQueue).setMessageListener(AreaNode2.getInstance());
        } catch (NamingException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static AreaNode2 getInstance() { return instance; }


    @Override
    public void onMessage(Message msg) {
        if (msg instanceof ObjectMessage) {
            try {
                CommunicationMessage cMsg = (CommunicationMessage) ((ObjectMessage) msg).getObject();
                switch (cMsg.getMessageType()) {
                    case NO_ACTION_REQUEST:
                        break;
                    case CONNECTION_REQUEST:
                        myConsumer.handleConnectionRequest(cMsg);
                        break;
                    case REGISTRY_CLOSURE_REQUEST:
                        myConsumer.handleRegistryClosureRequest(cMsg);
                        break;
                    case AGGREGATION_REQUEST:
                        myConsumer.handleAggregationRequest(cMsg);
                        break;
                    case DAILY_REPORT:
                        myConsumer.handleDailyReport(cMsg);
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
