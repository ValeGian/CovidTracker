package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import org.json.simple.parser.ParseException;

import javax.ejb.Remote;
import java.io.IOException;
import java.util.List;

@Remote
public interface HierarchyConnectionsRetriever {

    String getMyDestinationName(final String nodeName) throws IOException, ParseException;

    String getParentDestinationName(final String childName) throws IOException, ParseException;

    List<String> getChildrenDestinationName(final String parentName) throws IOException, ParseException;

    String getNationName() throws IOException, ParseException;

    List<String> getAllAreasName() throws IOException, ParseException;

    List<String> getAllRegionsName() throws IOException, ParseException;

    List<String> getAllNames() throws IOException, ParseException;

    List<String> getChildrenNames(final String parentName) throws IOException, ParseException;

}
