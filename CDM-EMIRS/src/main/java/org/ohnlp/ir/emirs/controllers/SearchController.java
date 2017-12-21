package org.ohnlp.ir.emirs.controllers;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.*;

@Controller
public class SearchController {

    private TransportClient client;
    private Properties properties;
    private RestTemplate REST_CLIENT = new RestTemplate();
    private String UIMA_REST_URL = null;

    @RequestMapping(value = "/_query", method = RequestMethod.POST)
    public String postMapper(RedirectAttributes out, ModelMap model, @RequestParam Query query) throws IOException {
        if (client == null) {
            Settings settings = Settings.builder() // TODO cleanup
                    .put("cluster.name", properties.getEs().getClusterName()).build();
            client = new PreBuiltTransportClient(settings)
                    .addTransportAddress(
                            new InetSocketTransportAddress(
                                    InetAddress.getByName(properties.getEs().getHost()),
                                    properties.getEs().getPort()));
        }
        // Baseline: keep preexisting objects in model
        for (Map.Entry<String, Object> e : model.entrySet()) {
            out.addFlashAttribute(e.getKey(), e.getValue());
        }
        Query modelQuery = (Query) out.getFlashAttributes().get("query");
        if (modelQuery == null) {
            modelQuery = new Query();
            out.addFlashAttribute("query", modelQuery);
        }
        processQuery(modelQuery);
        QueryBuilder esQuery = query.toESQuery();
        SearchResponse resp = client.prepareSearch(properties.getEs().getIndexName())
                .setQuery(esQuery)
                .setSize(1000)
                .execute()
                .actionGet();
        processResponse(out, model, resp, modelQuery);
        return "redirect:/";
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
        if (query.getCdmQuery() == null) {
            ServerRequest req = new ServerRequest(properties.getUima().getQueueName(), null, query.getUnstructured(), Collections.singleton("cdm"));
            ServerResponse resp = REST_CLIENT.postForObject(UIMA_REST_URL, req, ServerResponse.class);
            String cdmRespRaw = resp.getContent().get(properties.getUima().getQueueName()).toString();
            JSONArray cdmResp = new JSONArray(cdmRespRaw);
            ArrayList<JSONObject> parsedCDMModels = new ArrayList<>(cdmResp.length());
            for (Object o : cdmResp) {
                parsedCDMModels.add(new JSONObject(new JSONTokener(o.toString())));
            }
            query.setCdmQuery(parsedCDMModels);
        }
    }

    private void processResponse(RedirectAttributes out, ModelMap model, SearchResponse resp, Query query) {
        QueryResult result = (QueryResult) model.get("results");
        if (result == null) {
            result = new QueryResult();
            model.put("results", result);
        }
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
        out.addFlashAttribute("results", result);
    }

    public Properties getProperties() {
        return properties;
    }

    @Autowired
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
