package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import javax.jms.*;

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
                    case DAILY_REPORT:

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
