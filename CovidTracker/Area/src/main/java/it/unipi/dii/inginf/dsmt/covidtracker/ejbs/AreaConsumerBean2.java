package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import it.unipi.dii.inginf.dsmt.covidtracker.area.AreaNode;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.MessageType;

import javax.ejb.EJB;
import javax.ejb.Stateful;
import java.util.ArrayList;
import java.util.List;

@Stateful(name = "ConsumerEJB")
public class AreaConsumerBean2 {

    public String nome;
    public ArrayList<String> myRegions;
    public boolean[] connectedRegions;

    public boolean[] receivedDailyReport;
    public boolean waitingReport;

    public void initializeParameters(String nome, ArrayList<String> myRegions) {
        this.nome = nome;
        this.myRegions = myRegions;
        connectedRegions = new boolean[myRegions.size()];
        receivedDailyReport = new boolean[myRegions.size()];
    }


    public void handleDailyReport(CommunicationMessage cMsg) {
        String senderQueue = cMsg.getSenderName();
        int index = myRegions.indexOf(senderQueue);
        if(waitingReport && index != -1 && !receivedDailyReport[index]){
            //aggiungi in qualche modo da qualche parte il dato di cMsg.getMessageBody();
            receivedDailyReport[index] = true;
            if(AllReportArrived()) {
                waitingReport = false;
                AreaNode.myCommunicationMessage.setMessageType(MessageType.DAILY_REPORT);
                AreaNode.myCommunicationMessage.setSenderName(nome);
                AreaNode.myCommunicationMessage.setMessageBody("qui andranno i dati calcolati come aggragazioni delle varie cose");
                AreaNode.myProducer.enqueue("jms/nationQueue", AreaNode.myCommunicationMessage);
            }
        }
    }

    public boolean AllReportArrived() {
        for(int i = 0; i < connectedRegions.length; i++){
            if(receivedDailyReport[i] != connectedRegions[i])
                return false;
        }
        return true;
    }

    public void handleAggregationRequest(CommunicationMessage cMsg) {
        String senderQueue = cMsg.getSenderName();
        String request = cMsg.getMessageBody();

        String dest = ""; //dobbiamo parsare la request mettendoci d'accordo su una codifica

        int index = myRegions.indexOf(dest);

        if(index != -1) {//se index non è -1 vuol dire che il destinatario è una delle mie regioni e quindi la invio io
            AreaNode.myProducer.enqueue(myRegions.get(index), cMsg);

        }else if(dest.equals(nome)) {  //diretto a me e rispondo io
            String type; //dobbiamo trovare una codifica per parsare
            String period; //dobbiamo trovare una codifica per parsare
            List<Integer> dati; //get dal database i dati del periodo
            int result = 0; //chiamata a server Erlang a cui passo i dati come lista
            AreaNode.myCommunicationMessage.setMessageType(MessageType.AGGREGATION_RESPONSE);
            AreaNode.myCommunicationMessage.setMessageBody("" + result);

        }else //diretto a qualcun'altro e inoltro a nazione
            AreaNode.myProducer.enqueue("jms/nationQueue", cMsg);
    }

    public void handleRegistryClosureRequest(CommunicationMessage cMsg) {
        if(!waitingReport) {
            waitingReport = true;
            for (int i = 0; i < myRegions.size(); i++) {
                receivedDailyReport[i] = false;
                AreaNode.myCommunicationMessage.setMessageType(MessageType.REGISTRY_CLOSURE_REQUEST);
                AreaNode.myProducer.enqueue(myRegions.get(i), AreaNode.myCommunicationMessage);
            }
        }
    }

    public void handleConnectionRequest(CommunicationMessage cMsg) {
        String senderQueue = cMsg.getSenderName();
        String regionName = cMsg.getMessageBody();
        int index = myRegions.indexOf(regionName);
        if(index != -1 && !connectedRegions[index]){
            connectedRegions[index] = true;
            AreaNode.myCommunicationMessage.setMessageType(MessageType.CONNECTION_ACCEPTED);
        }else
            AreaNode.myCommunicationMessage.setMessageType(MessageType.CONNECTION_REFUSED);

        AreaNode.myProducer.enqueue(senderQueue, AreaNode.myCommunicationMessage);
    }
}
