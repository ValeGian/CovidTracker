package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import javafx.util.Pair;
import org.json.simple.parser.ParseException;

import javax.ejb.Remote;
import java.io.IOException;
import java.util.List;

@Remote
public interface HierarchyConnectionsRetriever {

    String getMyDestinationName(final String nodeName) throws IOException, ParseException;

    String getTopicDestinationName(final String nodeName) throws IOException, ParseException;

    String getParentDestinationName(final String nodeName) throws IOException, ParseException ;

    List<String> getChildrenDestinationName(final String nodeName) throws IOException, ParseException ;

    List<Pair<String, String>> getAllRegionsInfo() throws IOException, ParseException;

}
