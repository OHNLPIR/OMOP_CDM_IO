package edu.mayo.bsi.semistructuredir.cdm.compiler;


import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.InnerHitBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.join.query.HasChildQueryBuilder;

import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;

public class QueryCollator {
    Collection<QueryBuilder> required;
    Collection<QueryBuilder> optional;
    Collection<QueryBuilder> negated;
    Collection<QueryBuilder> prohibited;

    public QueryCollator() {
        this.required = new LinkedList<QueryBuilder>();
        this.optional = new LinkedList<QueryBuilder>();
        this.negated = new LinkedList<QueryBuilder>();
        this.prohibited = new LinkedList<QueryBuilder>();
    }


    public void add(Clause clause) {
        switch (clause.filter) {
            case REQUIRED:
                required.add(clause.query.toQuery(clause.field));
                break;
            case FILTER:
                prohibited.add(clause.query.toQuery(clause.field));
                break;
            case NEGATE:
                negated.add(clause.query.toQuery(clause.field));
                break;
            case OPTIONAL:
                optional.add(clause.query.toQuery(clause.field));
                break;
        }
    }

    public QueryBuilder toQuery(String childType) {
        BoolQueryBuilder base = QueryBuilders.boolQuery();
        for (QueryBuilder context : optional) {
            HasChildQueryBuilder childQuery = new HasChildQueryBuilder(childType, context, ScoreMode.Avg).innerHit(new InnerHitBuilder());
            childQuery.innerHit().setExplain(true);
            childQuery.innerHit().setName(childType + "_optional_" + UUID.randomUUID());
            base.should(childQuery);
        }
        for (QueryBuilder context : required) {
            HasChildQueryBuilder childQuery = new HasChildQueryBuilder(childType, context, ScoreMode.Avg).innerHit(new InnerHitBuilder());
            childQuery.innerHit().setExplain(true);
            childQuery.innerHit().setName(childType + "_required_" + UUID.randomUUID());
            base.must(childQuery);
        }
        for (QueryBuilder context : prohibited) {
            HasChildQueryBuilder childQuery = new HasChildQueryBuilder(childType, context, ScoreMode.Avg).innerHit(new InnerHitBuilder());
            childQuery.innerHit().setExplain(true);
            childQuery.innerHit().setName(childType + "_prohibited_" + UUID.randomUUID());
            base.mustNot(childQuery);
        }
        return base;
    }

    public QueryBuilder toQuery() {
        BoolQueryBuilder base = QueryBuilders.boolQuery();
        for (QueryBuilder context : optional) {
            base.should(context);
        }
        for (QueryBuilder context : required) {
            base.must(context);
        }
        for (QueryBuilder context : prohibited) {
            base.mustNot(context);
        }
        BoolQueryBuilder neg = QueryBuilders.boolQuery();
        for (QueryBuilder context : negated) {
            neg.should(context);
        }
        return base;
    }

    public QueryBuilder buildShouldNots(String childType) {
        if (negated.isEmpty()) return null;
        BoolQueryBuilder neg = QueryBuilders.boolQuery();
        for (QueryBuilder context : negated) {
            HasChildQueryBuilder childQuery = new HasChildQueryBuilder(childType, context, ScoreMode.Avg).innerHit(new InnerHitBuilder());
            childQuery.innerHit().setExplain(true);
            childQuery.innerHit().setName(childType + "_should_not_" + UUID.randomUUID());
            neg.should(childQuery);
        }
        return neg;
    }
}
