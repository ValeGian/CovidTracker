package it.unipi.dii.inginf.dsmt.covidtracker.communication;

import java.io.Serializable;

public class AggregationRequest implements Serializable {
    private String type;
    private String destination;
    private String operation;  //sum, avg, standard_deviation
    private String startDay; //format "dd/MM/yyyy"
    private String lastDay;  //format "dd/MM/yyyy"

    public AggregationRequest() {}

    public AggregationRequest(String type,
                              String destination,
                              String operation,
                              String startDay,
                              String lastDay) {

        this.type = type;
        this.destination = destination;
        this.operation = operation;
        this.startDay = startDay;
        this.lastDay = lastDay;
    }

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

    public String toKey(){
        return type + ":" + operation + ":" + startDay + ":" + lastDay;
    }

    public String toString() {
        return "[" + startDay + "-" + lastDay + "] " + operation + " - " + type + " on " + destination;
    }
}
