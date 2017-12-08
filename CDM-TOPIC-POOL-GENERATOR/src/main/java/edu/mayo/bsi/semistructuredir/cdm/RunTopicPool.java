package edu.mayo.bsi.semistructuredir.cdm;

import edu.mayo.bsi.semistructuredir.cdm.controllers.TopicSearchController;
import edu.mayo.bsi.semistructuredir.cdm.elasticsearch.QueryGeneratorFactory;
import edu.mayo.bsi.semistructuredir.cdm.model.TopicResult;
import edu.mayo.bsi.semistructuredir.cdm.model.TopicResultEntry;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


public class RunTopicPool {

    /**
     * Dirty/undocumented demonstration code, not for production TODO
     *
     * @throws IOException If file cannot be written
     */
    public static void main(String... ignored) throws IOException {
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

                    System.out.println("Writing to " + topicID + "_" + s.name() + ".pool");
                    FileWriter out = new FileWriter(new File(topicID + "_" + s.name() + ".pool"));
                    QueryBuilder textQuery = QueryGeneratorFactory.newTextQuery().rawTextQuery("RawText", desc).build();
                    System.out.println(textQuery.toString());
                    boolean flag = false;
                    for (SearchHit e : client.prepareSearch("index").setQuery(textQuery).setSize(1000).execute().actionGet().getHits()) {
                        if (!flag) {
                            flag = true;
                        } else {
                            out.write("\r\n");
                        }
                        out.write(e.getId());
                        out.write("\t" + e.getScore());
                    }
                    out.flush();
                    out.close();
                    if (s.equals(Similarity.BM25)) {
                        System.out.println("Writing to " + topicID + "_mrf.pool");
                        out = new FileWriter(new File(topicID + "_mrf.pool"));
                        textQuery = QueryGeneratorFactory.newTextQuery().mrfQuery(0.8f, 0.1f, 0.1f,"RawText", desc.split(" ")).build();
                        System.out.println(textQuery.toString());
                        flag = false;
                        for (SearchHit e : client.prepareSearch("index").setQuery(textQuery).setSize(1000).execute().actionGet().getHits()) {
                            if (!flag) {
                                flag = true;
                            } else {
                                out.write("\r\n");
                            }
                            out.write(e.getId());
                            out.write("\t" + e.getScore());
                        }
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
                FileWriter out = new FileWriter(new File(topicID + ".pool"));
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
    }

    public static void setSimilarity(String index, TransportClient client, Similarity similarity) {
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
