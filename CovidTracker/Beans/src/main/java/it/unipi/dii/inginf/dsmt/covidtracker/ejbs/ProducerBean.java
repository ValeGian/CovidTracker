package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.Producer;

import javax.ejb.EJB;
import javax.ejb.Stateful;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.HashMap;
import java.util.Map;

@Stateful(name = "ProducerEJB")
public class ProducerBean implements Producer {

    static final String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";
    JMSContext myJMSContext; //initialized in constructor
    Context ic;

    Map<String, Queue> consumers = new HashMap<>();

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
        if(consumers.get(consumerName) == null) {
            try {
                Queue newQueue= (Queue)ic.lookup(consumerName);
                consumers.put(consumerName, newQueue);
            } catch (NamingException e) {
                e.printStackTrace();
                return;
            }
        }

        try {
            ObjectMessage myMsg = myJMSContext.createObjectMessage();
            CommunicationMessageBean cMsgBean = (CommunicationMessageBean) cMsg;
            myMsg.setObject(cMsgBean);
            Queue myQueue = consumers.get(consumerName);
            myJMSContext.createProducer().send(myQueue,myMsg);
        } catch (JMSException|ClassCastException e) {
            e.printStackTrace();
        }
    }
}
