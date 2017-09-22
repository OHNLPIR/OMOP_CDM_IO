/*
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
 * TODO: doublecheck license requirements before publishing
 */
package org.apache.ctakes.assertion.medfacts.cleartk.extractors;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.ctakes.perf.AnnotationCache;
import org.apache.ctakes.assertion.util.NegationManualDepContextAnalyzer;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
import org.apache.ctakes.dependency.parser.util.DependencyUtility;
import org.apache.ctakes.typesystem.type.syntax.ConllDependencyNode;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;

/**
 * Copy of Apache cTAKES version of NegationDependencyFeatureExtractor with performance fixes
 * Search: mod::perf
 */
public class NegationDependencyFeatureExtractor implements
        FeatureExtractor1<IdentifiedAnnotation> {

    NegationManualDepContextAnalyzer conAnal = null;

    public NegationDependencyFeatureExtractor(){
        conAnal = new NegationManualDepContextAnalyzer();
    }

    @Override
    public List<Feature> extract(JCas jcas, IdentifiedAnnotation focusAnnotation)
            throws CleartkExtractorException {
        List<Feature> feats = new ArrayList<>();
        Sentence sent = null;
        // Mod::perf use (what should already be an existing) annotation index instead
        AnnotationCache.AnnotationTree index = AnnotationCache.getAnnotationCache(DocumentIDAnnotationUtil.getDocumentID(jcas), jcas);
        List<Sentence> sents = new LinkedList<>(index.getCovering(focusAnnotation.getBegin(), focusAnnotation.getEnd(), Sentence.class));
        if(sents != null && sents.size() > 0){
            sent = sents.get(0);
        } else{
            return feats;
        }

        List<ConllDependencyNode> nodes = DependencyUtility.getDependencyNodes(jcas, sent);
        ConllDependencyNode headNode = DependencyUtility.getNominalHeadNode(jcas, focusAnnotation);
        try {
            boolean[] regexFeats = conAnal.findNegationContext(nodes, headNode);
            for(int j = 0; j < regexFeats.length; j++){
                if(regexFeats[j]){
                    feats.add(new Feature("DepPath_" + conAnal.getRegexName(j))); //"NEG_DEP_REGEX_"+j));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new CleartkExtractorException(e);
        }
        return feats;
    }

}