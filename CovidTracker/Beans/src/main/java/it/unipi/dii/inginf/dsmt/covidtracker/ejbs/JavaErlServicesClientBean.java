package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.JavaErlServicesClient;

import javax.ejb.Stateless;
import java.util.List;

@Stateless(name = "JavaErlServicesClientEJB")
public class JavaErlServicesClientBean implements JavaErlServicesClient {
    public JavaErlServicesClientBean() {
    }

    @Override
    public double computeAggregation(String operation, List<Integer> reports) {
        return 0;
    }
}
