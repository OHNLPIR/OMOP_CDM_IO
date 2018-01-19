package edu.mayo.omopindexer.indexing;

import edu.mayo.omopindexer.model.CDMModel;
import edu.mayo.omopindexer.model.CDMPerson;
import edu.mayo.omopindexer.model.GeneratedEncounter;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.join.query.HasParentQueryBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Handler class with methods for the ElasticSearch indexing pipeline
 * Acts as a consumer thread for produced ElasticSearch operations
 */
public class ElasticSearchIndexer extends Thread {

    private static ElasticSearchIndexer INSTANCE;
    private String HOST;
    private int HTTP_PORT;
    private String INDEX;
    private Client ES_CLIENT;
    private BlockingDeque<RequestPair> requestQueue = new LinkedBlockingDeque<>(10000);
    private boolean terminate = false;
    private int MAX_CONCURRENT_QUEUED = 0;

    // Circumvent end-user forgetting to init via static constructor
    static {
        try {
            INSTANCE = new ElasticSearchIndexer();
            INSTANCE.init();
            INSTANCE.initializeESIndex();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ElasticSearchIndexer() {
        super("Elasticsearch Indexing Queue");
    }

    /**
     * Loads JSON Configuration Parameters
     **/
    private void init() throws IOException {
        File jsonFile = new File("configuration.json");
        if (!jsonFile.exists()) {
            FileOutputStream fos = new FileOutputStream(jsonFile);
            InputStream fis = ElasticSearchIndexer.class.getResourceAsStream("/configuration.json");
            IOUtils.copy(fis, fos);
            fis.close();
            fos.close();
        }
        JSONTokener tokenizer =
                new JSONTokener(new FileInputStream(jsonFile));
        JSONObject obj = new JSONObject(tokenizer);
        HOST = obj.getString("host");
        int PORT = obj.getInt("port");
        HTTP_PORT = obj.getInt("http_port");
        String CLUSTER = obj.getString("cluster");
        INDEX = obj.getString("index_name");
        Settings s = Settings.builder()
                .put("cluster.name", CLUSTER).put("client.transport.sniff", true).put().build();
        ES_CLIENT = new PreBuiltTransportClient(s).addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(HOST), PORT));
        int numIndexingCores;
        if (System.getProperty("indexing.threads") != null) {
            numIndexingCores = Integer.valueOf(System.getProperty("indexing.threads"));
        } else {
            numIndexingCores = 1; // Should never be high enough to actually cause an overflow
        }
        MAX_CONCURRENT_QUEUED = Math.toIntExact(Math.round(Math.floor(1000D / numIndexingCores)));
    }

    /**
     * Constructs indexes in Elasticsearch as appropriate based on configuration values
     */
    private void initializeESIndex() throws IOException, ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        // Construct setting information for index
        JSONObject settings = new JSONObject().put("index", new JSONObject().put("number_of_shards", 20).put("number_of_replicas", 0));
        // Construct mapping information for index
        // Get Model files via reflection
        String prefix = "edu.mayo.omopindexer.model.";
        List<CDMModel> models = new LinkedList<>();
        for (CDMModel.Types type : CDMModel.Types.values()) {
            Class<? extends CDMModel> modelClazz = (Class<? extends CDMModel>) Class.forName(prefix + type);
            Constructor<? extends CDMModel> c = modelClazz.getDeclaredConstructor();
            c.setAccessible(true); // Set accessible to true to override private constructor restrictions
            CDMModel model = c.newInstance();
            models.add(model);
        }
        JSONObject mapping = new JSONObject();
        Map<String, JSONObject> childMappings = new HashMap<>();
        for (CDMModel model : models) {
            // https://www.elastic.co/guide/en/elasticsearch/reference/1.7/mapping-parent-field.html
            JSONObject childObject = new JSONObject();
            JSONObject _parent = new JSONObject();
            _parent.put("type", "Document");
            childObject.put("_parent", _parent);
            childObject.put("properties", model.getJSONMapping());
            childMappings.put(model.getModelTypeName(), childObject);
        }
        mapping.put("Document", new JSONObject().put("_parent", new JSONObject().put("type", "Encounter")));
        mapping.put("Encounter",
                new JSONObject()
                        .put("_parent", new JSONObject().put("type", "Person"))
                        .put("properties", GeneratedEncounter.generateEmpty().getJSONMapping()));
        JSONObject personObj = new JSONObject();
        personObj.put("properties", CDMPerson.generateEmpty().getJSONMapping());
        mapping.put("Person", personObj);
        for (Map.Entry<String, JSONObject> e : childMappings.entrySet()) {
            mapping.put(e.getKey(), e.getValue());
        }
        // Wrapper object sent to ES
        JSONObject submitToES = new JSONObject();
        submitToES.put("mappings", mapping);
        submitToES.put("settings", settings);
        System.out.print(submitToES.toString());
        try {
            String base = "http://" + HOST + ":" + HTTP_PORT + "/" + INDEX;
            URL indexURL = new URL(base);
            HttpURLConnection conn = (HttpURLConnection) indexURL.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
            osw.write(submitToES.toString());
            osw.flush();
            osw.close();
            conn.getResponseCode(); // Force update
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Indexes the given document to ElasticSearch index
     */
    public void indexSerialized(CDMToJSONSerializer docSerializer) {
        Deque<JSONObject> jsons = docSerializer.toElasticSearchIndexableJSONs();
        JSONObject document = jsons.pollFirst();
        String docID = document.getString("DocumentID");
        // Clean up any children if the document already exists as they were re-generated
        DeleteByQueryRequestBuilder cleanupQuery = DeleteByQueryAction.INSTANCE.newRequestBuilder(ES_CLIENT).source(INDEX)
                .filter(new HasParentQueryBuilder("Document", QueryBuilders.termQuery("DocumentID", docID.toLowerCase()), false))
                .setSlices(10);
        Collection<IndexRequestBuilder> iReqs = new LinkedList<>();
        String encounterID = document.getString("Encounter_ID");
        String personID = document.getString("Person_ID");
        // Index its parent person
        CDMPerson person = PersonStaging.get(personID);
        if (person == null) {
            return; // Continue without indexing since its parent person will not exist
        }
        iReqs.add(ES_CLIENT.prepareIndex(INDEX, "Person", personID).setVersion(person.getVersion()).setVersionType(VersionType.EXTERNAL).setSource(person.getAsJSON().toString(), XContentType.JSON));
        // Index the relevant encounter
        GeneratedEncounter encounterModel = EncounterStaging.get(encounterID);
        if (encounterModel == null) {
            return; // Continue without indexing since its parent encounter will not exist
        }
        iReqs.add(ES_CLIENT.prepareIndex(INDEX, "Encounter", encounterID).setRouting(personID).setParent(personID).setSource(encounterModel.getAsJSON().toString(), XContentType.JSON));
        // Index document itself
        iReqs.add(ES_CLIENT.prepareIndex(INDEX, "Document", docID).setSource(document.toString(), XContentType.JSON).setParent(encounterID).setRouting(personID));
        // Index its children
        JSONObject nextChild;
        while ((nextChild = jsons.pollFirst()) != null) {
            iReqs.add(ES_CLIENT.prepareIndex(INDEX, nextChild.getString("type")).setParent(docID).setRouting(personID).setSource(nextChild.toString(), XContentType.JSON));
        }
        // Add the request to the request queue for processing
        try {
            requestQueue.putFirst(new RequestPair(cleanupQuery, iReqs));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static ElasticSearchIndexer getInstance() {
        return INSTANCE;
    }

    public void terminate() {
        terminate = true;
    }

    @Override
    public void run() {
        try (FileWriter writer = new FileWriter(new File(UUID.randomUUID() + ".out"))) {
            while (true) {
                // Local storage
                LinkedList<RequestPair> reqs = new LinkedList<>();
                // Get current queue states - only do maximum of 100 elements per thread at a time
                requestQueue.drainTo(reqs, MAX_CONCURRENT_QUEUED);
                boolean flag = reqs.size() > 0;
                writer.write("Processing " + reqs.size() + "\r\n");
                writer.flush();
                if (flag) {
                    BulkRequestBuilder opBuilder = ES_CLIENT.prepareBulk();
                    LinkedList<ActionFuture> barriers = new LinkedList<>(); // Storage for action futures to resynchronize with
                    for (RequestPair req : reqs) {
                        // - Await deletion completion
//                        barriers.add(req.deleteSearch.execute());
                        // - Add indexing requests
                        for (IndexRequestBuilder iReq : req.indexReqs) {
                            opBuilder.add(iReq);
                        }
                    }
                    // Wait for barrier resynchronization
                    for (ActionFuture future : barriers) {
                        try {
                            future.get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                    // Execute the indexing requests
                    opBuilder.execute();
                }
                if (!terminate || requestQueue.size() > 0) { // Continue consuming if we are either not terminated or still have stuff in queue
                    if (reqs.size() < MAX_CONCURRENT_QUEUED) {
                        // Wait if we consumed less than maximum queue, otherwise run again immediately
                        try {
                            sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    break; // Exit conditions reached, quit thread
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Used to store a delete request with its associated index request (clean up children first then index)
     */
    private static class RequestPair {
        DeleteByQueryRequestBuilder deleteSearch;
        Collection<IndexRequestBuilder> indexReqs;

        RequestPair(DeleteByQueryRequestBuilder deleteSearch, Collection<IndexRequestBuilder> indexReqs) {
            this.deleteSearch = deleteSearch;
            this.indexReqs = indexReqs;
        }
    }
}
