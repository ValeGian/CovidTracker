package it.unipi.dii.inginf.dsmt.covidtracker.ejb.Areas;

import it.unipi.dii.inginf.dsmt.covidtracker.ejb.GenericAreaNode;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.areaInterfaces.AreaNorth;
import it.unipi.dii.inginf.dsmt.covidtracker.persistence.KVManagerImpl;
import org.json.simple.parser.ParseException;

import javax.annotation.PostConstruct;
import javax.ejb.Stateful;
import java.io.IOException;

@Stateful(name = "NorthAreaEJB")
public class NorthAreaNode extends GenericAreaNode implements AreaNorth {

    @PostConstruct
    public void init(){
        try {
            myDestinationName = myHierarchyConnectionsRetriever.getMyDestinationName("north");
            myKVManager = new KVManagerImpl("north");
            myKVManager.deleteAllClientRequest();
            myConsumer.initializeParameters(myDestinationName, myHierarchyConnectionsRetriever.getChildrenDestinationName("north"), myHierarchyConnectionsRetriever.getParentDestinationName("north"));
            setQueueConsumer(myDestinationName);
            startReceivingLoop();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
