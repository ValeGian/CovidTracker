package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;

import javax.ejb.Local;
import javax.jms.MessageListener;

@Local
public interface RegionWebConsumer extends MessageListener{
    String getAggregationResponses();
    void addAggregation(AggregationRequest aggregationRequest);
}
