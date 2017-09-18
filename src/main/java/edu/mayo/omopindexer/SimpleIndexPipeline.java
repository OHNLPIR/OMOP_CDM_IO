package edu.mayo.omopindexer;

import edu.mayo.omopindexer.io.serializer.JCAStoOMOPCDMSerializer;
import org.apache.ctakes.chunker.ae.Chunker;
import org.apache.ctakes.chunker.ae.adjuster.ChunkAdjuster;
import org.apache.ctakes.clinicalpipeline.ClinicalPipelineFactory;
import org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.core.cr.FilesInDirectoryCollectionReader;
import org.apache.ctakes.core.cr.XmiCollectionReaderCtakes;
import org.apache.ctakes.dictionary.lookup.ae.DictionaryLookupAnnotator;
import org.apache.ctakes.dictionary.lookup.ae.UmlsDictionaryLookupAnnotator;
import org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator;
import org.apache.ctakes.drugner.ae.DrugMentionAnnotator;
import org.apache.ctakes.lvg.ae.LvgAnnotator;
import org.apache.ctakes.postagger.POSTagger;
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

    /** Runs the simple pipeline, <b>make sure to update static configuration constants</b> **/
    public static void main(String... args) throws UIMAException, IOException {
//         Read in XMIs via Collection Reader
        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                XmiCollectionReaderCtakes.class,
                XmiCollectionReaderCtakes.PARAM_INPUTDIR, "data"
        );//
        // Convert to OMOP CDM and write to ElasticSearch
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(JCAStoOMOPCDMSerializer.createAnnotatorDescription());
        AnalysisEngineDescription pipeline = builder.createAggregateDescription();

        // Execute pipeline
        SimplePipeline.runPipeline(reader, pipeline);
    }
}
