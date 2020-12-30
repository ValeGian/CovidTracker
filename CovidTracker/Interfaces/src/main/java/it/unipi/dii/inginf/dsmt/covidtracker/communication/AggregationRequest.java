package it.unipi.dii.inginf.dsmt.covidtracker.communication;

public class AggregationRequest {
    private String type;
    private String destination;
    private String operation;
    private String startDay; //format "dd/MM/yyyy"
    private String lastDay;  //format "dd/MM/yyyy"
    private long timestamp;
    private double result;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getStartDay() {
        return startDay;
    }

    public void setStartDay(String startDay) {
        this.startDay = startDay;
    }

    public String getLastDay() {
        return lastDay;
    }

    public void setLastDay(String lastDay) {
        this.lastDay = lastDay;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public double getResult() { return result; }

    public void setResult(double result) { this.result = result; }

    public String toKey(){
        return type + ":" + operation + ":" + startDay + ":" + lastDay;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
