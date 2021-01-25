package it.unipi.dii.inginf.dsmt.covidtracker.regions;

import it.unipi.dii.inginf.dsmt.covidtracker.ejbs.GenericRegionNode;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.regionInterfaces.RegionValleDAosta;
import it.unipi.dii.inginf.dsmt.covidtracker.persistence.KVManagerImpl;
import org.json.simple.parser.ParseException;

import javax.annotation.PostConstruct;
import javax.ejb.Stateful;
import java.io.IOException;

@Stateful(name = "ValleDAostaRegionEJB")
public class ValleDAostaRegionNode extends GenericRegionNode implements RegionValleDAosta {
    @PostConstruct
    public void init(){
        try {
            myName = "valledaosta";
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
