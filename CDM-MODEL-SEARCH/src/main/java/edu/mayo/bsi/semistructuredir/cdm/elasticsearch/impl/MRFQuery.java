package edu.mayo.bsi.semistructuredir.cdm.elasticsearch.impl;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Generates a markov random fields query based on a collection of individual tokens
 */
public class MRFQuery implements TextQueryGenerator.TextQuery {

    private final String field;
    private final String[] tokens;
    private float termWeight;
    private float orderedWeight;
    private float unorderedWeight;
    private int ngramLimit = 5;
    private List<String[]> combinations;
    private List<int[]> indexCombinations;

    public MRFQuery(float termWeight, float orderedWeight, float unorderedWeight, String field, String... tokens) {
        this.field = field;
        this.tokens = tokens;
        this.termWeight = termWeight;
        this.orderedWeight = orderedWeight;
        this.unorderedWeight = unorderedWeight;
        this.combinations = new LinkedList<>();
        this.indexCombinations = new LinkedList<>();
    }

    @Override
    public boolean setOption(String opt, Object value) {
        return false;
    }

    @Override
    public QueryBuilder build() {
        QueryBuilder terms = QueryBuilders.termsQuery(field, tokens);
        // Find combinations
        for (int i = 2; i <= tokens.length; i++) { // Build up to tokens.length ordered, skip 1 since that's terms
            // On this iteration we are recursively building i terms
            buildOrderedCombinations(tokens, i, 0, new String[i], new int[i]);
        }
        combinations = new ArrayList<>(combinations);
        indexCombinations = new ArrayList<>(indexCombinations);
        BoolQueryBuilder ordered = QueryBuilders.boolQuery();
        BoolQueryBuilder unordered = QueryBuilders.boolQuery();
        // Select those with continuous portions
        for (int i = 0; i < indexCombinations.size(); i++) {
            String[] combination = combinations.get(i);
            // Add to ordered if continuous
            if (isContinuous(indexCombinations.get(i))) {
                QueryBuilder orderedPhrase = QueryBuilders.matchPhraseQuery(field, combination).boost(orderedWeight);
                ordered.should(orderedPhrase);
                // Add to unordered queries
                QueryBuilder unorderedPhrase =
                        QueryBuilders.matchPhraseQuery(field, combination).slop(4 * combination.length);
                unordered.should(unorderedPhrase);
            }
        }
        return QueryBuilders.boolQuery()
                .should(terms.boost(termWeight))
                .should(ordered.boost(orderedWeight))
                .should(unordered.boost(unorderedWeight));
    }

    private boolean isContinuous(int[] args) {
        // TODO: something more efficient perhaps?
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i] + 1 != args[i + 1]) {
                return false;
            }
        }
        return true;
    }

    private void buildOrderedCombinations(String[] tokens, int length, int idx, String[] result, int[] resultIdx) {
        if (length == 0) {
            if (result.length <= ngramLimit) {
                String[] copy = new String[result.length];
                int[] idxCopy = new int[result.length];
                System.arraycopy(result, 0, copy, 0, result.length);
                System.arraycopy(resultIdx, 0, idxCopy, 0, result.length);
                combinations.add(copy);
                indexCombinations.add(idxCopy);
            }
        } else {
            for (int i = idx; i <= tokens.length - length; i++) {
                result[result.length - length] = tokens[i];
                resultIdx[result.length - length] = i;
                buildOrderedCombinations(tokens, length - 1, i + 1, result, resultIdx);
            }
        }
    }
}
