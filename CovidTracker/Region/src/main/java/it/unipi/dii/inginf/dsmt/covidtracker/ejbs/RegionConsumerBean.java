package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.region.RegionNode;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.*;

public class RegionConsumerBean implements MessageListener{
    public RegionConsumerBean() {
    }

    @Override
    public void onMessage(Message msg) {
        if (msg instanceof ObjectMessage) {
            try {
                CommunicationMessage cMsg = (CommunicationMessage) ((ObjectMessage) msg).getObject();
                switch (cMsg.getMessageType()) {
                    case NO_ACTION_REQUEST:
                        break;
                    case CONNECTION_ACCEPTED:
                        handleConnectionAccepted(cMsg);
                        break;
                    case CONNECTION_REFUSED:
                        handleConnectionRefused(cMsg);
                        break;
                    case REGISTRY_CLOSURE_REQUEST:
                        handleRegistryClosureRequest(cMsg);
                        break;
                    case AGGREGATION_REQUEST:
                        handleAggregationRequest(cMsg);
                        break;
                    case AGGREGATION_RESPONSE:
                        handleAggregationResponse(cMsg);
                        break;
                    default:
                        break;
                }
            } catch (final JMSException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleRegistryClosureRequest(CommunicationMessage cMsg) {
        if (RegionNode.connectionState != 1)
            return;

        if (RegionNode.registryOpened){
            RegionNode.registryOpened = false;
            RegionNode.myCommunicationMessage.setMessageType(MessageType.DAILY_REPORT);
            //recupero di tutti i dati giornalieri
            RegionNode.myCommunicationMessage.setMessageBody(""); //immissione dei dati nel corpo del messaggio
            RegionNode.myProducer.enqueue(RegionNode.myArea, RegionNode.myCommunicationMessage);
        }
    }

    private void handleAggregationRequest(CommunicationMessage cMsg){
        if (RegionNode.connectionState != 1)
            return;

        String requester =  ""; //coda del richiedente da ottenere dal corpo del messaggio
        String aggregationType = ""; //tipo di aggregazione da ottenere dal corpo del messaggio
        String aggregationResult = ""; //qui andranno i risultati dell'aggragazione

        switch (aggregationType){
            //eseguo l'aggregazione richiesta interfacciandomi con Mongo per ottenere i log necessari
        }

        RegionNode.myCommunicationMessage.setMessageType(MessageType.AGGREGATION_RESPONSE);
        RegionNode.myCommunicationMessage.setMessageBody(aggregationResult);
        RegionNode.myProducer.enqueue(requester, RegionNode.myCommunicationMessage);
    }

    private void handleAggregationResponse(CommunicationMessage cMsg){
        if (RegionNode.connectionState != 1)
            return;

        String aggregationResult = cMsg.getMessageBody();

        //invio dei risultati dell'aggregazione a myRegionWeb che li mostrerà a video
    }

    private void handleConnectionAccepted(CommunicationMessage cMsg){
        RegionNode.connectionState = 1;
        RegionNode.myArea = cMsg.getSenderName();
    }

    private void handleConnectionRefused(CommunicationMessage cMsg){
        RegionNode.connectionState = -11;
    }
}
