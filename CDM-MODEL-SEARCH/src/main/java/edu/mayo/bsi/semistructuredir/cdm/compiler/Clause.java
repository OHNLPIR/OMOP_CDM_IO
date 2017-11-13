package edu.mayo.bsi.semistructuredir.cdm.compiler;

public class Clause {
    public FilterOption filter;
    public String field;
    public SearchContext query;

    public Clause(FilterOption filter, String field, SearchContext query) {
        this.filter = filter;
        this.field = field;
        this.query = query;
    }
}
