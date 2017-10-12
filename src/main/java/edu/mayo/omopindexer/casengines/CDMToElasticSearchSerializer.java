package edu.mayo.omopindexer.casengines;

import edu.mayo.omopindexer.indexing.CDMModelStaging;
import edu.mayo.omopindexer.indexing.CDMToJSONSerializer;
import edu.mayo.omopindexer.indexing.ElasticSearchIndexer;
import edu.mayo.omopindexer.model.CDMModel;

import edu.mayo.omopindexer.types.ClinicalDocumentMetadata;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Processes Staged CDM Models and indexes to elasticsearch
 */
public class CDMToElasticSearchSerializer extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        // - Retrieve Metadata Information
        String text = jCas.getDocumentText();
        String id = JCasUtil.selectSingle(jCas, DocumentID.class).getDocumentID();
        ClinicalDocumentMetadata header = JCasUtil.selectSingle(jCas, ClinicalDocumentMetadata.class);
        // - Serialize
        CDMToJSONSerializer serializer = new CDMToJSONSerializer(id, text, header, CDMModelStaging.unstage(jCas).toArray(new CDMModel[0]));
        ElasticSearchIndexer.getInstance().indexSerialized(serializer);
    }

    public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(CDMToElasticSearchSerializer.class);
    }
}
