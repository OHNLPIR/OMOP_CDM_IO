package org.ohnlp.ir.emirs.model.queryeditor;

import java.util.HashMap;
import java.util.Map;

public enum CDMObjectFieldDefinitionTerminal implements CDMObjectFieldDefinition {
    STRING,
    NUMERIC_WHOLE,
    NUMERIC_FP;

    @Override
    public Map<String, String> toPathDataTypeMap(String currPath, char pathSeparator) {
        HashMap<String, String> ret = new HashMap<>();
        switch (this) {
            case STRING:
                ret.put(currPath, "string");
                break;
            case NUMERIC_WHOLE:
                ret.put(currPath, "int");
                break;
            case NUMERIC_FP:
                ret.put(currPath, "float");
                break;
        }
        return ret;
    }
}
