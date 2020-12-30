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
import java.util.Map;

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
    Map<String, List<DataLog>> dataLogs; //logs received from web servers, the key is the day of the dataLog (format dd/MM/yyyy)
                                             //and the value is the list of logs received in that day
    Map<AggregationRequest, String> aggregationToAnswer = new HashMap<AggregationRequest, String>();

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
            DailyReport dailyReport = new DailyReport();

            for (DataLog dataLog : dataLogs.get(getCurrentDate())){
                if (dataLog.getType().equals("swab"))
                    dailyReport.addTotalSwab(dataLog.getQuantity());
                if (dataLog.getType().equals("positive"))
                    dailyReport.addTotalPositive(dataLog.getQuantity());
                if (dataLog.getType().equals("negative"))
                    dailyReport.addTotalNegative(dataLog.getQuantity());
                if (dataLog.getType().equals("dead"))
                    dailyReport.addTotalDead(dataLog.getQuantity());
            }

            kvDB.addDailyReport(dailyReport);

            myCommunicationMessage.setMessageType(MessageType.DAILY_REPORT);
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

        if (aggregationRequest.getDestination().equals(myName)) {
            double result;

            result = kvDB.getAggregation(aggregationRequest);

            if (result == -1) {
                List<Integer> reportsToAggregate;
                if (aggregationRequest.getStartDay() == null)
                    reportsToAggregate = kvDB.getDailyReportsInAPeriod(aggregationRequest.getLastDay(), aggregationRequest.getLastDay(), aggregationRequest.getType());
                else if (aggregationRequest.getLastDay() == null)
                    reportsToAggregate = kvDB.getDailyReportsInAPeriod(aggregationRequest.getStartDay(), aggregationRequest.getStartDay(), aggregationRequest.getType());
                else
                    reportsToAggregate = kvDB.getDailyReportsInAPeriod(aggregationRequest.getStartDay(), aggregationRequest.getLastDay(), aggregationRequest.getType());

                result = 0;//getAggregationFromErlang(
                //myDb.getDailyReportsInAPeriod(aggregationRequested.getStartDay(), aggregationRequested.getLastDay(), aggregationRequested.getType())
                //);
                kvDB.saveAggregation(aggregationRequest, result);
            }

            myCommunicationMessage.setMessageType(MessageType.AGGREGATION_RESPONSE);
            myCommunicationMessage.setMessageBody(gson.toJson(aggregationRequest));
            return new Pair<>(cMsg.getSenderName(), myCommunicationMessage);
        }
        else {
            aggregationToAnswer.put(aggregationRequest, cMsg.getSenderName());
            myCommunicationMessage.setMessageType(MessageType.AGGREGATION_REQUEST);
            myCommunicationMessage.setMessageBody(gson.toJson(aggregationRequest));
            return new Pair<>(myParent, myCommunicationMessage);
        }
    }

    @Override
    public Pair<String, CommunicationMessage> handleAggregationResponse(CommunicationMessage cMsg){
        if (connectionState != 1)
            return null;

        Gson gson = new Gson();
        AggregationRequest aggregationRequest = gson.fromJson(cMsg.getMessageBody(), AggregationRequest.class);
        myCommunicationMessage.setMessageType(MessageType.AGGREGATION_RESPONSE);
        myCommunicationMessage.setMessageBody(gson.toJson(aggregationRequest));
        AggregationRequest key = aggregationRequest;
        key.setResult(-1.0);
        String regionWeb = aggregationToAnswer.get(key);
        if (regionWeb != null)
            return new Pair<>(regionWeb, myCommunicationMessage);
        return null;
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
