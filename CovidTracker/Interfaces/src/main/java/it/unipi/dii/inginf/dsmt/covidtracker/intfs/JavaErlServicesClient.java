package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import javax.ejb.Remote;
import java.io.IOException;
import java.util.List;

@Remote
public interface JavaErlServicesClient {
        double computeAggregation(String operation, List<Integer> reports) throws IOException;
}
