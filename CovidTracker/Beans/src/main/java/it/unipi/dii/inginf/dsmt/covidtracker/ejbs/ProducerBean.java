package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.Producer;
import it.unipi.dii.inginf.dsmt.covidtracker.log.CTLogger;

import javax.ejb.Stateless;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.PrintWriter;
import java.io.StringWriter;

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
            CTLogger.getLogger(this.getClass()).warn(e.getMessage());
        }
    }

    @Override
    public void enqueue(final String consumerName, final CommunicationMessage cMsg) {
        try {
            ObjectMessage outMsg = myJMSContext.createObjectMessage();
            outMsg.setObject(cMsg);
            enqueue(consumerName, outMsg);
        } catch (JMSException e) {
            CTLogger.getLogger(this.getClass()).warn(e.getMessage());
        }
    }

    @Override
    public void enqueue(final String consumerName, final Message outMsg) {
        try {
            Queue consumerQueue = (Queue) ic.lookup(consumerName);
            myJMSContext.createProducer().send(consumerQueue, outMsg);
        } catch (NamingException e) {
            CTLogger.getLogger(this.getClass()).warn(e.getMessage());
        }
    }

    @Override
    public void enqueue(final Destination consumerName, final CommunicationMessage cMsg) {
        try {
            CTLogger.getLogger(this.getClass()).info("Enqueue message to " + consumerName + "\n\n" + cMsg.toString());
            ObjectMessage outMsg = myJMSContext.createObjectMessage();
            outMsg.setObject(cMsg);
            myJMSContext.createProducer().send(consumerName, outMsg);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            CTLogger.getLogger(this.getClass()).warn(sw.toString());
        }
    }
}