package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;

import java.util.List;

public interface KVManager {

    boolean addDailyReport(DailyReport dailyReport);

    double getDailyReport(String day, String type);

    List<Integer> getDailyReportsInAPeriod(String initialDateS, String finalDateS, String type);

    double getAggregation(AggregationRequest aggregation);

    void saveAggregation(AggregationRequest aggregation, double result);

}
