package it.unipi.dii.inginf.dsmt.covidtracker.communication;

public class DataLog {
    private int newSwab;
    private int newPositive;
    private int newNegative;
    private int newDead;

    public int getNewSwab() {
        return newSwab;
    }

    public void setNewSwab(int newSwab) {
        this.newSwab = newSwab;
    }

    public int getNewPositive() {
        return newPositive;
    }

    public void setNewPositive(int newPositive) {
        this.newPositive = newPositive;
    }

    public int getNewNegative() {
        return newNegative;
    }

    public void setNewNegative(int newNegative) {
        this.newNegative = newNegative;
    }

    public int getNewDead() {
        return newDead;
    }

    public void setNewDead(int newDead) {
        this.newDead = newDead;
    }
}
