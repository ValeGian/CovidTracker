package it.unipi.dii.inginf.dsmt.covidtracker.nation;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.HierarchyConnectionsRetriever;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.NationConsumer;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.Producer;
import org.json.simple.parser.ParseException;

import javax.ejb.EJB;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;

public class NationNode implements MessageListener {

    private final static NationNode instance = new NationNode();

    final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";
    final static String nodeName = "nation";

    @EJB static Producer myProducer;
    @EJB static NationConsumer myConsumer;
    @EJB public static HierarchyConnectionsRetriever myHierarchyConnectionsRetriever;

    private NationNode() {

    }

    public static NationNode getInstance() { return instance; }

    public static void main(String[] args) {
        try {
            setMessageListener(myHierarchyConnectionsRetriever.getMyDestinationName(nodeName));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    static void setMessageListener(final String QUEUE_NAME) {
        try{
            Context ic = new InitialContext();
            Queue myQueue= (Queue)ic.lookup(QUEUE_NAME);
            QueueConnectionFactory qcf = (QueueConnectionFactory)ic.lookup(QC_FACTORY_NAME);
            qcf.createContext().createConsumer(myQueue).setMessageListener(instance);
        }
        catch (NamingException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(Message message) {

    }
}
