package it.unipi.dii.inginf.dsmt.covidtracker.web.servlets;

import com.google.gson.Gson;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.HierarchyConnectionsRetriever;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.Producer;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.RegionWebConsumer;
import it.unipi.dii.inginf.dsmt.covidtracker.web.servlets.ejbs.RegionWebConsumerBean;
import org.json.simple.parser.ParseException;

import javax.ejb.EJB;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "RegionServlet", urlPatterns={"/region/*"})
public class RegionServlet extends HttpServlet {

    private String regionQueueName;
    private String regionTopicName;

    final static String TC_FACTORY_NAME = "jms/__defaultConnectionFactory";

    @EJB
    public static Producer myProducer;

    @EJB
    static RegionWebConsumer myConsumer;
    @EJB
    static HierarchyConnectionsRetriever myHierarchyConnectionsRetriever;

    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(true);
        String region = (String) session.getAttribute("region");
        if(region != null) {
            try {
                regionQueueName = myHierarchyConnectionsRetriever.getMyDestinationName(region);
                regionTopicName = myHierarchyConnectionsRetriever.getTopicDestinationName(region);

                setMessageListener(regionTopicName);

                resp.setContentType("text/html");
                PrintWriter out = resp.getWriter();

                out.println("<HTML> <HEAD> <TITLE> Covid Tracker </TITLE> </HEAD> <BODY BGCOLOR=white>");
                out.println("<CENTER> <FONT size=+4> Region page sending requests to " + regionQueueName + "</FONT> </CENTER> <br> <p> ");

                out.println("<h2>Log new data</h2>");
                out.println("<form action=\"" + req.getContextPath() + "/region/regionUI.jsp\" method=\"GET\">");

                out.println("<label for=\"log_type\">Choose a log type:</label>");
                out.println("<select name=\"log_type\" id=\"log_type\">");
                out.println("<option value=\"swab\">Swab</option>");
                out.println("<option value=\"positive\">Positive</option>");
                out.println("<option value=\"negative\">Negative</option>");
                out.println("<option value=\"dead\">Dead</option>");
                out.println("</select> <br><br>");

                out.println("<label for=\"log_quantity\">Insert the quantity to be logged:</label>");
                out.println("<input type=\"text\" name=\"log_quantity\"> <br><br>");

                out.println("<input type=\"submit\" name=\"Submit_Log\">");
                out.println("</form>");

                out.println("<br><br><br>");

                out.println("<h2>Request an aggregation</h2>");
                out.println("<form action=\"" + req.getContextPath() + "/region/regionUI.jsp\" method=\"GET\">");

                out.println("<label for=\"log_aggr_type\">Choose the type to aggregate:</label>");
                out.println("<select name=\"log_aggr_type\" id=\"log_aggr_type\">");
                out.println("<option value=\"swab\">Swab</option>");
                out.println("<option value=\"positive\">Positive</option>");
                out.println("<option value=\"negative\">Negative</option>");
                out.println("<option value=\"dead\">Dead</option>");
                out.println("</select> <br><br>");

                out.println("<label for=\"aggr_type\">Choose the type of the aggregation:</label>");
                out.println("<select name=\"aggr_type\" id=\"aggr_type\">");
                out.println("<option value=\"sum\">Sum</option>");
                out.println("<option value=\"avg\">Average</option>");
                out.println("<option value=\"standard_deviation\">Standard Deviation</option>");
                out.println("</select> <br><br>");

                out.println("<label for=\"start_date\">Beginning of the period:</label>");
                out.println("<input type=\"date\" name=\"start_date\"> <br><br>");

                out.println("<label for=\"end_date\">End of the period:</label>");
                out.println("<input type=\"date\" name=\"end_date\"> <br><br>");

                out.println("<input type=\"submit\" name=\"Submit_aggrReq\">");
                out.println("</form> <br>");


                String logType = req.getParameter("log_type");
                if (logType != null) {
                    out.println("<FONT size=+1 color=red> Message back from StatelessSessionBean: </FONT>"
                            + "<br>"+ logType + "<br>");
                }

                String logAggrType = req.getParameter("log_aggr_type");
                if (logAggrType != null) {
                    out.println("<FONT size=+1 color=red> Message back from StatelessSessionBean: </FONT>"
                            + "<br>"+ logAggrType + "<br>");
                }


                //stampa dei risultati delle aggregazioni
                out.println("<h2>Aggregation results:</h2>");
                out.println("<p>" + myConsumer.getAggregationResponses() +"</p>");


                //pulsante per refreshare pagina e stampare nuove aggregazioni
                out.println("<button onClick=\"window.location.reload();\">Update Results</button>");

                out.println("</BODY> </HTML> ");

            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("webclient servlet test failed");
                throw new ServletException(ex);
            }
        }
    }

    protected void setMessageListener(final String TOPIC_NAME) {
        try {
            Context ic = new InitialContext();
            Topic myTopic = (Topic) ic.lookup(TOPIC_NAME);
            TopicConnectionFactory tcf = (TopicConnectionFactory) ic.lookup(TC_FACTORY_NAME);
            tcf.createContext().createConsumer(myTopic).setMessageListener(myConsumer);
        } catch (NamingException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    protected void sendAggregationRequest(String queue, AggregationRequest aggregationRequest){
        Gson gson = new Gson();
        CommunicationMessage cMsg = new CommunicationMessage();
        cMsg.setSenderName("RegionWeb");
        cMsg.setMessageBody(gson.toJson(aggregationRequest));
        cMsg.setMessageType(MessageType.AGGREGATION_REQUEST);

        //salvo la richiesta tra le richieste effettuate nel bean
        myConsumer.addAggregation(aggregationRequest);

        myProducer.enqueue(queue, cMsg);
    }
}
