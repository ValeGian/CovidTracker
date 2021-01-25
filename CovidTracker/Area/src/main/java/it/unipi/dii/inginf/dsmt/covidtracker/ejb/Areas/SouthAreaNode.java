package it.unipi.dii.inginf.dsmt.covidtracker.ejb.Areas;

import it.unipi.dii.inginf.dsmt.covidtracker.ejb.AreaConsumer;
import it.unipi.dii.inginf.dsmt.covidtracker.ejb.GenericAreaNode;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.areaInterfaces.AreaSouth;
import it.unipi.dii.inginf.dsmt.covidtracker.persistence.KVManagerImpl;
import org.json.simple.parser.ParseException;

import javax.annotation.PostConstruct;
import javax.ejb.Stateful;
import java.io.IOException;

@Stateful(name = "SouthAreaEJB")
public class SouthAreaNode extends GenericAreaNode implements AreaSouth {
    @PostConstruct
    public void init(){
        try {
            myDestinationName = myHierarchyConnectionsRetriever.getMyDestinationName("south");
            myKVManager = new KVManagerImpl("south");
            myKVManager.deleteAllClientRequest();
            myConsumer = new AreaConsumer(myKVManager, "south", myHierarchyConnectionsRetriever.getChildrenNames("south"));
            setQueueConsumer(myDestinationName);
            startReceivingLoop();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
