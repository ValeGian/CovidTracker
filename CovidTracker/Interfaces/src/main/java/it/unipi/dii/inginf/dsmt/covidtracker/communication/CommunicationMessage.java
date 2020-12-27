package it.unipi.dii.inginf.dsmt.covidtracker.communication;

import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;

import java.io.Serializable;

public class CommunicationMessage implements Serializable {

    MessageType messageType = MessageType.NO_ACTION_REQUEST;
    String senderName = null;
    String messageBody = null;

    public CommunicationMessage() {

    }

    public CommunicationMessage(MessageType messageType, String senderName, String messageBody) {
        this.messageType = messageType;
        this.senderName = senderName;
        this.messageBody = messageBody;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }
}
