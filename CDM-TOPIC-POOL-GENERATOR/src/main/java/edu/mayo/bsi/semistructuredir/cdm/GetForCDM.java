package edu.mayo.bsi.semistructuredir.cdm;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

public class GetForCDM {
    public static void main(String... args) throws IOException, UnirestException {
        File topicStructuredDir = new File("topic_desc");
        File outDir = new File("cdmView");
        outDir.mkdirs();
        for (File f : topicStructuredDir.listFiles()) {
            File out = new File(outDir, f.getName());
            FileWriter fW = new FileWriter(out);
            // - Obtain CDM artifacts following UIMA-REST-Server server request format
            HttpResponse<JsonNode> jsonResponse = Unirest.post("http://localhost:8080/")
                    .header("accept", "application/json")
                    .header("Content-Type", "application/json")
                    .body(new JSONObject()
                            .put("streamName", "cdm")
                            .put("metadata", (String) null)
                            .put("document", String.join(" ", Files.readAllLines(f.toPath())))
                            .put("serializers", Collections.singleton("cdm")))
                    .asJson();
            JSONObject obj = jsonResponse.getBody().getObject();
            fW.write(obj.toString(4));
            fW.flush();
            fW.close();
        }
    }
}
