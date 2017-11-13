/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * TODO Licensing
 */
package org.apache.ctakes.temporal.ae.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
//import java.util.logging.Logger;

import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.perf.AnnotationCache;
import org.apache.ctakes.typesystem.type.syntax.WordToken;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

/** Modified for performance, search mod::perf */
public class NearbyVerbTenseXExtractor implements FeatureExtractor1 {

    private String name;

    //private Logger logger = Logger.getLogger(this.getClass().getName());

    public NearbyVerbTenseXExtractor() {
        super();
        this.name = "VerbTenseFeature";

    }

    @Override
    public List<Feature> extract(JCas view, Annotation annotation) throws CleartkExtractorException {
        // mod::perf get already indexed cache
        String documentId = DocumentIDAnnotationUtil.getDocumentID(view);
        AnnotationCache.AnnotationTree index = AnnotationCache.getAnnotationCache(documentId, view);
        List<Feature> features = new ArrayList<>();

        //1 get covering sentence :
        // mod::perf Remove useless indexing call that is never taken advantage of
        EventMention targetTokenAnnotation = (EventMention)annotation;
        Collection<Sentence> sentList = index.getCovering(targetTokenAnnotation.getBegin(), targetTokenAnnotation.getEnd(), Sentence.class);

        //2 get Verb Tense
        if (sentList != null && !sentList.isEmpty()){
            for(Sentence sent : sentList) {
                String verbTP ="";
                for ( WordToken wt : index.getCovered(sent.getBegin(), sent.getEnd(), WordToken.class)) {
                    if (wt != null){
                        String pos = wt.getPartOfSpeech();
                        if (pos.startsWith("VB")){
                            verbTP = verbTP + "_" + pos;
                        }
                    }
                }
                Feature feature = new Feature(this.name, verbTP);
                features.add(feature);
                //logger.info("found nearby verb's pos tag: "+ verbTP);
            }

        }
        return features;
    }

}