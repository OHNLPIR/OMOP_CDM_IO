package edu.mayo.bsi.semistructuredir.cdm;

import edu.mayo.bsi.semistructuredir.cdm.compiler.Parser;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;

public class GetSocialHx {
    public static void main(String... args) throws IOException {
        File outDir = new File("social_hx");
        outDir.mkdirs();
        Settings settings = Settings.builder() // TODO cleanup
                .put("cluster.name", "elasticsearch").build();
        try (TransportClient client = new PreBuiltTransportClient(settings).addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9310))) {
                QueryBuilder query = QueryBuilders.matchPhraseQuery("Section_Name", "Social History");
                SearchResponse scrollResp = client.prepareSearch("index")
                        .addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
                        .setScroll(new TimeValue(60000))
                        .setQuery(query)
                        .setSize(10000).get();
                LinkedList<Tuple<String, String>> docs = new LinkedList<>();
                int iterations = 0;
                do {
                    for (SearchHit hit : scrollResp.getHits()) {
                        String docID = hit.getSource().get("DocumentID").toString();
                        String text = hit.getSource().get("RawText").toString();
                        docs.add(new Tuple<>(docID, text));
                    }
                    iterations++;
                    if (iterations >= 30) { // Limit to 300000 random docs
                        break;
                    }
                    scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(6000000)).execute().actionGet();
                } while (scrollResp.getHits().getHits().length != 0);
                System.out.println("Retrieved " + docs.size() + " docs");
                for (Tuple<String, String> doc : docs) {
                    FileWriter out = new FileWriter(new File(outDir, doc.v1() + ".txt"));
                    out.write(doc.v2());
                    out.flush();
                    out.close();
                }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
