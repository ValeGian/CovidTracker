package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import com.google.gson.Gson;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DataLog;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.RegionConsumer;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.KVManager;
import it.unipi.dii.inginf.dsmt.covidtracker.persistence.KVManagerImpl;
import javafx.util.Pair;

import javax.ejb.Stateful;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Stateful(name = "RegionConsumerEJB")
public class RegionConsumerBean implements RegionConsumer {

    String myName;
    boolean registryOpened;
    String myParent;
    int connectionState; //equal to:  0 if the connection is pending
                         //           1 if the connection is been accepted
                         //           -1 if the connection is been refused

    KVManager kvDB = new KVManagerImpl();
    CommunicationMessage myCommunicationMessage;
    HashMap<String, List<DataLog>> dataLogs; //logs received from web servers, the key is the day of the dataLog (format dd/MM/yyyy)
                                             //and the value is the list of logs received in that day
    @Override
    public void initializeParameters(String myName, String parent) {
        this.myName = myName;
        connectionState = 1;
        myParent = parent;
        myCommunicationMessage.setSenderName(myName);
    }

    @Override
    public Pair<String, CommunicationMessage> handleRegistryClosureRequest(CommunicationMessage cMsg) {
        if (connectionState != 1)
            return null;

        Gson gson = new Gson();

        if (registryOpened){
            registryOpened = false;
            myCommunicationMessage.setMessageType(MessageType.DAILY_REPORT);
            DailyReport dailyReport = new DailyReport();

            for (DataLog dataLog : dataLogs.get(getCurrentDate())){
                dailyReport.addTotalDead(dataLog.getNewDead());
                dailyReport.addTotalNegative(dataLog.getNewNegative());
                dailyReport.addTotalPositive(dataLog.getNewPositive());
                dailyReport.addTotalSwab(dataLog.getNewSwab());
            }

            kvDB.addDailyReport(dailyReport);
            myCommunicationMessage.setMessageBody(gson.toJson(dailyReport)); //immissione dei dati nel corpo del messaggio
            return new Pair<>(myParent, myCommunicationMessage);
        }
        return null;
    }

    @Override
    public Pair<String, CommunicationMessage> handleAggregationRequest(CommunicationMessage cMsg){
        if (connectionState != 1)
            return null;

        Gson gson = new Gson();
        AggregationRequest aggregationRequest = gson.fromJson(cMsg.getMessageBody(), AggregationRequest.class);
        AggregationRequest aggregationResult = aggregationRequest;

        aggregationResult.setResult(kvDB.getAggregation(aggregationRequest));
        if (aggregationResult.getResult() == -1) {
            switch (aggregationRequest.getType()){
                //eseguo l'aggregazione richiesta interfacciandomi con Mongo per ottenere i log necessari
            }
            kvDB.saveAggregation(aggregationResult, aggregationResult.getResult());
        }

        myCommunicationMessage.setMessageType(MessageType.AGGREGATION_RESPONSE);
        myCommunicationMessage.setMessageBody(gson.toJson(aggregationResult));
        return new Pair<>(cMsg.getSenderName(), myCommunicationMessage);
    }

    @Override
    public void handleAggregationResponse(CommunicationMessage cMsg){
        if (connectionState != 1)
            return;

        Gson gson = new Gson();
        AggregationRequest aggregationRequest = gson.fromJson(cMsg.getMessageBody(), AggregationRequest.class);

        //invio dei risultati dell'aggregazione a myRegionWeb che li mostrerà a video
    }

    @Override
    public void handleNewData(CommunicationMessage cMsg) {
        String currentDate = getCurrentDate();
        Gson gson = new Gson();
        DataLog newDataLog = gson.fromJson(cMsg.getMessageBody(), DataLog.class);
        dataLogs.get(currentDate);
        if (dataLogs.get(currentDate) == null)
            dataLogs.put(currentDate, new ArrayList<DataLog>());
        dataLogs.get(currentDate).add(newDataLog);
    }

    private String getCurrentDate(){
        LocalDate localDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return localDate.format(formatter);
    }
}
