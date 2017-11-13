package edu.mayo.bsi.uima.server.plugin.cdm;

import edu.mayo.bsi.uima.server.StreamingMetadata;
import edu.mayo.bsi.uima.server.api.UIMANLPResultSerializer;
import edu.mayo.bsi.uima.server.api.UIMAServer;
import edu.mayo.bsi.uima.server.api.UIMAServerPlugin;
import edu.mayo.omopindexer.SimpleIndexPipeline;
import edu.mayo.omopindexer.casengines.JCAStoOMOPCDMAnnotator;
import edu.mayo.omopindexer.indexing.CDMModelStaging;
import edu.mayo.omopindexer.model.CDMModel;
import org.apache.ctakes.clinicalpipeline.ClinicalPipelineFactory;
import org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator;
import org.apache.ctakes.drugner.ae.DrugMentionAnnotator;
import org.apache.ctakes.temporal.ae.*;
import org.apache.ctakes.temporal.pipelines.FullTemporalExtractionPipeline;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;

public class CDMModelPlugin implements UIMAServerPlugin {
    @Override
    public String getName() {
        return "cdm";
    }

    @Override
    public void onEnable(UIMAServer uimaServer) {
        URLClassLoader cL = (URLClassLoader) ClassLoader.getSystemClassLoader();
        try {
            Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addURL.setAccessible(true);
            File resourcesDir = new File("resources");
            addURL.invoke(cL, resourcesDir.toURI().toURL());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            uimaServer.registerStream("cdm", AnalysisEngineFactory.createEngineDescription(JobIDToDocumentIDMetaAE.class), constructAEdesc());
        } catch (ResourceInitializationException | MalformedURLException e) {
            e.printStackTrace();
        }
        uimaServer.registerSerializer("cdm", new CDMSerializer());

    }

    private static class CDMSerializer implements UIMANLPResultSerializer {

        @Override
        public Serializable serializeNLPResult(CAS cas) {
            try {
                JCas jCas = cas.getJCas();
                HashSet<String> ret = new HashSet<>();
                for (CDMModel model : CDMModelStaging.unstage(JCasUtil.selectSingle(jCas, DocumentID.class).getDocumentID())) {
                    ret.add(model.getAsJSON().toString());
                }
                return ret;
            } catch (CASException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private AnalysisEngineDescription constructAEdesc() throws MalformedURLException, ResourceInitializationException {
        AggregateBuilder builder = new AggregateBuilder();
        // Run cTAKES
        // - Base Features
        builder.add(ClinicalPipelineFactory.getTokenProcessingPipeline());
        builder.add(DefaultJCasTermAnnotator.createAnnotatorDescription());
        // - Drug Extraction
        builder.add(AnalysisEngineFactory.createEngineDescription(DrugMentionAnnotator.class));
        // - Temporal extraction
        builder.add(BackwardsTimeAnnotator
                .createAnnotatorDescription("/org/apache/ctakes/temporal/ae/timeannotator/model.jar"));
        builder.add(EventAnnotator
                .createAnnotatorDescription("/org/apache/ctakes/temporal/ae/eventannotator/model.jar"));
        builder.add(AnalysisEngineFactory.createEngineDescription(FullTemporalExtractionPipeline.CopyPropertiesToTemporalEventAnnotator.class));
        builder.add(AnalysisEngineFactory.createEngineDescription(SimpleIndexPipeline.AddEvent.class));
        builder.add(DocTimeRelAnnotator
                .createAnnotatorDescription("/org/apache/ctakes/temporal/ae/doctimerel/model.jar"));
        builder.add(EventTimeSelfRelationAnnotator
                .createEngineDescription("/org/apache/ctakes/temporal/ae/eventtime/model.jar"));
        builder.add(EventEventRelationAnnotator
                .createAnnotatorDescription("/org/apache/ctakes/temporal/ae/eventevent/model.jar"));
        // Adapted cTAKES Components
        // - Ethnicity and Race Extraction
        // Convert to OMOP CDM and write to ElasticSearch
        builder.add(JCAStoOMOPCDMAnnotator.createAnnotatorDescription());
        return builder.createAggregateDescription();
    }

}
