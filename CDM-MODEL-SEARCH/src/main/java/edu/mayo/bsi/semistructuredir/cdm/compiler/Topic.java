package edu.mayo.bsi.semistructuredir.cdm.compiler;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.BoostingQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Topic {
    Map<String, QueryCollator> docTypeToQuery;

    public Topic() {
        docTypeToQuery = new HashMap<String, QueryCollator>();
    }

    public void add(String docType, QueryCollator query) {
        if (docTypeToQuery.containsKey(docType)) {
            QueryCollator add = docTypeToQuery.get(docType);
            add.prohibited.addAll(query.prohibited);
            add.required.addAll(query.required);
            add.optional.addAll(query.optional);
            add.negated.addAll(query.negated);
        } else {
            docTypeToQuery.put(docType, query);
        }
    }

    public void add(String docType, Clause c) {
        QueryCollator collator = docTypeToQuery.computeIfAbsent(docType, (s) -> new QueryCollator());
        collator.add(c);
    }

    public QueryBuilder toQuery() {
        BoolQueryBuilder pos = QueryBuilders.boolQuery();
        pos.must(QueryBuilders.typeQuery("Person"));
        LinkedList<QueryBuilder> shouldNotQueries = new LinkedList<>();
        for (Map.Entry<String, QueryCollator> e : docTypeToQuery.entrySet()) {
            if (e.getKey().equalsIgnoreCase("Person")) {
                pos.must(e.getValue().toQuery());
            } else {
                BoolQueryBuilder bool = (BoolQueryBuilder) e.getValue().toQuery(e.getKey());
                bool.must().forEach(pos::must);
                bool.should().forEach(pos::should);
                bool.mustNot().forEach(pos::mustNot);
                QueryBuilder negShould = e.getValue().buildShouldNots(e.getKey());
                if (negShould != null) {
                    shouldNotQueries.add(negShould);
                }
            }
        }
        if (shouldNotQueries.isEmpty()) {
            return pos;
        } else {
            BoolQueryBuilder neg = QueryBuilders.boolQuery();
            shouldNotQueries.forEach(neg::should);
            return new BoostingQueryBuilder(pos, neg).negativeBoost(0.2f);
        }
    }
}
