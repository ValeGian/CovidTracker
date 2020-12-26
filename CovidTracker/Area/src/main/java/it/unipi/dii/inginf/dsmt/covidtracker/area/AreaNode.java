package it.unipi.dii.inginf.dsmt.covidtracker.area;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.Producer;

import javax.ejb.EJB;
import java.util.ArrayList;

//NOTA GENERALE: per ora gestisco tutto nel bean, ma preferirei chiamare un metodo statico di questa classe e far gestire tutto a lui
//ditemi cosa ne pensate e decidiamo (va fatto tutti uguale)

public class AreaNode {
    public static final String nome = "jms/centerQueue"; //problema enorme perché dobbiamo per forza decidere il nome a compile
                                                        // time e quindi dobbiamo avere un modulo diverso per ogni area/regione
    public static ArrayList<String> myRegions; //salva le sue regioni
    public static boolean[] connectedRegions; //tiene traccia delle regioni connesse

    public static boolean[] receivedDailyReport; //tiene traccia di chi ha inviato i report
    public static String state; //tiene traccia dei daily report e per ora ho pensato a due stati: WAITING per i report o NORMAL

    @EJB
    public static Producer myProducer;

    @EJB
    public static CommunicationMessage myCommunicationMessage;

    public static void main(String[] args) {
        if(args.length == 1){
            String myName = args[0];
            //nome = "jms/" + myName + "Queue";
            state = "NORMAL";
            myCommunicationMessage.setMessageType(MessageType.CONNECTION_REQUEST);
            myProducer.enqueue("jms/nationQueue", myCommunicationMessage);
            myRegions = new ArrayList<>();//getMyRegions(myName);
            connectedRegions = new boolean[myRegions.size()];
        }


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
