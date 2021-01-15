package it.unipi.dii.inginf.dsmt.covidtracker.communication;

import java.io.Serializable;

public class DataLog implements Serializable {
    private String type;
    private int quantity;


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
