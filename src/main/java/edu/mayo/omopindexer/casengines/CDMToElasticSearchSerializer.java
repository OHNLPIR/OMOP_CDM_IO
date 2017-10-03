package edu.mayo.omopindexer.casengines;

import edu.mayo.omopindexer.indexing.CDMModelStaging;
import edu.mayo.omopindexer.indexing.CDMToJSONSerializer;
import edu.mayo.omopindexer.indexing.ElasticSearchIndexer;
import edu.mayo.omopindexer.model.CDMModel;
import edu.mayo.omopindexer.types.BioBankCNHeader;
import edu.mayo.omopindexer.types.BioBankCNSectionHeader;
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
        BioBankCNHeader header = JCasUtil.selectSingle(jCas, BioBankCNHeader.class);
        // - Pull Metadata
        String headerText = header.getValue();
        BioBankCNSectionHeader section = JCasUtil.selectSingle(jCas, BioBankCNSectionHeader.class);
        String sectionName = section.getSectionName();
        String sectionID = section.getSectionID();
        // - Serialize
        CDMToJSONSerializer serializer = new CDMToJSONSerializer(id, text, headerText, sectionName, sectionID, CDMModelStaging.unstage(jCas).toArray(new CDMModel[0]));
        ElasticSearchIndexer.getInstance().indexSerialized(serializer);
    }

    public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(CDMToElasticSearchSerializer.class);
    }
}
