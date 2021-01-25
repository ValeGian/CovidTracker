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
            CTLogger.getLogger(this.getClass()).info("invio un messaggio a " + consumerName + "     " + cMsg.toString());
            enqueue(consumerName, outMsg);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            CTLogger.getLogger(this.getClass()).info("Eccezione: " + sw.toString());
        }
    }

    @Override
    public void enqueue(final String consumerName, final Message outMsg) {
        try {
            Queue consumerQueue = (Queue) ic.lookup(consumerName);
            myJMSContext.createProducer().send(consumerQueue, outMsg);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            CTLogger.getLogger(this.getClass()).info("Eccezione: " + sw.toString());
        }
    }

    @Override
    public void enqueue(final Destination consumerName, final CommunicationMessage cMsg) {
        try {
            ObjectMessage outMsg = myJMSContext.createObjectMessage();
            outMsg.setObject(cMsg);
            CTLogger.getLogger(this.getClass()).info("Prima di send");
            myJMSContext.createProducer().send(consumerName, outMsg);
            CTLogger.getLogger(this.getClass()).info("APPENA INVIATO IN enqueue(Destination): consumerName: " + consumerName + " cMsg: " + cMsg.toString());
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            CTLogger.getLogger(this.getClass()).info("Eccezione: " + sw.toString());
        }
    }

    @Override
    public void enqueue(String consumerName, CommunicationMessage cMsg, Destination replyTo) {
        try {
            ObjectMessage outMsg = myJMSContext.createObjectMessage();
            outMsg.setJMSReplyTo(replyTo);
            outMsg.setObject(cMsg);
            CTLogger.getLogger(this.getClass()).info("invio un messaggio a " + consumerName + "     " + cMsg.toString());
            enqueue(consumerName, outMsg);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            CTLogger.getLogger(this.getClass()).info("Eccezione: " + sw.toString());
        }
    }
}