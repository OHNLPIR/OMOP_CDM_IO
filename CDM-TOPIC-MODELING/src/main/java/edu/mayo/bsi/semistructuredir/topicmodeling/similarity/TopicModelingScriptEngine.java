package edu.mayo.bsi.semistructuredir.topicmodeling.similarity;

import org.apache.lucene.index.LeafReaderContext;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.index.fielddata.ScriptDocValues;
import org.elasticsearch.script.*;
import org.elasticsearch.search.lookup.IndexField;
import org.elasticsearch.search.lookup.IndexFieldTerm;
import org.elasticsearch.search.lookup.SearchLookup;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class TopicModelingScriptEngine implements ScriptEngineService {
    private static Map<String, double[]> TOPIC_WEIGHT_MAP; // lower case word -> weight vector by topic
    private static int NUM_TOPICS = 300;

    static {
        TOPIC_WEIGHT_MAP = new ConcurrentHashMap<>();
        File weights = new File("model.tsv");
        try {
            List<String> lines = Files.readAllLines(weights.toPath());
            for (String wordWeights : lines) {
                String[] arr = wordWeights.split("\t");
                String word = arr[0].toLowerCase();
                double[] vec = new double[NUM_TOPICS];
                for (int i = 0; i < vec.length; i++) {
                    vec[i] = Double.valueOf(arr[i + 1]);
                }
                TOPIC_WEIGHT_MAP.put(word, vec);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error in model", e);
        }
    }

    @Override
    public String getType() {
        return "topic_modeling";
    }

    @Override
    public Function<Map<String, Object>, SearchScript>  compile(String scriptName, String scriptSource, Map<String, String> params) {
        return TopicModelingSearchScript::new; // Return appropriate search script
    }

    @Override
    public ExecutableScript executable(CompiledScript compiledScript, Map<String, Object> vars) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SearchScript search(CompiledScript compiledScript, SearchLookup lookup, Map<String, Object> params) {
        Function<Map<String, Object>, SearchScript> scriptFactory = (Function<Map<String, Object>, SearchScript>) compiledScript.compiled();
        return scriptFactory.apply(params);

    }

    @Override
    public void close() {
    }

    private static class TopicModelingSearchScript implements SearchScript {

        /**
         * The query itself
         */
        private final String query;
        /**
         * An array of individual tokenized words
         */
        private final String[] queryArr;
        /**
         * A normalized weight vector representing weight for each topic represented by this query
         */
        private final double[] queryVec;

        TopicModelingSearchScript(Map<String, Object> params) {
            this.query = params.getOrDefault("query", "").toString();
            this.queryArr = this.query.split(" "); // Matches MALLET tokenization
            this.queryVec = new double[NUM_TOPICS];
            for (String word : queryArr) {
                double[] vec = TOPIC_WEIGHT_MAP.getOrDefault(word.toLowerCase(), new double[0]);
                for (int i = 0; i < vec.length; i++) {
                    queryVec[i] += vec[i];
                }
            }
            int queryLength = this.queryArr.length;
            for (int i = 0; i < this.queryVec.length; i++) {
                this.queryVec[i] /= queryLength;
            }
        }

        @Override
        public LeafSearchScript getLeafSearchScript(LeafReaderContext context) {
            return new TopicModelingInternalSearchScript(this.queryVec);
        }

        @Override
        public boolean needsScores() {
            return false;
        }
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

    private static class TopicModelingInternalSearchScript extends AbstractDoubleSearchScript {

        private double[] queryVec;

        TopicModelingInternalSearchScript(double[] queryVec) {
            this.queryVec = queryVec;
        }

        @Override
        public double runAsDouble() {
            try {
                ScriptDocValues.Strings textTerms = (ScriptDocValues.Strings) doc().get("RawText");
                List<Long> docLens = (List<Long>) doc().get("DocLength");
                if (docLens == null || docLens.size() == 0) {
                    docLens = Collections.singletonList(0L);
                }
                long docLen = docLens.get(0);
                double[] docVec = new double[NUM_TOPICS];
                IndexField field = indexLookup().get("RawText");
                for (String term : textTerms) {
                    double[] vec = TOPIC_WEIGHT_MAP.getOrDefault(term.toLowerCase(), new double[0]);
                    IndexFieldTerm fieldTerm = field.get(term);
                    int tf = fieldTerm.tf();
                    long df = fieldTerm.df();
                    double idf = 1 + Math.log((double)field.docCount()/((double)df + 1.0));
                    double tfidf = tf * idf;
                    for (int i = 0; i < vec.length; i++) {
                        vec[i] /= docLen;
                        vec[i] *= tfidf;
                        docVec[i] += vec[i];
                    }
                }
                return cosineSimilarity(docVec, this.queryVec);
            } catch (Exception e) {
                throw new ElasticsearchException("Error during scoring: ", e);
            }
        }
    }
}
