package edu.mayo.bsi.semistructuredir.cdm.elasticsearch.impl;

import org.elasticsearch.index.query.QueryBuilder;

/**
 * Wrapper query generator for various text-based queries
 */
public class TextQueryGenerator {
    public RawTextQuery rawTextQuery(String field, String text) {
        return new RawTextQuery(field, text);
    }

    public MRFQuery mrfQuery(String field, String text, float termWeight, float orderedWeight, float unorderedWeight) {
        return new MRFQuery(field, text, termWeight, orderedWeight, unorderedWeight);
    }

    public interface TextQuery {
        boolean setOption(String opt, Object value);
        QueryBuilder build();
    }
}
