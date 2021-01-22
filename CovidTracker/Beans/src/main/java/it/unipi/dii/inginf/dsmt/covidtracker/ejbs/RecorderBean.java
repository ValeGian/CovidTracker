package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.Recorder;

import javax.ejb.Stateful;
import java.util.ArrayList;
import java.util.List;

@Stateful(name = "RecorderEJB")
public class RecorderBean implements Recorder {

    List<String> responses = new ArrayList<>();

    public RecorderBean() {
    }

    @Override
    public String readResponses() {
        String outMsg = "";
        for(String m : responses) {
            outMsg += m+"\n";
        }
        return outMsg;
    }

    @Override
    public void addResponse(Object response) {
        responses.add(response.toString());
    }
}
