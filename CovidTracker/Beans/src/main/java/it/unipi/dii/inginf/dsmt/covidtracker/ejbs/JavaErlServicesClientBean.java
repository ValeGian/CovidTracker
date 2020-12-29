package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import com.ericsson.otp.erlang.*;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.JavaErlServicesClient;

import javax.ejb.Stateful;
import java.io.IOException;
import java.util.List;

@Stateful(name = "JavaErlServicesClientEJB")
public class JavaErlServicesClientBean implements JavaErlServicesClient {
    private static final String serverNodeName = "services_node@localhost";
    private static final String serverRegisteredName = "services_server";
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

    @Override
    public double computeAggregation(String operation, List<Integer> reports) {
        //composing the request message
        OtpErlangInt num = new OtpErlangInt(drawnNum);
        OtpErlangTuple reqMsg = new OtpErlangTuple(new OtpErlangObject[]{this.mbox.self(), num});

        //sending out the request
        mbox.send(serverRegisteredName, serverNodeName, reqMsg);
        System.out.println("Request sent by " + Thread.currentThread().toString() + " : " +
                reqMsg.toString());

        //blocking receive operation
        OtpErlangObject msg = mbox.receive();
        //getting the message content (a number)
        OtpErlangDouble curr_avg_erlang = (OtpErlangDouble) msg;  //it is supposed to be a tuple...
        curr_avg = curr_avg_erlang.doubleValue();
        System.out.println("Response received on mailbox "+mbox.getName()+" : " + msg.toString() +
                "Content: " + Double.toString(curr_avg));
        return 0;
    }
}
