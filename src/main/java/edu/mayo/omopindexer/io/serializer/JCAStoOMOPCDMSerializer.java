package edu.mayo.omopindexer.io.serializer;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Converts incoming JCAS structures with assumed cTAKES type system into the OMOP CDM data model,
 * then sends to ElasticSearch for indexing
 */
public class JCAStoOMOPCDMSerializer extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {

    }

    public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(JCAStoOMOPCDMSerializer.class);
    }
}
