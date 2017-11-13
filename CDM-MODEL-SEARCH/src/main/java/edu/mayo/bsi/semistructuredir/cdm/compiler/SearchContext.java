package edu.mayo.bsi.semistructuredir.cdm.compiler;

import org.elasticsearch.index.query.QueryBuilder;

public interface SearchContext {
    QueryBuilder toQuery(String field);
}
