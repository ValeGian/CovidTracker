package it.unipi.dii.inginf.dsmt.covidtracker.communication;

public class DailyReport {
    private int totalTampons;
    private int totalPositive;
    private int totalNegative;
    private int totalDead;

    public int getTotalTampons() {
        return totalTampons;
    }

    public void addAll(DailyReport reportToAggregate){
        this.totalTampons += reportToAggregate.totalTampons;
        this.totalPositive += reportToAggregate.totalPositive;
        this.totalNegative += reportToAggregate.totalNegative;
        this.totalDead += reportToAggregate.totalDead;
    }

    public void addTotalTampons(int totalTampons) {
        this.totalTampons += totalTampons;
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
