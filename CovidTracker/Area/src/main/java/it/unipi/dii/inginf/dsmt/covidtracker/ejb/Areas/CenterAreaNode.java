package it.unipi.dii.inginf.dsmt.covidtracker.ejb.Areas;

import it.unipi.dii.inginf.dsmt.covidtracker.ejb.GenericAreaNode;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.areaInterfaces.AreaCenter;
import it.unipi.dii.inginf.dsmt.covidtracker.persistence.KVManagerImpl;
import org.json.simple.parser.ParseException;

import javax.annotation.PostConstruct;
import javax.ejb.Stateful;
import java.io.IOException;

@Stateful(name = "CenterAreaEJB")
public class CenterAreaNode extends GenericAreaNode implements AreaCenter {
    @PostConstruct
    public void init(){
        try {
            myDestinationName = myHierarchyConnectionsRetriever.getMyDestinationName("center");
            myKVManager = new KVManagerImpl("center");
            myKVManager.deleteAllClientRequest();
            myConsumer.initializeParameters(myDestinationName, myHierarchyConnectionsRetriever.getChildrenDestinationName("center"), myHierarchyConnectionsRetriever.getParentDestinationName("center"));
            setQueueConsumer(myDestinationName);
            startReceivingLoop();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
