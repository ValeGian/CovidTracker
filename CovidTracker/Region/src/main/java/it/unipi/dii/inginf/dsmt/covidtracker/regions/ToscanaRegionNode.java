package it.unipi.dii.inginf.dsmt.covidtracker.regions;

import it.unipi.dii.inginf.dsmt.covidtracker.ejbs.GenericRegionNode;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.regionInterfaces.RegionToscana;
import it.unipi.dii.inginf.dsmt.covidtracker.persistence.KVManagerImpl;
import org.json.simple.parser.ParseException;

import javax.annotation.PostConstruct;
import javax.ejb.Stateful;
import java.io.IOException;

@Stateful(name = "ToscanaRegionEJB")
public class ToscanaRegionNode extends GenericRegionNode implements RegionToscana {
    @PostConstruct
    public void init(){
        try {
            myName = "toscana";
            myDestinationName = myHierarchyConnectionsRetriever.getMyDestinationName(myName);
            myAreaDestinationName = myHierarchyConnectionsRetriever.getParentDestinationName(myName);

            myKVManager = new KVManagerImpl(myName);
            myKVManager.deleteAllClientRequest();

            myMessageHandler.initializeParameters(myName, myDestinationName, myAreaDestinationName);

            setQueueConsumer(myDestinationName);
            startReceivingLoop();
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
    }
}
