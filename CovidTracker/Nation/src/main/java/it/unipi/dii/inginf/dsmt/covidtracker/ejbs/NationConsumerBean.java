package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@MessageDriven(name = "NationConsumerEJB",
        activationConfig = {
        @ActivationConfigProperty(propertyName = "clientId", propertyValue = "<JMS client identifier that will be used when connecting to the JMS provider from which a JMS message-driven bean is to receive messages>"),
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "<JMS queue or topic from which a JMS message-driven bean is to receive messages>"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
})
public class NationConsumerBean implements MessageListener {
    public NationConsumerBean() {
    }

    @Override
    public void onMessage(Message msg) {
        if (msg instanceof TextMessage) {
            try {
                String text = ((TextMessage) msg).getText();
                System.out.println(text);
            } catch (final JMSException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
