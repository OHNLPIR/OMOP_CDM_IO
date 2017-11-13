package edu.mayo.bsi.uima.server.plugin.cdm;

import edu.mayo.bsi.uima.server.StreamingMetadata;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

public class JobIDToDocumentIDMetaAE extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas cas) throws AnalysisEngineProcessException {
        DocumentID docID = new DocumentID(cas);
        docID.setDocumentID(JCasUtil.selectSingle(cas, StreamingMetadata.class).getJobID());
        docID.addToIndexes();
    }
}
