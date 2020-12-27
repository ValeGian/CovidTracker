package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.HierarchyConnectionsRetriever;

import javax.ejb.Stateless;
import java.util.List;

@Stateless(name = "HierarchyConnectionsRetrieverEJB")
public class HierarchyConnectionsRetrieverBean implements HierarchyConnectionsRetriever {

    public HierarchyConnectionsRetrieverBean() {
    }

    @Override
    public String getMyDestinationName(String nodeName) {
        return null;
    }

    @Override
    public String getParentDestinationName(String nodeName) {
        return null;
    }

    @Override
    public List<String> getChildrenDestinationName(String nodeName) {
        return null;
    }
}
