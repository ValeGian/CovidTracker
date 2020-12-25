package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.MessageType;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.*;

@MessageDriven(name = "NationConsumerEJB",
        activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/nationQueue"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
})
public class NationConsumerBean implements MessageListener {
    public NationConsumerBean() {
    }

    @Override
    public void onMessage(Message msg) {
        if (msg instanceof ObjectMessage) {
            try {
                CommunicationMessage cMsg = (CommunicationMessage) ((ObjectMessage) msg).getObject();
                switch (cMsg.getMessageType()) {
                    case NO_ACTION_REQUEST:
                        break;
                    case CONNECTION_REQUEST:

                        break;
                    case REGISTRY_CLOSURE_REQUEST:

                        break;
                    case AGGREGATION_REQUEST:
                        String messageBody = cMsg.getMessageBody();
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
