package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import com.ericsson.otp.erlang.*;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.JavaErlServicesClient;

import javax.ejb.Stateless;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Stateless(name = "JavaErlServicesClientEJB")
public class JavaErlServicesClientBean implements JavaErlServicesClient {
    private static final String avg_ServerNodeName = "avg_node@localhost";
    private static final String avg_ServerRegisteredName = "avg_server";

    private static final String sum_ServerNodeName = "sum_node@localhost";
    private static final String sum_ServerRegisteredName = "sum_server";

    private static final String standardDev_ServerNodeName = "standardDev_node@localhost";
    private static final String standardDev_ServerRegisteredName = "standardDev_server";
    private static final String clientNodeName = "services_client_node@localhost";
    private static final String cookie = "";
    private final OtpNode clientNode;
    private final OtpMbox mbox;


    public JavaErlServicesClientBean() throws IOException {
        if (cookie!="") {
            clientNode = new OtpNode(clientNodeName, cookie);
        }
        else {
            clientNode = new OtpNode(clientNodeName);
        }
        mbox = clientNode.createMbox("default_mbox");
    }

    public static void main(String[] args){
        JavaErlServicesClient serverProva = null;
        try {
            serverProva = new JavaErlServicesClientBean();


            List<Integer> reportTry = new ArrayList<>();

            reportTry.add(8);
            reportTry.add(9);
            reportTry.add(10);

            System.out.println("RESULT:" + serverProva.computeAggregation("average", reportTry));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public double computeAggregation(String operation, List<Integer> reports) {
        //composing the request message
        if(operation != null && reports.size() > 0){

            if(operation.equals("sum"))
                return contactServer(sum_ServerRegisteredName, sum_ServerNodeName, reports);

            else if(operation.equals("average"))
                return contactServer(avg_ServerRegisteredName, avg_ServerNodeName, reports);

            else if(operation.equals("standard_devaition"))
                return contactServer(standardDev_ServerRegisteredName, standardDev_ServerNodeName, reports);
        }
        return -1;
    }


    private double contactServer(String serverRegisteredName, String serverNodeName, List<Integer> reports) {

        OtpErlangTuple reqMsg = new OtpErlangTuple(new OtpErlangObject[]{this.mbox.self(), javaListToErl(reports)});

        //sending out the request
        mbox.send(serverRegisteredName, serverNodeName, reqMsg);

        //blocking receive operation
        OtpErlangObject msg = null;
        try {
            msg = mbox.receive();
        } catch (OtpErlangExit otpErlangExit) {
            otpErlangExit.printStackTrace();
        } catch (OtpErlangDecodeException e) {
            e.printStackTrace();
        }

        OtpErlangDouble curr_avg_erlang = (OtpErlangDouble) msg;  //it is supposed to be a double...
        return curr_avg_erlang.doubleValue();

    }


    private OtpErlangList javaListToErl(List<Integer> reports) {
        OtpErlangInt[] numlist = new OtpErlangInt[reports.size()];

        for(int i = 0; i < reports.size(); i++){
            numlist[i] = new OtpErlangInt(reports.get(i));
        }
        return new OtpErlangList(numlist);
    }
}
