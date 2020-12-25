package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.MessageType;

import javax.ejb.Stateless;
import java.io.Serializable;

@Stateless(name = "CommunicationMessageEJB")
public class CommunicationMessageBean implements CommunicationMessage, Serializable {

    MessageType messageType = MessageType.NO_ACTION_REQUEST;
    String messageBody = null;

    public CommunicationMessageBean() {

    }

    @Override
    public MessageType getMessageType() {
        return messageType;
    }

    @Override
    public String getMessageBody() {
        return messageBody;
    }

    @Override
    public void setMessage(MessageType messageType) {
        this.messageType = messageType;
    }

    @Override
    public void setMessage(MessageType messageType, String messageBody) {
        this.messageType = messageType;
        this.messageBody = messageBody;
    }
}
