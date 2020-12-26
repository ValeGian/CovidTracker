package it.unipi.dii.inginf.dsmt.covidtracker.nation;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.Producer;

import javax.ejb.EJB;

public class NationNode {
    @EJB
    static Producer myProducer;

    @EJB
    static CommunicationMessage myCommunicationMessage;

    public static void main(String[] args) {
        /* Esempio per Region e Area, in Nazione non ha senso dato che ce n'è solo una
        if(args.length == 1) {
            String myName = args[0];
            // usare il nome (e.g. "centro") per recuperare le informazioni hostate nel file
            // utili per il futuro inoltro dei messaggi e mandare messaggi di connessione
            // e.g. "centro" manda messaggio di connessione a "nazione", il quale JNDI è
            // stato recuperato dal file, e poi attende risposta da "nazione" per sapere se
            // la connessione è andata a buon fine, ovvero se non si già era collegato
            // qualcun altro come "centro"; tale risposta viene gestita direttamente
            // da il metodo onMessage del proprio ConsumerBean hostato sullo stesso nodo
            // (prendere come riferimento NationConsumerBean nel modulo Nation)
            String nationName = getNationName(myName); //metodo di utility per ottenere informazioni dal file hostato
            myCommunicationMessage.setMessage(MessageType.CONNECTION_REQUEST);
            myProducer.enqueue(nationName, myCommunicationMessage);
        }
         */
    }
}
