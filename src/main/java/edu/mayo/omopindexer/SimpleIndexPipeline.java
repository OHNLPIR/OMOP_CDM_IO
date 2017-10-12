package edu.mayo.omopindexer;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.googlecode.clearnlp.io.FileExtFilter;
import edu.mayo.omopindexer.casengines.BioBankCNDeserializer;
import edu.mayo.omopindexer.casengines.CDMToElasticSearchSerializer;
import edu.mayo.omopindexer.casengines.EthnicityAndRaceExtractor;
import edu.mayo.omopindexer.casengines.JCAStoOMOPCDMAnnotator;
import edu.mayo.omopindexer.indexing.ElasticSearchIndexer;
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
import org.hsqldb.server.ServerAcl;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A simple pipeline that can be elaborated upon. Runs a cTAKES pipeline on input documents, performs mapping, and then
 * indexes in ElasticSearch
 */
public class SimpleIndexPipeline extends Thread {

    private final int inputDir;

    private SimpleIndexPipeline(int inputDir) {
        super();
        this.inputDir = inputDir;
    }

    /**
     * Runs the simple pipeline, <b>make sure to update static configuration constants</b>
     **/
    public static void main(String... args) throws UIMAException, IOException, ServerAcl.AclFormatException, ClassNotFoundException {
        // Bootstrapping for the sake of the end-user (cTAKES: "what are end-user readable error messages???")
        System.setProperty("vocab.src.dir", System.getProperty("user.dir"));
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
            if (!f.exists() && !f.mkdirs()) {
                System.out.println("Could not create resources dir");
            }
            System.out.println("Missing cTAKES dictionary definitions: please go to ");
            System.out.println("http://ctakes.apache.org/downloads.cgi");
            System.out.println("Download the UMLS dictionary, and extract to the \"resources\" folder");
            System.exit(1);
        }
        if (System.getProperty("dictionaryPath") == null) {
            System.out.println("Path to dictionary definition not set.");
            System.out.println("Set the -DdictionaryPath=path/to/lookup/script parameter");
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
        // Execute pipeline with multiple threads and verify some parameters
        // - Split data depending on how many cores we have access to (-Dpipeline.threads or ~5% total)
        // -- Detect number of cores to use
        int numCores;
        if (System.getProperty("pipeline.threads") != null) {
            numCores = Integer.valueOf(System.getProperty("pipeline.threads"));
            System.out.println("Running pipeline with " + numCores + " threads");
        } else {
            numCores = (int) Math.round(Runtime.getRuntime().availableProcessors() * 0.5); // Should never be high enough to actually cause an overflow
            if (numCores == 0) numCores = 1;
            System.out.println("-Dpipeline.threads not set, running pipeline with " + numCores + " threads based on system configuration");
        }
        int numIndexingCores;
        if (System.getProperty("indexing.threads") != null) {
            numIndexingCores = Integer.valueOf(System.getProperty("indexing.threads"));
            System.out.println("Running indexing with " + numIndexingCores + " threads");
        } else {
            numIndexingCores = 1; // Should never be high enough to actually cause an overflow
            System.out.println("-indexing.threads not set, running indexing with " + numIndexingCores + " threads based on system configuration");
        }
        if (System.getProperty("inputDir") == null) {
            System.out.println("-DinputDir not set, defaulting to \"data\"");
        }
        String input = System.getProperty("inputDir") == null ? "data" : System.getProperty("inputDir");
        File inputDir = new File(input);
        if (!inputDir.exists() || !inputDir.isDirectory() || inputDir.listFiles() == null) {
            System.out.println("Invalid input directory " + inputDir.getAbsolutePath() + ": does not exist, is empty, or is not a directory");
            System.exit(1);
        }
        System.setProperty("workingDir", System.getProperty("user.dir")); // Because the lvg annotator changes it at some point for some reason, so we need to store here
        // -- Split data into pools
        if (System.getProperty("skipPool") == null) {
            // --- Construct pools
            File pool = new File("pool");
            if (pool.exists()) {
                System.out.println("Pool temp directory already exists, please remove prior to execution or set -DskipPool=true");
                System.exit(1);
            }
            pool.mkdirs();
            // Split data
            List<File> files = Arrays.asList(inputDir.listFiles());
            int currPartition = 0;
            for (List<File> list : Lists.partition(files, (int) Math.ceil(files.size() / (double) numCores))) {
                File copyFolder = new File(pool, "pool_" + currPartition);
                if (!copyFolder.exists()) {
                    copyFolder.mkdirs();
                }
                for (File doc : list) {
                    Files.copy(doc, new File(copyFolder, doc.getName()));
                }
                currPartition++;
            }

            if (numCores != currPartition) {
                throw new RuntimeException("Something went terribly wrong, more partitions created than threads");
            }
        }

        // - Check for an OHDSI Folder
        File vocabDefFolder = new File("OHDSI");
        if (!vocabDefFolder.exists() || vocabDefFolder.listFiles().length == 0) {
            vocabDefFolder.mkdirs();
            System.out.println("OHDSI vocabularies not present");
            System.out.println("Please download from http://athena.ohdsi.org/ snd place in the \"OHDSI\" folder");
            System.exit(1);
        }
        // - Load OHDSI Vocabularies into SQLite if Necessary
        File OHDSIVocab = new File(vocabDefFolder, "ATHENA.sqlite");
        Class.forName("org.sqlite.JDBC"); // Force load the driver class
        String connURL = "jdbc:sqlite::memory:";
        if (!OHDSIVocab.exists()) {
            try (Connection conn = DriverManager.getConnection(connURL)) {
                if (conn != null) {
                    conn.getMetaData(); // Trigger a db creation
                    // - Load CSVs (really tab delimited in default format)
                    for (File csv : vocabDefFolder.listFiles(new FileExtFilter("csv"))) {
                        BufferedReader reader = new BufferedReader(new FileReader(csv));
                        String next;
                        // - Table Definition
                        next = reader.readLine();
                        String[] parsed = next.split("\t");
                        String tableName = csv.getName().substring(0, csv.getName().length() - 4);
                        StringBuilder tableBuilder = new StringBuilder("CREATE TABLE ")
                                .append(tableName)
                                .append(" (");
                        boolean flag = false;
                        for (String s : parsed) {
                            if (flag) {
                                tableBuilder.append(",");
                            } else {
                                flag = true;
                            }
                            tableBuilder.append(s);
                            tableBuilder.append(" VARCHAR(255)");
                        }
                        tableBuilder.append(");");
                        System.out.println("Creating Table " + tableName);
                        conn.createStatement().execute(tableBuilder.toString());
                        StringBuilder insertStatementBuilder = new StringBuilder("INSERT INTO " + tableName + " VALUES (");
                        flag = false;
                        for (int i = 0; i < parsed.length; i++) {
                            if (flag) {
                                insertStatementBuilder.append(",");
                            } else {
                                flag = true;
                            }
                            insertStatementBuilder.append("?");
                        }
                        insertStatementBuilder.append(");");
                        // - Insert values into table
                        PreparedStatement ps = conn.prepareStatement(insertStatementBuilder.toString());
                        System.out.println("Loading Table " + tableName);
                        int counter = 0;
                        while ((next = reader.readLine()) != null && next.length() > 1) {
                            if (counter > 0 && counter % 500000 == 0) {
                                System.out.print("Inserting " + counter + " Elements...");
                                ps.executeBatch();
                                ps.clearBatch();
                                System.out.println("Done");
                                counter = 0;
                            }
                            String[] values = next.trim().split("\t");
                            for (int j = 0; j < values.length; j++) {
                                ps.setString(j + 1, values[j]);
                            }
                            ps.addBatch();
                            counter++;
                        }
                        System.out.print("Inserting " + counter + " Elements...");
                        ps.executeBatch();
                        System.out.println("Done");
                    }
                    // - Index for performance
                    conn.createStatement().executeUpdate("CREATE INDEX CONCEPT_INDEX ON CONCEPT (CONCEPT_CODE, CONCEPT_NAME, CONCEPT_ID)");
                    // - Write In-Memory DB To File
                    System.out.print("Saving Database to Disk...");
                    conn.createStatement().execute("backup to OHDSI/ATHENA.sqlite");
                    System.out.println("Done");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        // - Run the pipeline
        ExecutorService executor = Executors.newFixedThreadPool(numCores); // Technically should set pool size, not really necessary since we artificially bound
        for (int i = 0; i < numCores; i++) {
            Thread t = new SimpleIndexPipeline(i);
            executor.submit(t);
        }
        for (int i = 0; i < numIndexingCores; i++) {
            Executors.newSingleThreadExecutor().submit(ElasticSearchIndexer.getInstance());
        }

        try {
            executor.awaitTermination(Long.MAX_VALUE - 1, TimeUnit.DAYS); // Do not time out
            System.out.println("Completed processing of document");
            ElasticSearchIndexer.getInstance().terminate();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // Read in BioBank Clinical Notes via Collection Reader
            CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                    BioBankCNDeserializer.class,
                    BioBankCNDeserializer.PARAM_INPUTDIR, "pool/pool_" + inputDir
            );
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
            // Adapted cTAKES Components
            // - Ethnicity and Race Extraction
            builder.add(EthnicityAndRaceExtractor.buildPipeline());
            // Convert to OMOP CDM and write to ElasticSearch
            builder.add(JCAStoOMOPCDMAnnotator.createAnnotatorDescription());
            builder.add(CDMToElasticSearchSerializer.createAnnotatorDescription());
            AnalysisEngineDescription pipeline = builder.createAggregateDescription();

            // Execute pipeline
            SimplePipeline.runPipeline(reader, pipeline);
        } catch (Exception e) {
//            PrintStream temp = System.out; // TODO thread safety on output
//            try {
//                System.setOut(new PrintStream(new FileOutputStream(new File(UUID.randomUUID() + ".err"))));
//                e.printStackTrace(); // Exit with error
//                System.out.flush();
//                System.out.close();
//                System.setOut(temp);
//            } catch (FileNotFoundException e1) {
//                e1.printStackTrace();
//            }
            e.printStackTrace();
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
}
