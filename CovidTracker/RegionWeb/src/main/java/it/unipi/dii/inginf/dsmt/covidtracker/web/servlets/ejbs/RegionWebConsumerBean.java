package it.unipi.dii.inginf.dsmt.covidtracker.web.servlets.ejbs;

import com.google.gson.Gson;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DataLog;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.RegionWebConsumer;
import javafx.util.Pair;

import javax.ejb.Stateful;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.util.ArrayList;
import java.util.List;

@Stateful(name = "RegionWebConsumerEJB")
public class RegionWebConsumerBean implements RegionWebConsumer{
    List<AggregationRequest> aggregationRequests = new ArrayList<AggregationRequest>();

    @Override
    public String getAggregationResponses() {
        String outMsg = "";

        for (AggregationRequest aggregationRequest : aggregationRequests){
            if (aggregationRequest.getResult() != -1.0) { //se il risultato dell'aggregazione non è presente non è stata ancora ricevuta risposta
                outMsg.concat(aggregationRequest.getType() + " " + aggregationRequest.getDestination() + "\n");
                outMsg.concat("From " + aggregationRequest.getStartDay() + " to " + aggregationRequest.getLastDay() + "\n");
                outMsg.concat(aggregationRequest.getOperation() + " = " + aggregationRequest.getResult() + "\n\n");
            }
        }

        return outMsg;
    }

    @Override
    public void addAggregation(AggregationRequest aggregationRequest){
        aggregationRequests.add(aggregationRequest);
    }
    @Override
    public void onMessage(Message msg) {
        if (msg instanceof ObjectMessage) {
            try {
                CommunicationMessage cMsg = (CommunicationMessage) ((ObjectMessage) msg).getObject();
                Gson gson = new Gson();

                AggregationRequest aggregationResponse = gson.fromJson(cMsg.getMessageBody(), AggregationRequest.class);
                //controllo se la risposta all'aggregazione era per me, e nel caso setto il suo risultato
                AggregationRequest aggregationRequest = aggregationResponse;
                aggregationRequest.setResult(-1.0);
                for (int i = 0; i < aggregationRequests.size(); i++)
                    if (aggregationRequests.get(i).equals(aggregationRequest))
                        aggregationRequests.get(i).setResult(aggregationResponse.getResult());

            } catch (final JMSException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
