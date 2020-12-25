package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import javax.ejb.Remote;

@Remote
public interface CommunicationMessage {

    MessageType getMessageType();

    String getMessageBody();

    void setMessage(MessageType messageType);

    void setMessage(MessageType messageType, String messageBody);

}
