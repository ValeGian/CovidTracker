package it.unipi.dii.inginf.dsmt.covidtracker.region;

import it.unipi.dii.inginf.dsmt.covidtracker.ejbs.RegionConsumerBean;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.Producer;

import javax.ejb.EJB;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class RegionNode {
    final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";

    @EJB
    public static Producer myProducer;

    @EJB
    public static CommunicationMessage myCommunicationMessage;

    public static boolean registryOpened;

    public static String myArea;

    public static int connectionState; //equal to:  0 if the connection is pending
                                       //           1 if the connection is been accepted
                                       //           -1 if the connection is been refused

    public static void main(String[] args) {
        if (args.length != 2)
            return;

        String myName = args[0];
        myArea = args[1];
        connectionState = 0;

        setMessageListener(myName);

        myCommunicationMessage.setMessageType(MessageType.CONNECTION_REQUEST);
        myProducer.enqueue(myArea, myCommunicationMessage);

        /*
        //RegionNode resta in attesa della ricezione dei messaggi da parte di RegionWeb o da altri nodi
        while(true);


         */

        // Esempio per Region e Area, in Nazione non ha senso dato che ce n'è solo una
        //if(args.length == 1) {
            //String myName = args[0];
            // usare il nome (e.g. "centro") per recuperare le informazioni hostate nel file
            // utili per il futuro inoltro dei messaggi e mandare messaggi di connessione
            // e.g. "centro" manda messaggio di connessione a "nazione", il quale JNDI è
            // stato recuperato dal file, e poi attende risposta da "nazione" per sapere se
            // la connessione è andata a buon fine, ovvero se non si già era collegato
            // qualcun altro come "centro"; tale risposta viene gestita direttamente
            // da il metodo onMessage del proprio ConsumerBean hostato sullo stesso nodo
            // (prendere come riferimento NationConsumerBean nel modulo Nation)
            //String nationName = getNationName(myName); //metodo di utility per ottenere informazioni dal file hostato
            //myCommunicationMessage.setMessageBody(MessageType.CONNECTION_REQUEST);
            //myProducer.enqueue(nationName, myCommunicationMessage);
        }

    static void setMessageListener(final String QUEUE_NAME) {
        try {
            Context ic = new InitialContext();
            Queue myQueue = (Queue) ic.lookup(QUEUE_NAME);
            QueueConnectionFactory qcf = (QueueConnectionFactory) ic.lookup(QC_FACTORY_NAME);
            qcf.createContext().createConsumer(myQueue).setMessageListener(new RegionConsumerBean());
        } catch (NamingException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
