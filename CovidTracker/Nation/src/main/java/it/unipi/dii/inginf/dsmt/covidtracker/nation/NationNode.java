package it.unipi.dii.inginf.dsmt.covidtracker.nation;

import it.unipi.dii.inginf.dsmt.covidtracker.ejbs.NationConsumerBean;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.Producer;

import javax.ejb.EJB;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class NationNode {

    final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";

    @EJB
    static Producer myProducer;

    @EJB
    static CommunicationMessage myCommunicationMessage;

    public static void main(String[] args) {
        setMessageListener("jms/nationQueue");
    }

    static void setMessageListener(final String QUEUE_NAME) {
        try{
            Context ic = new InitialContext();
            Queue myQueue= (Queue)ic.lookup(QUEUE_NAME);
            QueueConnectionFactory qcf = (QueueConnectionFactory)ic.lookup(QC_FACTORY_NAME);
            qcf.createContext().createConsumer(myQueue).setMessageListener(new NationConsumerBean());
        }
        catch (NamingException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
