package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.HierarchyConnectionsRetriever;
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
    static final String NATION_NAME = "nation";

    public HierarchyConnectionsRetrieverBean() {
    }

    JSONObject getJsonObject() throws IOException, ParseException {
        return (JSONObject) jsonParser.parse(new FileReader(FILE_PATH));
    }

    @Override
    public String getMyDestinationName(String nodeName) throws IOException, ParseException {
        return getJsonObject().get(nodeName).toString();
    }

    @Override
    public String getParentDestinationName(String childName) throws IOException, ParseException {
        String parentName = (String) getJsonObject().get(childName+"Parent");
        return getJsonObject().get(parentName).toString();
    }

    @Override
    public List<String> getChildrenDestinationName(String parentName) throws IOException, ParseException {
        List<String> childrenDestName = new ArrayList<>();

        List<String> childrenList = getChildrenNames(parentName);
        for (String childName: childrenList) {
            childrenDestName.add((String) getJsonObject().get(childName));
        }

        return childrenDestName;
    }

    @Override
    public String getNationName() throws IOException, ParseException {
        return NATION_NAME;
    }

    @Override
    public List<String> getAllAreasName() throws IOException, ParseException {
        List<String> areasName = new ArrayList<>();

        JSONArray areasList = (JSONArray) getJsonObject().get("nationChildren");
        for (int i = 0; i < areasList.size(); i++) {
            String areaName = areasList.get(i).toString();
            areasName.add(areaName);
        }
        return areasName;
    }

    @Override
    public List<String> getAllRegionsName() throws IOException, ParseException {
        List<String> regionsName = new ArrayList<>();

        JSONArray regionsList = (JSONArray) getJsonObject().get("regions");
        for (int i = 0; i < regionsList.size(); i++) {
            String regionName = regionsList.get(i).toString();
            regionsName.add(regionName);
        }
        return regionsName;
    }

    @Override
    public List<String> getAllNames() throws IOException, ParseException {
        List<String> names = new ArrayList<>();

        names.add(NATION_NAME);
        names.addAll(getChildrenNames(NATION_NAME));
        names.addAll(getAllRegionsName());

        return names;
    }

    //------------------------------------------------------------------------------------------------------------------

    private List<String> getChildrenNames(String parentName) throws IOException, ParseException {
        List<String> childrenNames = new ArrayList<>();
        JSONArray childrenList = (JSONArray) getJsonObject().get(parentName+"Children");
        for (int i = 0; i < childrenList.size(); i++) {
            childrenNames.add(childrenList.get(i).toString());
        }
        return childrenNames;
    }
}
