package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.Producer;

import javax.ejb.Stateless;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@Stateless(name = "ProducerEJB")
public class ProducerBean implements Producer {

    static final String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";
    JMSContext myJMSContext; //initialized in constructor
    Context ic;

    public ProducerBean() {
        try{
            ic = new InitialContext();
            QueueConnectionFactory qcf = (QueueConnectionFactory)ic.lookup(QC_FACTORY_NAME);
            myJMSContext = qcf.createContext();
        }
        catch (NamingException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void enqueue(final String consumerName, final CommunicationMessage cMsg) {
        try {
            ObjectMessage outMsg = myJMSContext.createObjectMessage();
            outMsg.setObject(cMsg);
            enqueue(consumerName, outMsg);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void enqueue(final String consumerName, final Message outMsg) {
        try {
            if(consumerName.equals("tmp")) {
                myJMSContext.createProducer().send(outMsg.getJMSReplyTo(), outMsg);
            } else {
                Queue consumerQueue = (Queue) ic.lookup(consumerName);
                myJMSContext.createProducer().send(consumerQueue, outMsg);
            }
        } catch (NamingException | JMSException e) {
            e.printStackTrace();
        }
    }
}