package edu.mayo.omopindexer.indexing;

import edu.mayo.omopindexer.model.CDMModel;
import jdk.nashorn.internal.parser.JSONParser;
import org.json.JSONML;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

public class ElasticSearchIndexer {

    private static String HOST;
    private static int PORT;
    private static String CLUSTER;
    private static String INDEX;

    /** Loads JSON Configuration Parameters **/
    public static void main(String... args) throws FileNotFoundException {
        JSONTokener tokenizer = new JSONTokener(ElasticSearchIndexer.class.getResourceAsStream("/configuration.json"));
        JSONObject obj = new JSONObject(tokenizer);
        HOST = obj.getString("host");
        PORT = obj.getInt("port");
        CLUSTER = obj.getString("cluster");
        INDEX = obj.getString("index_name");
        System.out.println(HOST + ":" + PORT + "/" + CLUSTER + "/" + INDEX);
    }
    /** Constructs indexes in elasticsearch as appropriate based on configuration values */
    public static void initializeESIndex() throws IOException, ClassNotFoundException, NoSuchMethodException,
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
    }
}
