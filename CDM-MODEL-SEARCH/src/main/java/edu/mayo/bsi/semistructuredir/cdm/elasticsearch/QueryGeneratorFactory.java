package edu.mayo.bsi.semistructuredir.cdm.elasticsearch;

import edu.mayo.bsi.semistructuredir.cdm.elasticsearch.impl.CDMQueryGenerator;
import edu.mayo.bsi.semistructuredir.cdm.elasticsearch.impl.StructuredQueryGenerator;
import edu.mayo.bsi.semistructuredir.cdm.elasticsearch.impl.TextQueryGenerator;

/**
 * Constructs query generators: a query generator supports generation of an
 * ElasticSearch query based on some input
 */
public interface QueryGeneratorFactory {

    /**
     * @return A new query generator capable of creating queries based on CDM models
     */
    static CDMQueryGenerator newCDMQuery() {
        return new CDMQueryGenerator();
    }

    /**
     * @return A new query generator capable of creating queries from raw text
     */
    static TextQueryGenerator newTextQuery() {
        return new TextQueryGenerator();
    }

    /**
     * @return A new query generator that parses structured query format queries
     */
    static StructuredQueryGenerator newStructuredQuery() {
            return new StructuredQueryGenerator();
    }
}
