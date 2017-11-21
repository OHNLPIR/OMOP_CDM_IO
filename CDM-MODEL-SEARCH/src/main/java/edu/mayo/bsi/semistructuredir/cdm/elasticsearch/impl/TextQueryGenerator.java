package edu.mayo.bsi.semistructuredir.cdm.elasticsearch.impl;

import org.elasticsearch.index.query.QueryBuilder;

/**
 * Wrapper query generator for various text-based queries
 */
public class TextQueryGenerator {
    public RawTextQuery rawTextQuery(String field, String text) {
        return new RawTextQuery(field, text);
    }

    public MRFQuery mrfQuery(float termWeight, float orderedWeight, float unorderedWeight, String field, String... text) {
        return new MRFQuery(termWeight, orderedWeight, unorderedWeight, field, text);
    }

    public interface TextQuery {
        TextQuery setOption(String opt, Object value);
        QueryBuilder build();
    }
}
