package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.MessageType;
import javax.ejb.Stateless;

@Stateless(name = "CommunicationMessageEJB")
public class CommunicationMessageBean implements CommunicationMessage {

    MessageType messageType = MessageType.NO_ACTION_REQUEST;
    String senderName = null;
    String messageBody = null;

    public CommunicationMessageBean() {

    }

    @Override
    public MessageType getMessageType() {
        return messageType;
    }

    @Override
    public String getSenderName() {
        return senderName;
    }

    @Override
    public String getMessageBody() {
        return messageBody;
    }

    @Override
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    @Override
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    @Override
    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }
}
