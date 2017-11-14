package edu.mayo.bsi.semistructuredir.cdm;

import edu.mayo.bsi.semistructuredir.cdm.compiler.Parser;
import org.apache.uima.pear.util.FileUtil;
import org.elasticsearch.index.query.QueryBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class StructuredDataQueryGenerator {

    public static void main(String... args) throws InterruptedException, IOException {
        File f = new File("topics");
        for (File t : f.listFiles(new FileUtil.ExtFilenameFilter("ssq"))) {
            QueryBuilder q = Parser.generateQuery(t.getAbsolutePath());
            FileWriter out = new FileWriter(new File(t.getName() + ".json"));
            out.write("{\"query\":" + q.toString() + "}");
            out.flush();
            out.close();
        }
    }
}
