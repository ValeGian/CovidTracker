package it.unipi.dii.inginf.dsmt.covidtracker.regions;

import it.unipi.dii.inginf.dsmt.covidtracker.ejbs.GenericRegionNode;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.regionInterfaces.RegionFriuliVeneziaGiulia;
import it.unipi.dii.inginf.dsmt.covidtracker.persistence.KVManagerImpl;
import org.json.simple.parser.ParseException;

import javax.annotation.PostConstruct;
import javax.ejb.Stateful;
import java.io.IOException;

@Stateful(name = "FriuliVeneziaGiuliaRegionEJB")
public class FriuliVeneziaGiuliaRegionNode extends GenericRegionNode implements RegionFriuliVeneziaGiulia {
    @PostConstruct
    public void init(){
        try {
            myName = "friuliveneziagiulia";
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
