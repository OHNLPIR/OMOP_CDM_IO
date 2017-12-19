package org.ohnlp.ir.emirs.model.queryeditor;

import java.util.Map;

public interface CDMObjectFieldDefinition {
    Map<String, String> toPathDataTypeMap(String currPath, char pathSeparator);
}
