package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import it.unipi.dii.inginf.dsmt.covidtracker.area.AreaNode;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.MessageType;

import javax.jms.*;
import java.util.List;


public class AreaConsumerBean implements MessageListener {
    //private final MongoClient client;
    //private final MongoDatabase mongoDB;


    public AreaConsumerBean() {
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
                        handleConnectionRequest(cMsg);
                        break;
                    case REGISTRY_CLOSURE_REQUEST:
                        handleRegistryClosureRequest(cMsg);
                        break;
                    case AGGREGATION_REQUEST:
                        handleAggregationRequest(cMsg);
                        break;
                    case DAILY_REPORT:
                        handleDailyReport(cMsg);
                        break;
                    default:
                        break;
                }
            } catch (final JMSException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleDailyReport(CommunicationMessage cMsg) {
        String senderQueue = cMsg.getSenderName();
        int index = AreaNode.myRegions.indexOf(senderQueue);
        if(AreaNode.waitingReport && index != -1 && !AreaNode.receivedDailyReport[index]){
            //aggiungi in qualche modo da qualche parte il dato di cMsg.getMessageBody();
            AreaNode.receivedDailyReport[index] = true;
            if(AllReportArrived()) {
                AreaNode.waitingReport = false;
                AreaNode.myCommunicationMessage.setMessageType(MessageType.DAILY_REPORT);
                AreaNode.myCommunicationMessage.setSenderName(AreaNode.nome);
                AreaNode.myCommunicationMessage.setMessageBody("qui andranno i dati calcolati come aggragazioni delle varie cose");
                AreaNode.myProducer.enqueue("jms/nationQueue", AreaNode.myCommunicationMessage);
            }
        }
    }

    private boolean AllReportArrived() {
        for(int i = 0; i < AreaNode.connectedRegions.length; i++){
            if(AreaNode.receivedDailyReport[i] != AreaNode.connectedRegions[i])
                return false;
        }
        return true;
    }

    private void handleAggregationRequest(CommunicationMessage cMsg) {
        String senderQueue = cMsg.getSenderName();
        String request = cMsg.getMessageBody();

        String dest = ""; //dobbiamo parsare la request mettendoci d'accordo su una codifica

        int index = AreaNode.myRegions.indexOf(dest);

        if(index != -1) {//se index non è -1 vuol dire che il destinatario è una delle mie regioni e quindi la invio io
            AreaNode.myProducer.enqueue(AreaNode.myRegions.get(index), cMsg);

        }else if(dest.equals(AreaNode.nome)) {  //diretto a me e rispondo io
            String type; //dobbiamo trovare una codifica per parsare
            String period; //dobbiamo trovare una codifica per parsare
            List<Integer> dati; //get dal database i dati del periodo
            int result = 0; //chiamata a server Erlang a cui passo i dati come lista
            AreaNode.myCommunicationMessage.setMessageType(MessageType.AGGREGATION_RESPONSE);
            AreaNode.myCommunicationMessage.setMessageBody("" + result);

        }else //diretto a qualcun'altro e inoltro a nazione
            AreaNode.myProducer.enqueue("jms/nationQueue", cMsg);
    }

    private void handleRegistryClosureRequest(CommunicationMessage cMsg) {
        if(!AreaNode.waitingReport) {
            AreaNode.waitingReport = true;
            for (int i = 0; i < AreaNode.myRegions.size(); i++) {
                AreaNode.receivedDailyReport[i] = false;
                AreaNode.myCommunicationMessage.setMessageType(MessageType.REGISTRY_CLOSURE_REQUEST);
                AreaNode.myProducer.enqueue(AreaNode.myRegions.get(i), AreaNode.myCommunicationMessage);
            }
        }
    }

    private void handleConnectionRequest(CommunicationMessage cMsg) {
        String senderQueue = cMsg.getSenderName();
        String regionName = cMsg.getMessageBody();
        int index = AreaNode.myRegions.indexOf(regionName);
        if(index != -1 && !AreaNode.connectedRegions[index]){
            AreaNode.connectedRegions[index] = true;
            AreaNode.myCommunicationMessage.setMessageType(MessageType.CONNECTION_ACCEPTED);
        }else
            AreaNode.myCommunicationMessage.setMessageType(MessageType.CONNECTION_REFUSED);

        AreaNode.myProducer.enqueue(senderQueue, AreaNode.myCommunicationMessage);
    }
}
