package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import javax.ejb.Remote;

@Remote
public interface Producer {

    public void enqueue(final String consumerName, final String text);

}
