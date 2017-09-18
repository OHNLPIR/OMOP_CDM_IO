package edu.mayo.omopindexer;

import edu.mayo.omopindexer.io.BioBankCNDeserializer;
import edu.mayo.omopindexer.io.JCAStoOMOPCDMSerializer;
import org.apache.ctakes.clinicalpipeline.ClinicalPipelineFactory;
import org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator;
import org.apache.ctakes.drugner.ae.DrugMentionAnnotator;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;

import java.io.IOException;

/**
 * A simple pipeline that can be elaborated upon. Runs a cTAKES pipeline on input documents, performs mapping, and then
 * indexes in ElasticSearch
 */
public class SimpleIndexPipeline {

    /**
     * Runs the simple pipeline, <b>make sure to update static configuration constants</b>
     **/
    public static void main(String... args) throws UIMAException, IOException {
        // Read in BioBank Clinical Notes via Collection Reader
        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                BioBankCNDeserializer.class,
                BioBankCNDeserializer.PARAM_INPUTDIR, "data"
        );
//        // Read in XMIs via Collection Reader
//        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
//                XmiCollectionReaderCtakes.class,
//                XmiCollectionReaderCtakes.PARAM_INPUTDIR, "data"
//        );//
        AggregateBuilder builder = new AggregateBuilder();
        // Run cTAKES
        builder.add(ClinicalPipelineFactory.getTokenProcessingPipeline());
        builder.add(DefaultJCasTermAnnotator.createAnnotatorDescription());
//        builder.add(ClearNLPDependencyParserAE.createAnnotatorDescription());
//        builder.add(PolarityCleartkAnalysisEngine.createAnnotatorDescription());
//        builder.add(UncertaintyCleartkAnalysisEngine.createAnnotatorDescription());
//        builder.add(HistoryCleartkAnalysisEngine.createAnnotatorDescription());
//        builder.add(ConditionalCleartkAnalysisEngine.createAnnotatorDescription());
//        builder.add(GenericCleartkAnalysisEngine.createAnnotatorDescription());
//        builder.add(SubjectCleartkAnalysisEngine.createAnnotatorDescription());
        builder.add(AnalysisEngineFactory.createEngineDescription(DrugMentionAnnotator.class));
        // Convert to OMOP CDM and write to ElasticSearch
        builder.add(JCAStoOMOPCDMSerializer.createAnnotatorDescription());
        AnalysisEngineDescription pipeline = builder.createAggregateDescription();

        // Execute pipeline
        SimplePipeline.runPipeline(reader, pipeline);
    }
}
