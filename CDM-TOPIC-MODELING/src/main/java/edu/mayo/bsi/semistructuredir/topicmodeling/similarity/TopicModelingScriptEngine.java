package edu.mayo.bsi.semistructuredir.topicmodeling.similarity;

import org.elasticsearch.script.CompiledScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptEngineService;
import org.elasticsearch.script.SearchScript;
import org.elasticsearch.search.lookup.SearchLookup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TopicModelingScriptEngine implements ScriptEngineService {
    private static Map<String, double[]> TOPIC_WEIGHT_MAP; // lower case word -> weight vector by topic
    private static int NUM_TOPICS = 300;
    private static double[] EMPTY_TOPIC_VEC;

    @Override
    public String getType() {
        return "topic_modeling";
    }

    @Override
    public Object compile(String scriptName, String scriptSource, Map<String, String> params) {
        return scriptSource; // Just return the source, no compilation steps necessary
    }

    @Override
    public ExecutableScript executable(CompiledScript compiledScript, Map<String, Object> vars) {
        return null;
    }

    @Override
    public SearchScript search(CompiledScript compiledScript, SearchLookup lookup, Map<String, Object> vars) {
        String query = ((String)compiledScript.compiled()).toLowerCase();
        String[] queryArr = query.split(" "); // Simple space split tokenization because that's what mallet does too TODO cache this
        double[] queryVec = new double[NUM_TOPICS];
        for (String word : queryArr) {
            double[] vec = TOPIC_WEIGHT_MAP.getOrDefault(word, EMPTY_TOPIC_VEC);
            for (int i = 0; i < vec.length; i++) {
                queryVec[i] += vec[i];
            }
        }
        int queryLength = queryArr.length;
        for (int i = 0; i < queryVec.length; i++) {
            queryVec[i] /= queryLength;
        }
        String doc = lookup.source().getOrDefault("RawText", "").toString(); // TODO slow?
        String[] docArr = doc.split(" ");  // Simple space split tokenization because that's what mallet does too
        double[] docVec = new double[NUM_TOPICS];
        for (String word : docArr) {
            double[] vec = TOPIC_WEIGHT_MAP.getOrDefault(word, EMPTY_TOPIC_VEC);
            for (int i = 0; i < vec.length; i++) {
                docVec[i] += vec[i];
            }
        }
        int docLength = docArr.length;
        for (int i = 0; i < docVec.length; i++) {
            docVec[i] /= docLength;
        }
        double score = cosineSimilarity(docVec, queryVec);
        return null;
    }

    @Override
    public void close() throws IOException {

    }

    private static double cosineSimilarity(double[] vecA, double[] vecB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += Math.pow(vecA[i], 2);
            normB += Math.pow(vecB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
