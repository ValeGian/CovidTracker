package it.unipi.dii.inginf.dsmt.covidtracker.communication;

import java.io.Serializable;

public class DailyReport implements Serializable {
    private int totalSwab;
    private int totalPositive;
    private int totalNegative;
    private int totalDead;

    public int getTotalSwab() {
        return totalSwab;
    }

    public void addAll(DailyReport reportToAggregate){
        this.totalSwab += reportToAggregate.totalSwab;
        this.totalPositive += reportToAggregate.totalPositive;
        this.totalNegative += reportToAggregate.totalNegative;
        this.totalDead += reportToAggregate.totalDead;
    }

    public void addTotalSwab(int totalSwab) {
        this.totalSwab += totalSwab;
    }

    public int getTotalPositive() {
        return totalPositive;
    }

    public void addTotalPositive(int totalPositive) {
        this.totalPositive += totalPositive;
    }

    public int getTotalNegative() {
        return totalNegative;
    }

    public void addTotalNegative(int totalNegative) {
        this.totalNegative += totalNegative;
    }

    public int getTotalDead() {
        return totalDead;
    }

    public void addTotalDead(int totalDead) {
        this.totalDead += totalDead;
    }
}
