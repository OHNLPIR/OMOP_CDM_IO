package org.ohnlp.ir.emirs.controllers;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.mayo.bsi.uima.server.rest.models.ServerRequest;
import edu.mayo.bsi.uima.server.rest.models.ServerResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ohnlp.ir.emirs.Properties;
import org.ohnlp.ir.emirs.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Logger;

@Controller
public class SearchController {

    private TransportClient client;
    private Properties properties;
    private RestTemplate REST_CLIENT = new RestTemplate();
    private String UIMA_REST_URL = null;

    @RequestMapping(value = "/_search", method = RequestMethod.POST)
    public @ResponseBody
    QueryResult postMapper(@RequestBody Query query) throws IOException {
        if (client == null) {
            Settings settings = Settings.builder() // TODO cleanup
                    .put("cluster.name", properties.getEs().getClusterName()).build();
            client = new PreBuiltTransportClient(settings)
                    .addTransportAddress(
                            new InetSocketTransportAddress(
                                    InetAddress.getByName(properties.getEs().getHost()),
                                    properties.getEs().getPort()));
        }
        processQuery(query);
        QueryBuilder esQuery = query.toESQuery();
        SearchResponse resp = client.prepareSearch(properties.getEs().getIndexName())
                .setQuery(esQuery)
                .setSize(10000)
                .setScroll(new TimeValue(60000))
                .addSort(SortBuilders.scoreSort())
                .setFetchSource(new String[]{"DocumentID", "Section_Name", "Section_ID", "Encounter_ID"}, new String[]{})
                .execute()
                .actionGet();
        return processResponse(resp, query);
    }

    /**
     * Populates model query fields from an input query,
     *
     * @param query
     */
    private void processQuery(Query query) {
        // Initialize values from properties if needed
        if (UIMA_REST_URL == null) {
            UIMA_REST_URL = "http://" + properties.getUima().getHost() + ":" + properties.getUima().getPort() + "/";
        }
        if (query.getCdmQuery() == null) {
            query.setCdmQuery(getCDMObjects(query.getUnstructured()));
        }
        if (query.getStructured() != null && query.getStructured().size() > 0) {
            SearchResponse scrollResp = client.prepareSearch(properties.getEs().getIndexName())
                    .addSort(SortBuilders.scoreSort())
                    .setScroll(new TimeValue(60000))
                    .setQuery(query.getPatientIDFilterQuery())
                    .setSize(1000).get();
            ArrayList<Integer> patientIDs = new ArrayList<>();
            do {
                for (SearchHit hit : scrollResp.getHits()) {
                    patientIDs.add(Integer.valueOf(hit.getId()));
                }
                scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
            } while (scrollResp.getHits().getHits().length != 0);
            query.setPatientIDFilter(patientIDs);
        }
    }

    @RequestMapping(value = "/_patient", method = RequestMethod.POST)
    public @ResponseBody
    Patient getPatient(@RequestBody String id) {
        if (client == null) {
            Settings settings = Settings.builder() // TODO cleanup
                    .put("cluster.name", properties.getEs().getClusterName()).build();
            try {
                client = new PreBuiltTransportClient(settings)
                        .addTransportAddress(
                                new InetSocketTransportAddress(
                                        InetAddress.getByName(properties.getEs().getHost()),
                                        properties.getEs().getPort()));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        QueryBuilder esQuery = QueryBuilders.idsQuery("Person").addIds(id);
        SearchResponse resp = client.prepareSearch(properties.getEs().getIndexName())
                .setQuery(esQuery)
                .setFetchSource(true)
                .setSize(1)
                .execute()
                .actionGet();
        SearchHit[] results = resp.getHits().getHits();
        if (results.length == 0) {
            return null;
        } else {
            SearchHit hit = results[0];
            JSONObject source = new JSONObject(hit.getSourceAsString());
            Date dob = null;
            Long dobMillis = source.optLong("date_of_birth", Long.MIN_VALUE);
            if (dobMillis != Long.MIN_VALUE) {
                dob = new Date(dobMillis);
            }
            return new Patient(id,
                    source.optString("gender"),
                    source.optString("ethnicity"),
                    source.optString("race"),
                    source.optString("city"),
                    dob);
        }
    }

    @RequestMapping(value = "/_cdm", method = RequestMethod.POST)
    public @ResponseBody
    ArrayList<JsonNode> getCDMObjects(@RequestBody String text) {
        // Initialize values from properties if needed
        if (UIMA_REST_URL == null) {
            UIMA_REST_URL = "http://" + properties.getUima().getHost() + ":" + properties.getUima().getPort() + "/";
        }
        ServerRequest req = new ServerRequest(properties.getUima().getQueue(), null, text, Collections.singleton("cdm"));
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
        return parsedCDMModels;
    }

    @RequestMapping(value = "/_text", method = RequestMethod.POST)
    public @ResponseBody
    JsonNode getDocumentText(@RequestBody String docID) throws IOException {
        if (client == null) {
            Settings settings = Settings.builder() // TODO cleanup
                    .put("cluster.name", properties.getEs().getClusterName()).build();
            client = new PreBuiltTransportClient(settings)
                    .addTransportAddress(
                            new InetSocketTransportAddress(
                                    InetAddress.getByName(properties.getEs().getHost()),
                                    properties.getEs().getPort()));
        }
        QueryBuilder textQuery = QueryBuilders.matchQuery("DocumentID", docID);
        SearchResponse resp = client.prepareSearch(properties.getEs().getIndexName())
                .setFetchSource(new String[]{"RawText"}, new String[0])
                .setQuery(textQuery)
                .execute()
                .actionGet();
        if (resp.getHits().totalHits < 1) {
            return new ObjectMapper().readValue(new JSONObject().put("text", "No record found for " + docID).toString(), ObjectNode.class);
        } else if (resp.getHits().totalHits > 1) {
            Logger.getLogger(SearchController.class.getName()).warning("More than 1 matching record found for " + docID);
        }
        SearchHit retHit = resp.getHits().getHits()[0];
        return new ObjectMapper().readValue(new JSONObject().put("text", retHit.getSource().getOrDefault("RawText", "")).toString(), ObjectNode.class);
    }

    private QueryResult processResponse(SearchResponse resp, Query query) {
        QueryResult result = new QueryResult();
        Map<String, Patient> patientMap = new HashMap<>();
        Map<String, Integer> patientFreqMap = new HashMap<>();
        // Associated Query
        result.setQuery(query);
        // Associated Documents
        List<DocumentHit> hits = new LinkedList<>();
        int iteration = 0;
        do {
            Logger.getLogger("debug-log").info("Current iteration: " + iteration++);
            if (iteration == 20) { // We already got top 200000 documents
                break;
            }
            for (SearchHit hit : resp.getHits()) {
                DocumentHit qHit = new DocumentHit();
                // Document
                Document doc = new Document();
                Map<String, Object> source = hit.getSource();
                String docIDRaw = source.get("DocumentID").toString(); // TODO this is really specific/infrastructure dependent way of doing things
                String[] docFields = docIDRaw.split("_");
                doc.setDocLinkId(docFields[1]);
                doc.setRevision(docFields[2]);
                doc.setDocType(docFields[3]); //TODO
                doc.setIndexDocID(docIDRaw);
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
                // TODO: parallelize maybe
                Patient patient = patientMap.computeIfAbsent(pid, k -> getPatient(pid));
                qHit.setPatient(patient);
                qHit.setScore(hit.getScore());
                hits.add(qHit);
                patientFreqMap.merge(pid, 1, (k, v) -> v + 1);
            }
            resp = client.prepareSearchScroll(resp.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
        } while (resp.getHits().getHits().length != 0);

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
        result.calculatePatientHits();
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
