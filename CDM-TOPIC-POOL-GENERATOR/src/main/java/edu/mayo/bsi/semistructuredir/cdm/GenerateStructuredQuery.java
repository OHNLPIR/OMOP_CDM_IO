package edu.mayo.bsi.semistructuredir.cdm;

import edu.mayo.bsi.semistructuredir.cdm.compiler.Parser;
import edu.mayo.bsi.semistructuredir.cdm.elasticsearch.QueryGeneratorFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class GenerateStructuredQuery {
    public static void main(String... args) throws IOException {
        File topicsDir = new File("topics");
        for (File f : topicsDir.listFiles()) {
//            List<String> parsed = Files.readAllLines(f.toPath());
//            StringBuilder sB = new StringBuilder();
//            for (String s : parsed) {
//                sB.append(s).append("\n");
//            }
            FileWriter out = new FileWriter(new File(f.getName() + ".json"));
            out.write(Parser.generateQuery(f.getAbsolutePath()).toString());
            out.flush();
            out.close();
        }
    }
}
