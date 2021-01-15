package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationResponse;

import javax.ejb.Remote;

@Remote
public interface SynchRequester {
    AggregationResponse requestAndReceiveAggregation(final String consumerName, final AggregationRequest requestMsg) throws Exception;
}
