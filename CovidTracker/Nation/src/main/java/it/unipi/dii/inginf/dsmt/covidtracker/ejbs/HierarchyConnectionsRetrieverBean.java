package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.HierarchyConnectionsRetriever;
import javafx.util.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ejb.Stateless;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Stateless(name = "HierarchyConnectionsRetrieverEJB")
public class HierarchyConnectionsRetrieverBean implements HierarchyConnectionsRetriever {

    static final String FILE_PATH = "HierarchyConnections.json";
    static final JSONParser jsonParser = new JSONParser();

    public HierarchyConnectionsRetrieverBean() {
    }

    JSONObject getJsonObject() throws IOException, ParseException {
        return (JSONObject) jsonParser.parse(new FileReader(FILE_PATH));
    }

    @Override
    public String getMyDestinationName(String nodeName) throws IOException, ParseException {
        return (String) getJsonObject().get(nodeName);
    }

    @Override
    public String getParentDestinationName(String nodeName) throws IOException, ParseException {
        String parentName = (String) getJsonObject().get(nodeName+"Parent");
        return (String) getJsonObject().get(parentName);
    }

    @Override
    public List<String> getChildrenDestinationName(String nodeName) throws IOException, ParseException {
        List<String> childrenDestName = new ArrayList<>();

        JSONArray companyList = (JSONArray) getJsonObject().get(nodeName+"Children");
        Iterator<JSONObject> iterator = companyList.iterator();
        while (iterator.hasNext()) {
            String childName = iterator.next().toString();
            childrenDestName.add((String) getJsonObject().get(childName));
        }

        return childrenDestName;
    }

    @Override
    public List<Pair<String, String>> getAllRegionsInfo() throws IOException, ParseException {
        List<Pair<String, String>> regionsInfo = new ArrayList<>();

        JSONArray companyList = (JSONArray) getJsonObject().get("regions");
        Iterator<JSONObject> iterator = companyList.iterator();
        while (iterator.hasNext()) {
            String regionName = iterator.next().toString();
            String regionDestName = getMyDestinationName(regionName);
            regionsInfo.add(new Pair<>(regionName, regionDestName));
        }
        return regionsInfo;
    }
}
