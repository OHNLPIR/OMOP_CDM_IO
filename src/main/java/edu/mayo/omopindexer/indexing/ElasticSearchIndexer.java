package edu.mayo.omopindexer.indexing;

import edu.mayo.omopindexer.model.CDMModel;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.HasParentQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Handler class with methods for the ElasticSearch indexing pipeline
 * Acts as a consumer thread for produced ElasticSearch operations
 */
public class ElasticSearchIndexer implements Runnable {

    private static ElasticSearchIndexer INSTANCE;
    private String HOST;
    private int PORT;
    private int HTTP_PORT;
    private String CLUSTER;
    private String INDEX;
    private Client ES_CLIENT;
    private BlockingDeque<RequestPair> requestQueue = new LinkedBlockingDeque<>();
    private boolean terminate = false;

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

    /** Loads JSON Configuration Parameters **/
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
        PORT = obj.getInt("port");
        HTTP_PORT = obj.getInt("http_port");
        CLUSTER = obj.getString("cluster");
        INDEX = obj.getString("index_name");
        Settings s = ImmutableSettings.settingsBuilder()
                .put("cluster.name", CLUSTER).put("client.transport.sniff", true).build();
        ES_CLIENT = new TransportClient(s).addTransportAddress(new InetSocketTransportAddress(HOST, PORT));
    }

    /** Constructs indexes in Elasticsearch as appropriate based on configuration values */
    private void initializeESIndex() throws IOException, ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
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
                _parent.put("type", "document");
                childObject.put("_parent", _parent);
            JSONObject properties = new JSONObject();
            for (Map.Entry<String, Object> e : model.getJSONMapping().toMap().entrySet()) {
                properties.put(e.getKey(), e.getValue());
            }
            childObject.put("properties", properties);
            childMappings.put(model.getModelTypeName(), childObject);
        }
        mapping.put("document", new JSONObject());
        for (Map.Entry<String, JSONObject> e : childMappings.entrySet()) {
            mapping.put(e.getKey(), e.getValue());
        }
        // Wrapper object sent to ES
        JSONObject submitToES = new JSONObject();
        submitToES.put("mappings", mapping);
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
     * TODO: queueing/batch system for performance reasons here
     */
    public void indexSerialized(CDMToJSONSerializer document) {
        Deque<JSONObject> jsons = document.toElasticSearchIndexableJSONs();
        JSONObject parent = jsons.pollFirst();
        String docID = parent.getString("DocumentID");
        // Clean up any children if the document already exists as they were re-generated
        HasParentQueryBuilder builder = QueryBuilders.hasParentQuery("document", QueryBuilders.termQuery("DocumentID", docID.toLowerCase()));
        SearchResponse resp = ES_CLIENT.prepareSearch(INDEX).setSearchType(SearchType.SCAN).setScroll(new TimeValue(60000)).setQuery(builder).setSize(100).execute().actionGet();
        BulkRequestBuilder bulkBuilder = ES_CLIENT.prepareBulk();
        boolean flag = resp.getHits().getHits().length > 0;
        while (true) {
            for (SearchHit hit : resp.getHits()) {
                bulkBuilder.add(ES_CLIENT.prepareDelete(hit.getIndex(), hit.getType(), hit.getId()));
            }
            resp = ES_CLIENT.prepareSearchScroll(resp.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();
            if (resp.getHits().getHits().length == 0) {
                break;
            }
        }
        if (flag) bulkBuilder.execute();
        // - Reinitialize for bulk builder
        bulkBuilder = ES_CLIENT.prepareBulk();
        // Index document itself
        bulkBuilder.add(ES_CLIENT.prepareIndex(INDEX, "document", docID).setSource(parent.toString()));
        // Index its children
        JSONObject nextChild;
        while ((nextChild = jsons.pollFirst()) != null) {
            bulkBuilder.add(ES_CLIENT.prepareIndex(INDEX, nextChild.getString("type")).setParent(docID).setSource(nextChild.toString()));
        }
        // Run the bulk request
        bulkBuilder.execute().actionGet();
    }

    public static ElasticSearchIndexer getInstance() {
        return INSTANCE;
    }

    public void terminate() {
        terminate = true;
    }

    @Override
    public void run() {
        // Local storage
        LinkedList<RequestPair> reqs = new LinkedList<>();
        // Get current queue states
        requestQueue.drainTo(reqs);
        if (reqs.size() > 0) {
            BulkRequestBuilder opBuilder = ES_CLIENT.prepareBulk();
            for (RequestPair req : reqs) {
                SearchResponse resp = req.deleteSearch.execute().actionGet();
                while (true) {
                    for (SearchHit hit : resp.getHits()) {
                        opBuilder.add(ES_CLIENT.prepareDelete(hit.getIndex(), hit.getType(), hit.getId()));
                    }
                    resp = ES_CLIENT.prepareSearchScroll(resp.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();
                    if (resp.getHits().getHits().length == 0) {
                        break;
                    }
                }
                opBuilder.add(req.indexReq);
            }
            opBuilder.execute();
        }
        if (!terminate || requestQueue.size() > 0) {
            run();

        }
    }

    /**
     * Used to store a delete request with its associated index request (clean up children first then index)
     */
    private static class RequestPair {
        SearchRequestBuilder deleteSearch;
        IndexRequestBuilder indexReq;

        public RequestPair(SearchRequestBuilder deleteSearch, IndexRequestBuilder indexReq) {
            this.deleteSearch = deleteSearch;
            this.indexReq = indexReq;
        }
    }
}
