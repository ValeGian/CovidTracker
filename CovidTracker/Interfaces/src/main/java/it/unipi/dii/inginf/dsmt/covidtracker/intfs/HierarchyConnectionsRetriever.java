package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import javax.ejb.Remote;
import java.util.List;

@Remote
public interface HierarchyConnectionsRetriever {

    String getMyDestinationName(final String nodeName);

    String getParentDestinationName(final String nodeName);

    List<String> getChildrenDestinationName(final String nodeName);

}
