package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import javax.ejb.Remote;
import java.util.List;

@Remote
public interface JavaErlServicesClient {

        double getAggregation(String operation, List<Integer> reports);

}
