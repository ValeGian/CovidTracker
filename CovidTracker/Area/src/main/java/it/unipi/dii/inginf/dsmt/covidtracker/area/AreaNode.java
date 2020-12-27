package it.unipi.dii.inginf.dsmt.covidtracker.area;

import it.unipi.dii.inginf.dsmt.covidtracker.ejbs.AreaConsumerBean;
import it.unipi.dii.inginf.dsmt.covidtracker.ejbs.AreaConsumerBean2;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.Producer;

import javax.ejb.EJB;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.ArrayList;

//NOTA GENERALE: per ora gestisco tutto nel bean, ma preferirei chiamare un metodo statico di questa classe e far gestire tutto a lui
//ditemi cosa ne pensate e decidiamo (va fatto tutti uguale)

public class AreaNode {
    public static String nome; //problema enorme perché dobbiamo per forza decidere il nome a compile
                                                        // time e quindi dobbiamo avere un modulo diverso per ogni area/regione
    public static ArrayList<String> myRegions; //salva le sue regioni
    public static boolean[] connectedRegions; //tiene traccia delle regioni connesse

    public static boolean[] receivedDailyReport; //tiene traccia di chi ha inviato i report
    public static boolean waitingReport; //tiene traccia dei daily report e per ora ho pensato a due stati: WAITING per i report o NORMAL

    final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";

    @EJB
    public static Producer myProducer;

    @EJB
    public static AreaConsumerBean2 myConsumer;

    @EJB
    public static CommunicationMessage myCommunicationMessage;

    public static void main(String[] args) {
        if(args.length == 1)
            setMessageListener(args[0]);

        String myName = args[0];
        myCommunicationMessage.setMessageType(MessageType.CONNECTION_REQUEST);
        myProducer.enqueue("jms/nationQueue", myCommunicationMessage);
        myRegions = new ArrayList<>();//getMyRegions(myName) che è un metodo della remota;
        connectedRegions = new boolean[myRegions.size()];
    }

    static void setMessageListener(final String QUEUE_NAME) {
        try {
            Context ic = new InitialContext();
            Queue myQueue = (Queue) ic.lookup(QUEUE_NAME);
            QueueConnectionFactory qcf = (QueueConnectionFactory) ic.lookup(QC_FACTORY_NAME);
            qcf.createContext().createConsumer(myQueue).setMessageListener(new AreaConsumerBean());
        } catch (NamingException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

}
