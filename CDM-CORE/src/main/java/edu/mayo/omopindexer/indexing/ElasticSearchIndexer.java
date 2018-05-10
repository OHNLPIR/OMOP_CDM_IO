package edu.mayo.omopindexer.indexing;

import edu.mayo.bigdata.elasticsearch.connection.ConnectionManager;
import edu.mayo.bigdata.elasticsearch.connection.Environment;
import edu.mayo.omopindexer.model.CDMPerson;
import edu.mayo.omopindexer.model.GeneratedEncounter;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;
import org.elasticsearch.join.query.HasParentQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handler class with methods for the ElasticSearch indexing pipeline
 * Acts as a consumer thread for produced ElasticSearch operations
 */
public class ElasticSearchIndexer extends Thread {

    private static ElasticSearchIndexer INSTANCE;
    private String INDEX;
    private RestHighLevelClient ES_CLIENT;
    private RestClient ES_LOWLEVEL_CLIENT;
    private BlockingDeque<RequestPair> requestQueue = new LinkedBlockingDeque<>(10000);
    private boolean terminate = false;
    private int MAX_CONCURRENT_QUEUED = 0;

    // Circumvent end-user forgetting to init via static constructor
    static {
        try {
            INSTANCE = new ElasticSearchIndexer();
            INSTANCE.init();
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
        INDEX = obj.getString("index_name");
        String user = obj.getString("user");
        String pass = obj.getString("pass");
        ConnectionManager bigdataClient = new ConnectionManager(user, pass, Environment.DEV);
        try {
            Field f = bigdataClient.getClass().getDeclaredField("restHighLevelClient");
            f.setAccessible(true);
            ES_CLIENT = (RestHighLevelClient) f.get(bigdataClient);
            ES_LOWLEVEL_CLIENT = bigdataClient.getRestClientBuilder().build();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        int numIndexingCores;
        if (System.getProperty("indexing.threads") != null) {
            numIndexingCores = Integer.valueOf(System.getProperty("indexing.threads"));
        } else {
            numIndexingCores = 1; // Should never be high enough to actually cause an overflow
        }
        MAX_CONCURRENT_QUEUED = Math.toIntExact(Math.round(Math.floor(1000D / numIndexingCores)));
    }

    /**
     * Indexes the given document to ElasticSearch index
     */
    public void indexSerialized(CDMToJSONSerializer docSerializer) {
        Deque<JSONObject> jsons = docSerializer.toElasticSearchIndexableJSONs();
        JSONObject document = jsons.pollFirst();
        String docID = document.getString("DocumentID");
        // Clean up any children if the document already exists as they were re-generated
//        QueryBuilder cleanupQuery = new HasParentQueryBuilder("Document", QueryBuilders.termQuery("DocumentID", docID.toLowerCase()), false);
//        try {
//            cleanupQuery.toXContent(XContentFactory.jsonBuilder(), Params.).toString()
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        Collection<IndexRequest> iReqs = new LinkedList<>();
        String encounterID = document.getString("Encounter_ID");
        String personID = document.getString("Person_ID");
        // Index its parent person
        CDMPerson person = PersonStaging.get(personID);
        if (person == null) {
            return; // Continue without indexing since its parent person will not exist
        }
        iReqs.add(new IndexRequest(INDEX, "Person", personID).version(person.getVersion()).versionType(VersionType.EXTERNAL).source(person.getAsJSON().toString(), XContentType.JSON));
        // Index the relevant encounter
        GeneratedEncounter encounterModel = EncounterStaging.get(encounterID);
        if (encounterModel == null) {
            return; // Continue without indexing since its parent encounter will not exist
        }
        iReqs.add(new IndexRequest(INDEX, "Encounter", encounterID).routing(personID).parent(personID).source(encounterModel.getAsJSON().toString(), XContentType.JSON));
        // Index document itself
        iReqs.add(new IndexRequest(INDEX, "Document", docID).source(document.toString(), XContentType.JSON).parent(encounterID).routing(personID));
        // Index its children
        JSONObject nextChild;
        while ((nextChild = jsons.pollFirst()) != null) {
            iReqs.add(new IndexRequest(INDEX, nextChild.getString("type")).parent(docID).routing(personID).source(nextChild.toString(), XContentType.JSON));
        }
        // Add the request to the request queue for processing
        try {
            requestQueue.putFirst(new RequestPair(null, iReqs));
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
                    BulkRequest opBuilder = new BulkRequest();
                    LinkedList<AtomicBoolean> barriers = new LinkedList<>(); // Storage for action futures to resynchronize with
                    for (RequestPair req : reqs) {
                        // - Await deletion completion
                        final AtomicBoolean completionFlag = new AtomicBoolean(false);
                        if (req.deleteSearch != null) {
                            HttpEntity ent = new NStringEntity(req.deleteSearch.source().toString());
                            ES_LOWLEVEL_CLIENT.performRequestAsync("POST", INDEX + "/_delete_by_query", Collections.EMPTY_MAP, ent, new ResponseListener() {
                                @Override
                                public void onSuccess(Response response) {
                                    synchronized (completionFlag) {
                                        completionFlag.set(true);
                                        completionFlag.notifyAll();
                                    }
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    synchronized (completionFlag) {
                                        completionFlag.set(true);
                                        completionFlag.notifyAll();
                                    }
                                }
                            });
                        }
                        barriers.add(completionFlag);
                        // - Add indexing requests
                        for (IndexRequest iReq : req.indexReqs) {
                            opBuilder.add(iReq);
                        }
                    }
//                     Wait for barrier resynchronization
                    for (final AtomicBoolean future : barriers) {
                        //noinspection SynchronizationOnLocalVariableOrMethodParameter
                        synchronized (future) {
                            while (!future.get()) {
                                try {
                                    future.wait(10000L);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    }
                    // Execute the indexing requests
                    ES_CLIENT.bulk(opBuilder);
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
        SearchRequest deleteSearch;
        Collection<IndexRequest> indexReqs;

        RequestPair(SearchRequest deleteSearch, Collection<IndexRequest> indexReqs) {
            this.deleteSearch = deleteSearch;
            this.indexReqs = indexReqs;
        }
    }
}
