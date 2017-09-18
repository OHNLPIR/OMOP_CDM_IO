package edu.mayo.omopindexer.indexing;

import edu.mayo.omopindexer.io.serializer.DocumentSerializer;
import edu.mayo.omopindexer.model.CDMModel;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Handler class with static methods for the ElasticSearch indexing pipeline
 */
public class ElasticSearchIndexer {

    private static String HOST;
    private static int PORT;
    private static String CLUSTER;
    private static String INDEX;
    private static Client ES_CLIENT;

    // Circumvent end-user forgetting to init via static constructor
    static {
        try {
            init();
            initializeESIndex();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Loads JSON Configuration Parameters **/
    private static void init() throws FileNotFoundException {
        JSONTokener tokenizer =
                new JSONTokener(ElasticSearchIndexer.class.getResourceAsStream("/configuration.json"));
        JSONObject obj = new JSONObject(tokenizer);
        HOST = obj.getString("host");
        PORT = obj.getInt("port");
        CLUSTER = obj.getString("cluster");
        INDEX = obj.getString("index_name");
        Settings s = ImmutableSettings.settingsBuilder()
                .put("cluster.name", CLUSTER).put("client.transport.sniff", true).build();
        ES_CLIENT = new TransportClient(s).addTransportAddress(new InetSocketTransportAddress(HOST, 9300));
    }

    /** Constructs indexes in Elasticsearch as appropriate based on configuration values */
    private static void initializeESIndex() throws IOException, ClassNotFoundException, NoSuchMethodException,
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
            childMappings.put(model.getModelTypeName(), childObject);
        }
        mapping.put("document", new JSONObject());
        for (Map.Entry<String, JSONObject> e : childMappings.entrySet()) {
            mapping.put(e.getKey(), e.getValue());
        }
        // Wrapper object sent to ES
        JSONObject submitToES = new JSONObject();
        submitToES.put("mappings", mapping);
        try {
            String base = "http://" + HOST + ":9200" + "/" + INDEX;
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
    public static void indexSerialized(DocumentSerializer document) {
        Deque<JSONObject> jsons = document.toElasticSearchIndexableJSONs();
        JSONObject parent = jsons.pollFirst();
        String docID = parent.getString("DocumentID");
        ES_CLIENT.prepareIndex(INDEX, "document", docID).setSource(parent.toString()).execute().actionGet(); // TODO this needs to be optimized
        JSONObject nextChild;
        while ((nextChild = jsons.pollFirst()) != null) {
            ES_CLIENT.prepareIndex(INDEX, nextChild.getString("type")).setParent(docID).setSource(nextChild.toString()).execute().actionGet(); // TODO possible resource leak with reindexing documents/infinite number of children...maybe purge all children when docujment already in index?
        }
    }
}
