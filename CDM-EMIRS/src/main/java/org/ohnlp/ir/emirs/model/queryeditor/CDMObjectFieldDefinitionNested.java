package org.ohnlp.ir.emirs.model.queryeditor;

import java.util.HashMap;
import java.util.Map;

public class CDMObjectFieldDefinitionNested implements CDMObjectFieldDefinition {
    Map<String, CDMObjectFieldDefinition> subDefs;

    @Override
    public Map<String, String> toPathDataTypeMap(String currPath, char pathSeparator) {
        Map<String, String> ret = new HashMap<>();
        for (Map.Entry<String, CDMObjectFieldDefinition> e : subDefs.entrySet()) {
            for (Map.Entry<String, String> e2 : e.getValue().toPathDataTypeMap(currPath + pathSeparator + e.getKey(), pathSeparator).entrySet()) {
                ret.put(e2.getKey(), e2.getValue());
            }
        }
        return ret;
    }
}
