package it.unipi.dii.inginf.dsmt.covidtracker.communication;

import java.io.Serializable;

public class AggregationResponse implements Serializable {
    private String type;
    private String operation;
    private String startDay; //format "dd/MM/yyyy"
    private String lastDay;  //format "dd/MM/yyyy"
    private double result;

    public AggregationResponse(AggregationRequest ar) {
        type = ar.getType();
        operation = ar.getOperation();
        startDay = ar.getStartDay();
        lastDay = ar.getLastDay();
        result = 0.0;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
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

    public double getResult() {
        return result;
    }

    public void setResult(double result) {
        this.result = result;
    }

    public String toString() {
        return "[" + startDay + "-" + lastDay + "] " + operation + " - " + type + " = " + result;
    }
}
