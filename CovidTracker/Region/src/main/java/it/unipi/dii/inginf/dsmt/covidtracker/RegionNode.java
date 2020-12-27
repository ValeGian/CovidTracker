package it.unipi.dii.inginf.dsmt.covidtracker;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.Producer;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.RegionConsumer;
import javafx.util.Pair;

import javax.ejb.EJB;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;


public class RegionNode implements MessageListener {
    final static String QC_FACTORY_NAME = "jms/__defaultConnectionFactory";

    @EJB
    public static Producer myProducer;

    @EJB
    public static CommunicationMessage myCommunicationMessage;

    @EJB
    public static RegionConsumer myConsumer;

    private static RegionNode istance;

    public static void main(String[] args) {

        /*
        if (args.length != 2)
            return;

        String myName = args[0];
        myArea = args[1];
        connectionState = 0;

        setMessageListener(myName);

        myCommunicationMessage.setMessageType(MessageType.CONNECTION_REQUEST);
        myProducer.enqueue(myArea, myCommunicationMessage);

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
            qcf.createContext().createConsumer(myQueue).setMessageListener(RegionNode.getInstance());
        } catch (NamingException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private RegionNode(){}

    public static RegionNode getInstance(){
        if (istance == null)
            istance = new RegionNode();
        return istance;
    };

    @Override
    public void onMessage(Message msg) {
        if (msg instanceof ObjectMessage) {
            try {
                CommunicationMessage cMsg = (CommunicationMessage) ((ObjectMessage) msg).getObject();
                Pair<String, CommunicationMessage> messageToSend;
                switch (cMsg.getMessageType()) {
                    case NO_ACTION_REQUEST:
                        break;
                    case CONNECTION_ACCEPTED:
                        myConsumer.handleConnectionAccepted(cMsg);
                        break;
                    case CONNECTION_REFUSED:
                        myConsumer.handleConnectionRefused(cMsg);
                        break;
                    case REGISTRY_CLOSURE_REQUEST:
                        messageToSend = myConsumer.handleRegistryClosureRequest(cMsg);
                        if (messageToSend != null)
                            myProducer.enqueue(messageToSend.getKey(), messageToSend.getValue());
                        break;
                    case AGGREGATION_REQUEST:
                        messageToSend = myConsumer.handleAggregationRequest(cMsg);
                        if (messageToSend != null)
                            myProducer.enqueue(messageToSend.getKey(), messageToSend.getValue());
                        break;
                    case AGGREGATION_RESPONSE:
                        myConsumer.handleAggregationResponse(cMsg);
                        break;
                    default:
                        break;
                }
            } catch (final JMSException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
