package edu.mayo.bsi.semistructuredir.cdm.compiler;

import org.elasticsearch.index.query.QueryBuilder;

import java.io.File;

public class Test {
    public static File INPUT_FOLDER = new File("testdir");
    public static void main(String... args) {
        for (File f : INPUT_FOLDER.listFiles()) {
            try {
                QueryBuilder q = Parser.generateQuery(f.getAbsolutePath());
            } catch (Exception e) {
                System.out.println(f.getName());
                e.printStackTrace();
            } catch (Error e) {
                System.out.println(f.getName());
                e.printStackTrace();
            }
        }
    }
}
