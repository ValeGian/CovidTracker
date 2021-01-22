package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;

import javax.ejb.Remote;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.naming.NamingException;

@Remote
public interface Producer {

    void enqueue(final String consumerName, final CommunicationMessage cMsg) throws NamingException, JMSException;
    void enqueue(final String consumerName, final Message outMsg) throws NamingException, JMSException;

}
