package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import javax.ejb.Remote;
import java.io.Serializable;

@Remote
public interface CommunicationMessage extends Serializable {

    MessageType getMessageType();

    String getSenderName();

    String getMessageBody();

    void setMessageType(MessageType messageType);

    void setSenderName(String receiverName);

    void setMessageBody(String messageBody);

}
