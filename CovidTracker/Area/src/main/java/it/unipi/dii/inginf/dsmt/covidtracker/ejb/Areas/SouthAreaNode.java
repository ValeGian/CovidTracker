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
            myName = "south";
            myDestinationName = myHierarchyConnectionsRetriever.getMyDestinationName(myName);
            myKVManager = new KVManagerImpl(myName);
            myKVManager.deleteAllClientRequest();
            myConsumer = new AreaConsumer(myKVManager, myName, myHierarchyConnectionsRetriever.getChildrenNames(myName));
            setQueueConsumer(myDestinationName);
            startReceivingLoop();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
