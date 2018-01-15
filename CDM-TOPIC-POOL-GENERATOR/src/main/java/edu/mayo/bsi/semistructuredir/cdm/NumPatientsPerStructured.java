package edu.mayo.bsi.semistructuredir.cdm;

import edu.mayo.bsi.semistructuredir.cdm.compiler.Parser;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

public class NumPatientsPerStructured {
    public static void main(String... args) throws IOException {
        File topicsDir = new File("topics");
        FileWriter out = new FileWriter(new File("patientCounts.txt"));
        Settings settings = Settings.builder() // TODO cleanup
                .put("cluster.name", "elasticsearch").build();
        try (TransportClient client = new PreBuiltTransportClient(settings).addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9310))) {
            for (File f : topicsDir.listFiles()) {
                System.out.println("Getting for " + f.getName());
                QueryBuilder query = Parser.generateQuery(f.getAbsolutePath());
                SearchResponse scrollResp = client.prepareSearch("index")
                        .addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
                        .setScroll(new TimeValue(60000))
                        .setQuery(query)
                        .setSize(100).get();
                ArrayList<Integer> patientIDs = new ArrayList<>();
                do {
                    for (SearchHit hit : scrollResp.getHits()) {
                        patientIDs.add(Integer.valueOf(hit.getId()));
                    }
                    scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(6000000)).execute().actionGet();
                } while (scrollResp.getHits().getHits().length != 0);
                out.write(f.getName() + "\t" + patientIDs.size() + "\n");
                System.out.println(f.getName() + "\t" + patientIDs.size());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        out.flush();
        out.close();
    }
}
