package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import javax.ejb.Remote;
import java.io.Serializable;

@Remote
public interface CommunicationMessage extends Serializable {

    MessageType getMessageType();

    String getMessageBody();

    void setMessage(MessageType messageType);

    void setMessage(MessageType messageType, String messageBody);

}
