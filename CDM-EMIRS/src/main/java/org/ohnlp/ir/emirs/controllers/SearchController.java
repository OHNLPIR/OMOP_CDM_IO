package org.ohnlp.ir.emirs.controllers;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.mayo.bsi.uima.server.rest.models.ServerRequest;
import edu.mayo.bsi.uima.server.rest.models.ServerResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.ohnlp.ir.emirs.Properties;
import org.ohnlp.ir.emirs.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

@Controller
public class SearchController {

    private TransportClient client;
    private Properties properties;
    private RestTemplate REST_CLIENT = new RestTemplate();
    private String UIMA_REST_URL = null;

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public @ResponseBody QueryResult postMapper(@RequestBody Query query) throws IOException {
        if (client == null) {
            Settings settings = Settings.builder() // TODO cleanup
                    .put("cluster.name", properties.getEs().getClusterName()).build();
            client = new PreBuiltTransportClient(settings)
                    .addTransportAddress(
                            new InetSocketTransportAddress(
                                    InetAddress.getByName(properties.getEs().getHost()),
                                    properties.getEs().getPort()));
        }
//        Query modelQuery = (Query) out.getFlashAttributes().get("query");
//        if (modelQuery == null) {
//            modelQuery = new Query();
//            out.addFlashAttribute("query", modelQuery);
//        }
//        processQuery(modelQuery);
        processQuery(query);
        QueryBuilder esQuery = query.toESQuery();
        SearchResponse resp = client.prepareSearch(properties.getEs().getIndexName())
                .setQuery(esQuery)
                .setSize(1000)
                .execute()
                .actionGet();
//        processResponse(out, model, resp, query);
        return processResponse(resp, query);
    }

    /**
     * Populates model query fields from unstructured text query,
     * @param query
     */
    private void processQuery(Query query) {
        // Initialize values from properties if needed
        if (UIMA_REST_URL == null) {
            UIMA_REST_URL = "http://" + properties.getUima().getHost() + ":" + properties.getUima().getPort() + "/";
        }
        if (query.getCdmQuery() == null || query.getCdmQuery().size() == 0) {
            ServerRequest req = new ServerRequest(properties.getUima().getQueue(), null, query.getUnstructured(), Collections.singleton("cdm"));
            ServerResponse resp = REST_CLIENT.postForObject(UIMA_REST_URL, req, ServerResponse.class);
            String cdmRespRaw = resp.getContent().get(properties.getUima().getQueue());
            JSONArray cdmResp = new JSONArray(cdmRespRaw);
            ArrayList<JsonNode> parsedCDMModels = new ArrayList<>(cdmResp.length());
            for (int i = 0; i < cdmResp.length(); i++) {
                Object o = cdmResp.get(i);
                try {
                    parsedCDMModels.add(new ObjectMapper().readValue(o.toString(), ObjectNode.class));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            query.setCdmQuery(parsedCDMModels);
        }
    }

    private QueryResult processResponse(SearchResponse resp, Query query) {
        QueryResult result = new QueryResult();
        Map<String, Patient> patientMap = new HashMap<>();
        Map<String, Integer> patientFreqMap = new HashMap<>();
        // Associated Query
        result.setQuery(query);
        // Associated Documents
        List<QueryHit> hits = new LinkedList<>();
        for (SearchHit hit : resp.getHits()) {
            QueryHit qHit = new QueryHit();
            // Document
            Document doc = new Document();
            Map<String, Object> source = hit.getSource();
            String docIDRaw = source.get("DocumentID").toString(); // TODO this is really specific/infrastructure dependent way of doing things
            String[] docFields = docIDRaw.split("_");
            doc.setDocLinkId(docFields[1]);
            doc.setRevision(docFields[2]);
            doc.setDocType(docFields[3]); //TODO
            doc.setText(source.get("RawText").toString());
            doc.setSectionName(source.get("Section_Name").toString());
            doc.setSectionID(source.get("Section_ID").toString());
            qHit.setDoc(doc);
            // Encounter
            Encounter encounter = new Encounter();
            String[] encounterParts = source.get("Encounter_ID").toString().split(":"); //mrn:encounter_tmr:dob
            encounter.setEncounterDate(new Date(new Long(encounterParts[1])));
            encounter.setEncounterAge(Long.valueOf(encounterParts[1]) - Long.valueOf(encounterParts[2])); // date - dob
            qHit.setEncounter(encounter);
            // Patient
            String pid = docFields[0].trim();
            Patient patient = patientMap.computeIfAbsent(pid, k -> {
                Patient p = new Patient();
                p.setId(pid);
                return p;
            });
            qHit.setPatient(patient);
            qHit.setScore(hit.getScore());
            hits.add(qHit);
            patientFreqMap.merge(pid, 1, (k,v) -> v + 1);
        }
        // Associated Patients
        // - Order them
        List<Map.Entry<String, Integer>> sortable = new ArrayList<>(patientFreqMap.entrySet());
        sortable.sort((e1, e2) -> e2.getValue() - e1.getValue());
        List<Patient> retList = new LinkedList<>();
        for (Map.Entry<String, Integer> e : sortable) {
            retList.add(patientMap.get(e.getKey()));
        }
        result.setPatients(retList);
        result.setHits(hits);
        return result;
    }

    public Properties getProperties() {
        return properties;
    }

    @Autowired
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
