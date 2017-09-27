package edu.mayo.omopindexer;

import com.google.common.collect.Lists;
import edu.mayo.omopindexer.io.BioBankCNDeserializer;
import edu.mayo.omopindexer.io.JCAStoOMOPCDMSerializer;
import org.apache.ctakes.clinicalpipeline.ClinicalPipelineFactory;
import org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator;
import org.apache.ctakes.drugner.ae.DrugMentionAnnotator;
import org.apache.ctakes.temporal.ae.*;
import org.apache.ctakes.temporal.pipelines.FullTemporalExtractionPipeline;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A simple pipeline that can be elaborated upon. Runs a cTAKES pipeline on input documents, performs mapping, and then
 * indexes in ElasticSearch
 */
public class SimpleIndexPipeline implements Runnable {

    /**
     * Runs the simple pipeline, <b>make sure to update static configuration constants</b>
     **/
    public static void main(String... args) throws UIMAException, IOException {
        // Bootstrapping for the sake of the end-user (cTAKES: "what are end-user readable error messages???")
        // - Check for UMLS user and password being set
        if (System.getProperty("ctakes.umlsuser") == null || System.getProperty("ctakes.umlspw") == null) {
            System.out.println("cTAKES requires a UMLS user as well as a UMLS password to be set for dictionary lookups");
            System.out.println("Please rerun with additional arguments -Dctakes.umlsuser=user -Dctakes.umlspw=pass");
            System.exit(1);
        }
        // - Check for dictionary resource:
        File f = new File("resources/org/apache/ctakes/dictionary/lookup/");
        if (!f.exists()) {
            f = new File("resources");
            f.mkdirs();
            System.out.println("Missing cTAKES dictionary definitions: please go to ");
            System.out.println("http://ctakes.apache.org/downloads.cgi");
            System.out.println("Download the UMLS dictionary, and extract to the \"resources\" folder");
            System.exit(1);
        }
        // - Copy over lvg resources
        File resource = new File("resources");
        byte[] buf = new byte[1024];
        ZipInputStream is = new ZipInputStream(SimpleIndexPipeline.class.getResourceAsStream("/lvg.zip"));
        ZipEntry entry;
        while ((entry = is.getNextEntry()) != null) {
            File out = new File(resource, entry.getName());
            if (entry.isDirectory()) {
                out.mkdirs();
            } else {
                out.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(out);
                int len;
                while ((len = is.read(buf)) > 0) {
                    fos.write(buf, 0, len);
                }

                fos.flush();
                fos.close();
            }
        }
        is.closeEntry();
        is.close();
        // - Load lvg resources to classpath
        URLClassLoader urlCL = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class cLClazz = URLClassLoader.class;
        // -- Access classloader method via reflection
        try {
            Method method = cLClazz.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            // -- Invoke with resource folder URL
            method.invoke(urlCL, resource.toURI().toURL());
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error, could not add URL to system classloader");
        }
    }

    /**
     * As used in cTAKES-Temporal-Demo
     **/
    public static class AddEvent extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
        @Override
        public void process(JCas jCas) throws AnalysisEngineProcessException {
            for (EventMention emention : Lists.newArrayList(JCasUtil.select(
                    jCas,
                    EventMention.class))) {
                EventProperties eventProperties = new org.apache.ctakes.typesystem.type.refsem.EventProperties(jCas);

                // create the event object
                Event event = new Event(jCas);

                // add the links between event, mention and properties
                event.setProperties(eventProperties);
                emention.setEvent(event);

                // add the annotations to the indexes
                eventProperties.addToIndexes();
                event.addToIndexes();
            }
        }
    }

    @Override public void run() {
        try {
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
            // - Base Features
            builder.add(ClinicalPipelineFactory.getTokenProcessingPipeline());
            builder.add(DefaultJCasTermAnnotator.createAnnotatorDescription());
//        builder.add(ClearNLPDependencyParserAE.createAnnotatorDescription());
//        builder.add(PolarityCleartkAnalysisEngine.createAnnotatorDescription());
//        builder.add(UncertaintyCleartkAnalysisEngine.createAnnotatorDescription());
//        builder.add(HistoryCleartkAnalysisEngine.createAnnotatorDescription());
//        builder.add(ConditionalCleartkAnalysisEngine.createAnnotatorDescription());
//        builder.add(GenericCleartkAnalysisEngine.createAnnotatorDescription());
//        builder.add(SubjectCleartkAnalysisEngine.createAnnotatorDescription());
            // - Drug Extraction
            builder.add(AnalysisEngineFactory.createEngineDescription(DrugMentionAnnotator.class));
            // - Temporal extraction
            builder.add(BackwardsTimeAnnotator
                    .createAnnotatorDescription("/org/apache/ctakes/temporal/ae/timeannotator/model.jar"));
            builder.add(EventAnnotator
                    .createAnnotatorDescription("/org/apache/ctakes/temporal/ae/eventannotator/model.jar"));
            builder.add(AnalysisEngineFactory.createEngineDescription(FullTemporalExtractionPipeline.CopyPropertiesToTemporalEventAnnotator.class));
            builder.add(AnalysisEngineFactory.createEngineDescription(AddEvent.class));
            builder.add(DocTimeRelAnnotator
                    .createAnnotatorDescription("/org/apache/ctakes/temporal/ae/doctimerel/model.jar"));
            builder.add(EventTimeSelfRelationAnnotator
                    .createEngineDescription("/org/apache/ctakes/temporal/ae/eventtime/model.jar"));
            builder.add(EventEventRelationAnnotator
                    .createAnnotatorDescription("/org/apache/ctakes/temporal/ae/eventevent/model.jar"));
            // Convert to OMOP CDM and write to ElasticSearch
            builder.add(JCAStoOMOPCDMSerializer.createAnnotatorDescription());
            AnalysisEngineDescription pipeline = builder.createAggregateDescription();

            // Execute pipeline
            SimplePipeline.runPipeline(reader, pipeline);
        } catch (Exception e) {
            PrintStream temp = System.out; // TODO thread safety on output
            try {
                System.setOut(new PrintStream(new FileOutputStream(new File(UUID.randomUUID() + ".err"))));
                e.printStackTrace(); // Exit with error
                System.out.flush();
                System.out.close();
                System.setOut(temp);
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
        }
    }
}
