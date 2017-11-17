package edu.mayo.bsi.semistructuredir.cdm.controllers;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import edu.mayo.bsi.semistructuredir.cdm.compiler.Parser;
import edu.mayo.bsi.semistructuredir.cdm.elasticsearch.QueryGeneratorFactory;
import edu.mayo.bsi.semistructuredir.cdm.model.TopicListEntry;
import edu.mayo.bsi.semistructuredir.cdm.model.TopicResult;
import edu.mayo.bsi.semistructuredir.cdm.model.TopicResultEntry;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.join.query.HasChildQueryBuilder;
import org.elasticsearch.join.query.HasParentQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.*;

@Controller
public class TopicSearchController {

    /**
     * Handles a topic request
     *
     * @param model    The model map to pass to the JSP
     * @param topicIDs A list of topic IDs to filter/get results for
     * @return The page to return
     */
    @RequestMapping(method = RequestMethod.GET)
    public String handleTopicRequest(ModelMap model, @RequestParam("topic_id[]") String... topicIDs) {
        // No defined topic IDs, list topics
        if (topicIDs == null || topicIDs.length == 0) {
            File topicRawDir = new File("topic_desc");
            if (!topicRawDir.exists() || !topicRawDir.isDirectory()) {
                model.put("topic_list_ex", "Error: No Topic Descriptions Found!");
                return "topiclist";
            }
            File[] files = topicRawDir.listFiles();
            if (files == null || files.length == 0) {
                model.put("topic_list_ex", "Error: No Topic Descriptions Found!");
                return "topiclist";
            }
            HashMap<String, String> topicIDtoTopicContentMap = new HashMap<>();
            for (File f : files) {
                try {
                    List<String> file = Files.readAllLines(f.toPath());
                    StringBuilder sB = new StringBuilder();
                    for (String s : file) {
                        sB.append(s).append("\n");
                    }
                    String topicName = f.getName();
                    topicIDtoTopicContentMap.put(topicName.substring(0, topicName.length() - 4), sB.toString());
                } catch (IOException e) {
                    e.printStackTrace(); // TODO log
                }
            }
            ArrayList<String> topicNames = new ArrayList<>(topicIDtoTopicContentMap.keySet());
            Collections.sort(topicNames);
            LinkedList<TopicListEntry> topics = new LinkedList<>();
            for (String s : topicNames) {
                topics.add(new TopicListEntry(s, topicIDtoTopicContentMap.get(s)));
            }
            model.put("topic_list", topics);
            return "topiclist";
        } else {
            // Topic IDs defined, run query
            File topicStructuredDir = new File("topics");
            if (!topicStructuredDir.exists() || !topicStructuredDir.isDirectory()) {
                model.put("topic_result_ex", "Error: No Topics Found!");
                return "topicsearch";
            }
            File[] files = topicStructuredDir.listFiles();
            if (files == null || files.length == 0) {
                model.put("topic_result_ex", "Error: No Topics Found!");
                return "topicsearch";
            }
            Settings settings = Settings.builder() // TODO cleanup
                    .put("cluster.name", "elasticsearch").build();
            try (TransportClient client = new PreBuiltTransportClient(settings).addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9310))) {
                for (String topicID : topicIDs) {
                    if (model.containsKey(topicID + "_results")) {
                        continue; // Already got results for this
                    }
                    File structuredFile = new File(topicStructuredDir, topicID + ".ssq");
                    QueryBuilder structuredQuery = Parser.generateQuery(structuredFile.getAbsolutePath());
                    //TODO hardcoded
                    SearchResponse resp =
                            client.prepareSearch("index").setQuery(structuredQuery).setSize(1000).execute().actionGet();
                    ArrayList<Integer> patientIDs = new ArrayList<>((int) resp.getHits().totalHits);
                    for (SearchHit hit : resp.getHits()) {
                        patientIDs.add(Integer.valueOf(hit.getId()));
                    }
                    // we have these IDs, now run a document level search
                    // - Get topic description TODO do we really need to redo here?
                    File descFile = new File(new File("topic_desc"), topicID + ".txt");
                    String desc;
                    try {
                        List<String> file = Files.readAllLines(descFile.toPath());
                        StringBuilder sB = new StringBuilder();
                        for (String s : file) {
                            sB.append(s).append("\n");
                        }
                        desc = sB.toString();
                    } catch (IOException e) {
                        throw new RuntimeException(e); // TODO
                    }
                    // - Obtain CDM artifacts following UIMA-REST-Server server request format
                    HttpResponse<JsonNode> jsonResponse = Unirest.post("http://localhost:8080/")
                            .header("accept", "application/json")
                            .header("Content-Type", "application/json")
                            .body(new JSONObject()
                                    .put("streamName", "cdm")
                                    .put("metadata", (String) null)
                                    .put("document", desc)
                                    .put("serializers", Collections.singleton("cdm")))
                            .asJson();
                    JSONObject obj = jsonResponse.getBody().getObject();
                    QueryBuilder unstructuredQuery = handleNLPResponse(obj, patientIDs.toArray(new Integer[patientIDs.size()]));
                    QueryBuilder textQuery = QueryBuilders.matchQuery("RawText", desc);
                    QueryBuilder run;
                    if (unstructuredQuery != null) {
                        run = QueryBuilders.boolQuery().should(textQuery.boost(0.5f)).should(unstructuredQuery.boost(0.5f));
                    } else {
                        run = textQuery;
                    }
                    resp = client.prepareSearch().setQuery(run).setSize(1000).execute().actionGet();
                    LinkedList<TopicResultEntry> result = new LinkedList<>();
                    for (SearchHit hit : resp.getHits()) {
                        // DO we really want to add the document text? (no)
                        result.add(new TopicResultEntry(hit.getId(), null, hit.getScore()));
                    }
                    model.put(topicID + "_results", new TopicResult(topicID, desc, run.toString(), result));
                }
                return "topicsearch";
            } catch (UnknownHostException | UnirestException e) {
                e.printStackTrace(); // TODO log
                throw new RuntimeException(e);
            }
        }
    }


    public QueryBuilder handleNLPResponse(JSONObject resp, Integer... persons) {
        JSONObject content = resp.getJSONObject("content");
        if (content == null) {
            return null;
        }
        JSONArray result = content.getJSONArray("cdm"); // TODO magic values
        if (result == null || result.length() == 0) {
            return null;
        }
        Collection<JSONObject> models = new LinkedList<>();
        for (Object o : result) { // An array of JSON strings
            models.add(new JSONObject(new JSONTokener(o.toString())));
        }
        return QueryGeneratorFactory
                .newCDMQuery()
                .addCDMObjects(models.toArray(new JSONObject[models.size()]))
                .addFilteringPersons(persons).build();
    }
}
