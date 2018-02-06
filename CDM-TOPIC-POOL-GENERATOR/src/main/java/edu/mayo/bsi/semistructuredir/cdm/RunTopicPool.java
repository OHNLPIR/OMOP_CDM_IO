package edu.mayo.bsi.semistructuredir.cdm;

import edu.mayo.bsi.semistructuredir.cdm.compiler.Parser;
import edu.mayo.bsi.semistructuredir.cdm.controllers.TopicSearchController;
import edu.mayo.bsi.semistructuredir.cdm.elasticsearch.QueryGeneratorFactory;
import edu.mayo.bsi.semistructuredir.cdm.model.TopicResult;
import edu.mayo.bsi.semistructuredir.cdm.model.TopicResultEntry;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.join.query.HasParentQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.*;


public class RunTopicPool {

    /**
     * Dirty/undocumented demonstration code, not for production TODO
     *
     * @throws IOException If file cannot be written
     */
    public static void main(String... ignored) throws IOException {
        File topicStructuredDir = new File("topics");
        Settings settings = Settings.builder() // TODO cleanup
                .put("cluster.name", "elasticsearch").build();
        try (TransportClient client = new PreBuiltTransportClient(settings).addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9310))) {
            HashMap in = new HashMap<>();
            ArrayList<String> names = new ArrayList<>(56);
            for (File f : new File("topics").listFiles()) {
                String name = f.getName().substring(0, f.getName().length() - 4);
                names.add(name);
            }
            Collections.sort(names);
            for (Similarity s : Similarity.values()) {
                setSimilarity("index", client, s);
                for (String topicID : names) {
                    File structuredFile = new File(topicStructuredDir, topicID + ".ssq");
                    QueryBuilder structuredQuery = Parser.generateQuery(structuredFile.getAbsolutePath());
                    SearchResponse scrollResp = client.prepareSearch("index")
                            .addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
                            .setScroll(new TimeValue(60000))
                            .setQuery(structuredQuery)
                            .setSize(100).get();
                    ArrayList<Integer> patientIDs = new ArrayList<>();
                    do {
                        for (SearchHit hit : scrollResp.getHits()) {
                            patientIDs.add(Integer.valueOf(hit.getId()));
                        }
                        scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
                    } while(scrollResp.getHits().getHits().length != 0);
                    File topicRawDir = new File("topic_desc");
                    File topicDesc = new File(topicRawDir, topicID + ".txt");
                    if (!topicDesc.exists()) {
                        throw new IllegalArgumentException("No descriptor found!");
                    }
                    String desc;
                    try {
                        List<String> file = Files.readAllLines(topicDesc.toPath());
                        StringBuilder sB = new StringBuilder();
                        for (String line : file) {
                            sB.append(line).append("\n");
                        }
                        desc = sB.toString();
                    } catch (IOException e) {
                        throw new RuntimeException(e); // TODO
                    }
                    QueryBuilder root;
                    if (patientIDs.size() > 0) {
                        root = QueryBuilders.termsQuery("person_id", patientIDs);
                    } else {
                        root =  new HasParentQueryBuilder("Encounter", QueryBuilders.matchAllQuery(), false);
                    }
                    System.out.println("Writing to " + topicID + "_" + s.name() + ".pool");
                    FileWriter out = new FileWriter(new File(new File("pools"),topicID + "_" + s.name() + ".pool"));
                    QueryBuilder textQuery = QueryGeneratorFactory.newTextQuery().rawTextQuery("RawText", desc).build();
                    System.out.println(textQuery.toString());
                    QueryBuilder actualQuery = QueryBuilders.boolQuery().should(textQuery).filter(new HasParentQueryBuilder("Encounter", new HasParentQueryBuilder("Person", root, false), false));
                    writeQueryResultsToFile(client, out, actualQuery);
                    out.flush();
                    out.close();
                    if (s.equals(Similarity.BM25)) {
                        System.out.println("Writing to " + topicID + "_mrf.pool");
                        out = new FileWriter(new File(new File("pools"),topicID + "_mrf.pool"));
                        textQuery = QueryGeneratorFactory.newTextQuery().mrfQuery(0.8f, 0.1f, 0.1f,"RawText", desc.split(" ")).build();
                        actualQuery = QueryBuilders.boolQuery().should(textQuery).filter(new HasParentQueryBuilder("Encounter", new HasParentQueryBuilder("Person", root, false), false));
                        System.out.println(textQuery.toString());
                        writeQueryResultsToFile(client, out, actualQuery);
                        out.flush();
                        out.close();
                    }
                }
            }
            setSimilarity("index", client, Similarity.BM25);
            for (String topicID : names) {
                new TopicSearchController().handleTopicRequest(in, topicID);
                TopicResult result = (TopicResult) in.get(topicID + "_results");
                System.out.println("Writing to " + topicID + ".pool");
                FileWriter out = new FileWriter(new File(new File("pools"),topicID + ".pool"));
                boolean flag = false;
                for (TopicResultEntry e : result.getResults()) {
                    if (!flag) {
                        flag = true;
                    } else {
                        out.write("\r\n");
                    }
                    out.write(e.getResult());
                    out.write("\t" + e.getScore());
                }
                out.flush();
                out.close();
            }
        }
        PoolCreator.main();
    }

    private static void writeQueryResultsToFile(TransportClient client, FileWriter out, QueryBuilder query) throws IOException {
        boolean flag = false;
        for (SearchHit e : client.prepareSearch("index").setQuery(query).setSize(1000).execute().actionGet().getHits()) {
            if (!flag) {
                flag = true;
            } else {
                out.write("\r\n");
            }
            out.write(e.getId());
            out.write("\t" + e.getScore());
        }
    }

    private static void setSimilarity(String index, TransportClient client, Similarity similarity) {
        client.admin().indices().prepareClose(index).get();
        client.admin().indices().prepareUpdateSettings("index").setSettings(Settings.builder().put("similarity.default.type", similarity)).get();
        client.admin().indices().prepareOpen(index).get();
        client.admin().cluster().prepareHealth(index).setWaitForYellowStatus().get();
    }

    enum Similarity {
        BM25,
        classic,
        LMDirichlet
    }
}
