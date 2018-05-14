package org.ohnlp.ir.create.controllers;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.mayo.bigdata.elasticsearch.connection.ConnectionManager;
import edu.mayo.bigdata.elasticsearch.connection.Environment;
import edu.mayo.bsi.uima.server.rest.models.ServerRequest;
import edu.mayo.bsi.uima.server.rest.models.ServerResponse;
import edu.mayo.nlp.bsi.uima.IntegratedUIMAServer;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ohnlp.ir.create.Properties;
import org.ohnlp.ir.create.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Logger;

@Controller
public class SearchController {

    private Properties properties;
    private RestTemplate REST_CLIENT = new RestTemplate();
    private String UIMA_REST_URL = null;
    private ConnectionManager BIGDATA_ES_CLIENT;
    private RestHighLevelClient ES_CLIENT;
    private IntegratedUIMAServer UIMA_SERVER = new IntegratedUIMAServer();

    public void init() {
        String user = properties.getEs().getUser();
        String pass = properties.getEs().getPass();
        try {
            BIGDATA_ES_CLIENT = new ConnectionManager(user, pass, Environment.DEV);
            Field f = BIGDATA_ES_CLIENT.getClass().getDeclaredField("restHighLevelClient");
            f.setAccessible(true);
            ES_CLIENT = (RestHighLevelClient) f.get(BIGDATA_ES_CLIENT);
        } catch (NoSuchFieldException | IllegalAccessException | IOException e) {
            throw new RuntimeException("Error connecting to elasticsearch", e);
        }
    }

    @RequestMapping(value = "/_search", method = RequestMethod.POST)
    public @ResponseBody
    QueryResult postMapper(@RequestBody Query query) throws IOException {
        if (ES_CLIENT == null) {
            init();
        }
        processQuery(query);
        QueryBuilder esQuery = query.toESQuery();
        SearchSourceBuilder sourceQuery = new SearchSourceBuilder()
                .query(esQuery)
                .size(10000)
                .sort(SortBuilders.scoreSort())
                .fetchSource(new String[]{"DocumentID", "Section_Name", "Section_ID", "Encounter_ID"}, new String[]{});
        SearchRequest req = new SearchRequest(properties.getEs().getIndexName())
                .source(sourceQuery);
        SearchResponse resp = BIGDATA_ES_CLIENT.getSearchResponse(req);
        return processResponse(resp, query);
    }

    /**
     * Populates model query fields from an input query,
     *
     * @param query
     */
    private void processQuery(Query query) throws IOException {
        // Initialize values from properties if needed
        if (UIMA_REST_URL == null) {
            UIMA_REST_URL = "http://" + properties.getUima().getHost() + ":" + properties.getUima().getPort() + "/";
        }
        if (query.getCdmQuery() == null) {
            query.setCdmQuery(getCDMObjects(query.getUnstructured()));
        }
        if (query.getStructured() != null && query.getStructured().size() > 0) {
            SearchSourceBuilder sourceQuery = new SearchSourceBuilder()
                    .query(query.getPatientIDFilterQuery())
                    .size(10000)
                    .sort(SortBuilders.scoreSort())
                    .fetchSource(false);
            SearchRequest req = new SearchRequest(properties.getEs().getIndexName())
                    .source(sourceQuery)
                    .scroll(TimeValue.timeValueMinutes(1));
            SearchResponse scrollResp = BIGDATA_ES_CLIENT.getSearchResponse(req);
            ArrayList<Integer> patientIDs = new ArrayList<>();
            do {
                for (SearchHit hit : scrollResp.getHits()) {
                    patientIDs.add(Integer.valueOf(hit.getId()));
                }
                scrollResp = BIGDATA_ES_CLIENT
                        .getScrollSearchResponse(new SearchScrollRequest(scrollResp.getScrollId())
                                .scroll(TimeValue.timeValueMinutes(1)));
            } while (scrollResp.getHits().getHits().length != 0);
            query.setPatientIDFilter(patientIDs);
        }
    }

    @RequestMapping(value = "/_patient", method = RequestMethod.POST)
    public @ResponseBody
    Patient getPatient(@RequestBody String id) {
        if (ES_CLIENT == null) {
            init();
        }
        QueryBuilder esQuery = QueryBuilders.idsQuery("Person").addIds(id);
        SearchSourceBuilder sourceQuery = new SearchSourceBuilder()
                .query(esQuery)
                .size(1)
                .fetchSource(true);
        SearchRequest req = new SearchRequest(properties.getEs().getIndexName())
                .types("Person")
                .source(sourceQuery);
        SearchResponse resp = null;
        try {
            resp = BIGDATA_ES_CLIENT.getSearchResponse(req);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    private Map<String, Patient> getPatients(Collection<String> ids) {
        if (ES_CLIENT == null) {
            init();
        }
        QueryBuilder esQuery = QueryBuilders.idsQuery("Person").addIds(ids.toArray(new String[ids.size()]));
        SearchSourceBuilder sourceQuery = new SearchSourceBuilder()
                .query(esQuery)
                .size(10000)
                .fetchSource(true);
        SearchRequest req = new SearchRequest(properties.getEs().getIndexName())
                .types("Person")
                .source(sourceQuery)
                .scroll(TimeValue.timeValueMinutes(1L));
        Map<String, Patient> ret = new HashMap<>();
        try {
            SearchResponse resp = BIGDATA_ES_CLIENT.getSearchResponse(req);
            SearchHit[] results = resp.getHits().getHits();
            do {
                if (results.length == 0) {
                    return Collections.emptyMap();
                } else {
                    for (SearchHit hit : results) {
                        JSONObject source = new JSONObject(hit.getSourceAsString());
                        Date dob = null;
                        Long dobMillis = source.optLong("date_of_birth", Long.MIN_VALUE);
                        if (dobMillis != Long.MIN_VALUE) {
                            dob = new Date(dobMillis);
                        }
                        Patient p = new Patient(
                                source.optString("person_id"),
                                source.optString("gender"),
                                source.optString("ethnicity"),
                                source.optString("race"),
                                source.optString("city"),
                                dob);
                        ret.put(p.getId(), p);
                    }
                }
                resp = BIGDATA_ES_CLIENT
                        .getScrollSearchResponse(new SearchScrollRequest(resp.getScrollId())
                                .scroll(TimeValue.timeValueMinutes(1)));
            } while (resp.getHits().getHits().length != 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @RequestMapping(value = "/_cdm", method = RequestMethod.POST)
    public @ResponseBody
    ArrayList<JsonNode> getCDMObjects(@RequestBody String text) {
        ServerRequest req = new ServerRequest(properties.getUima().getQueue(), null, text, Collections.singleton("cdm"));
        ServerResponse resp = UIMA_SERVER.submitJob(req);
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
        if (ES_CLIENT == null) {
            init();
        }
        QueryBuilder textQuery = QueryBuilders.matchQuery("DocumentID", docID);
        SearchSourceBuilder srcQuery = new SearchSourceBuilder()
                .query(textQuery).fetchSource(new String[]{"RawText"}, new String[0]);
        SearchResponse resp = BIGDATA_ES_CLIENT.getSearchResponse(
                new SearchRequest(properties.getEs().getIndexName())
                        .source(srcQuery)
        );
        if (resp.getHits().totalHits < 1) {
            return new ObjectMapper().readValue(new JSONObject().put("text", "No record found for " + docID).toString(), ObjectNode.class);
        } else if (resp.getHits().totalHits > 1) {
            Logger.getLogger(SearchController.class.getName()).warning("More than 1 matching record found for " + docID);
        }
        SearchHit retHit = resp.getHits().getHits()[0];
        return new ObjectMapper().readValue(new JSONObject().put("text", retHit.getSource().getOrDefault("RawText", "")).toString(), ObjectNode.class);
    }

    private QueryResult processResponse(SearchResponse resp, Query query) throws IOException {
        QueryResult result = new QueryResult();
        Map<String, Patient> patientMap = new HashMap<>();
        Map<String, Integer> patientFreqMap = new HashMap<>();
        // Associated Query
        result.setQuery(query);
        // Associated Documents
        List<DocumentHit> hits = new LinkedList<>();
//        int iteration = 0;
//        do {
//            Logger.getLogger("debug-log").info("Current iteration: " + iteration++);
//            if (iteration == 1) { // We already got top 10000 documents, assume the rest not relevant/minimal contribution to patient-level scoring
//                break;
//            }
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
            Patient patient = patientMap.computeIfAbsent(pid, k -> new Patient(pid));
            qHit.setPatient(patient);
            qHit.setScore(hit.getScore());
            hits.add(qHit);
            patientFreqMap.merge(pid, 1, (k, v) -> v + 1);
        }
//            resp = BIGDATA_ES_CLIENT.getScrollSearchResponse(new SearchScrollRequest(resp.getScrollId()).scroll(TimeValue.timeValueMinutes(1)));
//        } while (resp.getHits().getHits().length != 0);
        // Grab fully populated patient demographic information and repopulate here
        Map<String, Patient> fullyPopulatedPatients = getPatients(patientMap.keySet());
        fullyPopulatedPatients.forEach((s,p) -> {
            Patient target = patientMap.get(s);
            target.setGender(p.getGender());
            target.setEthnicity(p.getEthnicity());
            target.setRace(p.getRace());
            target.setCity(p.getCity());
            target.setDob(p.getDob());
        });
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
