package org.ohnlp.ir.emirs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ohnlp.ir.emirs.controllers.SearchController;
import org.ohnlp.ir.emirs.pagination.PaginatedResult;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Component
public class Pagination {
    int currPage;
    int pageSize;
    int numHits;
    Query query;
    List<DocumentHit> docs;

    public int getCurrPage() {
        return currPage;
    }

    public void setCurrPage(int currPage) {
        this.currPage = currPage;
    }

    public int getMaxPages() {
        return (int) Math.round(Math.ceil(this.numHits/(double)this.pageSize));
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getNumHits() {
        return numHits;
    }

    public void setNumHits(int numHits) {
        this.numHits = numHits;
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public void loadHits(SearchController context) {
        try {
            List<DocumentHit> hits = context.queryCache.get(this.query, () -> context.processSearch(this.query));
            this.docs = pageResults(hits);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private List<DocumentHit> pageResults(List<DocumentHit> hits) {
        ArrayList<DocumentHit> ret = new ArrayList<>(pageSize);
        for (int idx = currPage * pageSize, accumulate = 0; accumulate < pageSize && idx < hits.size(); idx++, accumulate++) {
            ret.add(hits.get(idx));
        }
        return ret;
    }

    public List<DocumentHit> getHits() {
        return this.docs;
    }
}
