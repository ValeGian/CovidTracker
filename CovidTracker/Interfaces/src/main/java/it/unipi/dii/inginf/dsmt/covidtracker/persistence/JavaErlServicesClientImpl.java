package it.unipi.dii.inginf.dsmt.covidtracker.persistence;


import com.ericsson.otp.erlang.*;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.JavaErlServicesClient;
import it.unipi.dii.inginf.dsmt.covidtracker.log.CTLogger;

import javax.ejb.Stateless;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class JavaErlServicesClientImpl implements JavaErlServicesClient {

    private static final String serverNodeName = "aggregation_node@localhost";

    private static final String serverRegisteredName = "aggregation_server";


    private static final String clientNodeName = "services_client_node@localhost";
    private static final String cookie = "";
    private OtpNode clientNode;
    private OtpMbox mbox;


    public JavaErlServicesClientImpl() {
        try {
            if (cookie!="") {
                clientNode = new OtpNode(clientNodeName, cookie);
            }
            else {
                clientNode = new OtpNode(clientNodeName);
            }
            mbox = clientNode.createMbox("default_mbox");
        } catch (IOException e) {
            clientNode = null;
            mbox = null;
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            CTLogger.getLogger(this.getClass()).info("Eccezione: " + sw.toString());
        }
    }

    @Override
    public double computeAggregation(String operation, List<Integer> reports) {

        if(reports.size() == 0)
            return 0.0;

        if(!operation.equals("sum") && !operation.equals("avg")) {
            operation = "standard_deviation";
            reports.add(80);
        }

        OtpErlangTuple reqMsg = new OtpErlangTuple(new OtpErlangObject[]{this.mbox.self(), javaListToErl(reports), new OtpErlangAtom(operation)});

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

        if(operation.equals("sum")) {
            OtpErlangLong curr_avg_erlang = (OtpErlangLong) msg;  //it is supposed to be a double...
            return curr_avg_erlang.longValue();
        }
        else {
            OtpErlangDouble curr_avg_erlang = (OtpErlangDouble) msg;  //it is supposed to be a double...
            return curr_avg_erlang.doubleValue();
        }
    }


    private OtpErlangList javaListToErl(List<Integer> reports) {
        OtpErlangInt[] numlist = new OtpErlangInt[reports.size()];

        for(int i = 0; i < reports.size(); i++){
            numlist[i] = new OtpErlangInt(reports.get(i));
        }
        return new OtpErlangList(numlist);
    }

}

