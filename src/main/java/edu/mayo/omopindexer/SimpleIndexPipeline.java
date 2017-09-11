package edu.mayo.omopindexer;

import edu.mayo.omopindexer.io.serializer.JCAStoOMOPCDMSerializer;
import org.apache.ctakes.clinicalpipeline.ClinicalPipelineFactory;
import org.apache.ctakes.core.cr.FilesInDirectoryCollectionReader;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.tools.components.FileSystemCollectionReader;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * A simple pipeline that can be elaborated upon. Runs a cTAKES pipeline on input documents, performs mapping, and then
 * indexes in ElasticSearch
 */
public class SimpleIndexPipeline {

    /** Runs the simple pipeline, <b>make sure to update static configuration constants</b> **/
    public static void main(String... args) throws UIMAException, IOException {
        // Initialize UIMA Components

        // - Collection Reader
        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                FilesInDirectoryCollectionReader.class,
                "InputDirectory", "data"
        );
        // - Clinical Pipeline TODO: ask for UMLS logins
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(ClinicalPipelineFactory.getDefaultPipeline());
        // - Convert to OMOP CDM and write to ElasticSearch
        builder.add(JCAStoOMOPCDMSerializer.createAnnotatorDescription());
        AnalysisEngineDescription pipeline = builder.createAggregateDescription();

        // Execute pipeline
        SimplePipeline.runPipeline(reader, pipeline);
    }
}
