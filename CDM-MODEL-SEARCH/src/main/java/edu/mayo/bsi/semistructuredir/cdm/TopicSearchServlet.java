package edu.mayo.bsi.semistructuredir.cdm;

import edu.mayo.bsi.semistructuredir.cdm.controllers.TopicSearchController;
import edu.mayo.bsi.semistructuredir.cdm.model.TopicResult;
import edu.mayo.bsi.semistructuredir.cdm.model.TopicResultEntry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.ui.ModelMap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@SpringBootApplication
@EnableAutoConfiguration
public class TopicSearchServlet extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(TopicSearchServlet.class);
    }

    //    public static void main(String... args) {
//        SpringApplication.run(TopicSearchServlet.class);
//    }

    /**
     * Dirty/undocumented demonstration code, not for production TODO
     * @param args
     * @throws IOException
     */
    public static void main(String... args) throws IOException {
        ModelMap in = new ModelMap();
        String[] topicIDs = new String[] {"01", "02", "07", "08"};
        new TopicSearchController().handleTopicRequest(in, topicIDs);
        for (String topicID : topicIDs) {
            TopicResult result = (TopicResult) in.get(topicID + "_results");
            System.out.println("Writing to " + topicID + ".pool");
            FileWriter out = new FileWriter(new File(topicID + ".pool"));
            out.write("Topic Desc: " + result.getTopicDesc() + "\r\n\r\n");
            out.write("Query:\r\n" + result.getQuery() + "\r\n");
            for (TopicResultEntry e : result.getResults()) {
                out.write(e.getResult());
                out.write("\t"+e.getScore());
                out.write("\t"+e.getDocumentText() + "\r\n");
            }
            out.flush();
            out.close();
        }
    }
}
