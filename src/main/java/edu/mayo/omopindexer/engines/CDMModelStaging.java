package edu.mayo.omopindexer.engines;

import edu.mayo.omopindexer.model.CDMModel;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A utility class providing temporary storage for CDM models across the various stages in the UIMA pipeline
 */
public class CDMModelStaging {
    private static ConcurrentHashMap<String, List<CDMModel>> STAGING = new ConcurrentHashMap<>();

    public static void stage(JCas cas, CDMModel model) {
        String id = JCasUtil.selectSingle(cas, DocumentID.class).getDocumentID();
        List<CDMModel> models = STAGING.computeIfAbsent(id, k -> new LinkedList<>()); // This should be thread safe (the same JCAS in the same thread)
        models.add(model);
    }

    public static List<CDMModel> unstage(JCas cas) {
        String id = JCasUtil.selectSingle(cas, DocumentID.class).getDocumentID();
        List<CDMModel> ret = STAGING.get(id);
        if (ret != null) {
            STAGING.remove(id);
        }
        return ret;
    }

}
