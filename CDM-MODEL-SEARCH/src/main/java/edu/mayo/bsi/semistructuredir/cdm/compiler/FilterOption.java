package edu.mayo.bsi.semistructuredir.cdm.compiler;

public enum FilterOption {
    REQUIRED,
    FILTER,
    NEGATE,
    OPTIONAL;

    public static FilterOption fromString(String s) {
        if (s.length() < 1) return FilterOption.OPTIONAL;
        switch (s.toLowerCase().toCharArray()[0]) {
            case '+': return FilterOption.REQUIRED;
            case '-': return FilterOption.FILTER;
            case '~': return FilterOption.NEGATE;
            default: return FilterOption.OPTIONAL;
        }

    }
}
