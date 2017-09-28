package edu.mayo.omopindexer.performance;

import org.apache.ctakes.core.util.collection.CollectionMap;
import org.apache.ctakes.dictionary.lookup2.ae.AbstractJCasTermAnnotator;
import org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator;
import org.apache.ctakes.dictionary.lookup2.dictionary.RareWordDictionary;
import org.apache.ctakes.dictionary.lookup2.term.RareWordTerm;
import org.apache.ctakes.dictionary.lookup2.textspan.DefaultTextSpan;
import org.apache.ctakes.dictionary.lookup2.textspan.TextSpan;
import org.apache.ctakes.dictionary.lookup2.util.FastLookupToken;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Thread-safe version of {@link org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator} */
public class DefaultJCasTermAnnotatorThreadSafe extends AbstractJCasTermAnnotator {

    private static final AtomicBoolean LOCK = new AtomicBoolean(false);

    public DefaultJCasTermAnnotatorThreadSafe() {
    }

//    @Override public void initialize(UimaContext context) throws ResourceInitializationException {
//        while (LOCK.getAndSet(true)) { // Flag was already true i.e. something else currently accessing
//            synchronized (LOCK) {
//                try {
//                    LOCK.wait(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        // Acquired lock and atomically changed flag from false to true
//        super.initialize(context);
//        LOCK.set(false);
//        synchronized (LOCK) {
//            LOCK.notifyAll();
//        }
//    }


    public void findTerms(RareWordDictionary dictionary, List<FastLookupToken> allTokens, List<Integer> lookupTokenIndices, CollectionMap<TextSpan, Long, ? extends Collection<Long>> termsFromDictionary) {
        Iterator var6 = lookupTokenIndices.iterator();

        while(true) {
            Collection rareWordHits;
            Integer lookupTokenIndex;
            FastLookupToken lookupToken;
            do {
                do {
                    if (!var6.hasNext()) {
                        return;
                    }

                    lookupTokenIndex = (Integer)var6.next();
                    lookupToken = (FastLookupToken)allTokens.get(lookupTokenIndex.intValue());
                    rareWordHits = dictionary.getRareWordHits(lookupToken);
                } while(rareWordHits == null);
            } while(rareWordHits.isEmpty());

            Iterator var9 = rareWordHits.iterator();

            while(var9.hasNext()) {
                RareWordTerm rareWordHit = (RareWordTerm)var9.next();
                if (rareWordHit.getText().length() >= this._minimumLookupSpan) {
                    if (rareWordHit.getTokenCount() == 1) {
                        termsFromDictionary.placeValue(lookupToken.getTextSpan(), rareWordHit.getCuiCode());
                    } else {
                        int termStartIndex = lookupTokenIndex.intValue() - rareWordHit.getRareWordIndex();
                        if (termStartIndex >= 0 && termStartIndex + rareWordHit.getTokenCount() <= allTokens.size()) {
                            int termEndIndex = termStartIndex + rareWordHit.getTokenCount() - 1;
                            if (isTermMatch(rareWordHit, allTokens, termStartIndex, termEndIndex)) {
                                int spanStart = ((FastLookupToken)allTokens.get(termStartIndex)).getStart();
                                int spanEnd = ((FastLookupToken)allTokens.get(termEndIndex)).getEnd();
                                termsFromDictionary.placeValue(new DefaultTextSpan(spanStart, spanEnd), rareWordHit.getCuiCode());
                            }
                        }
                    }
                }
            }
        }
    }

    public static boolean isTermMatch(RareWordTerm rareWordHit, List<FastLookupToken> allTokens, int termStartIndex, int termEndIndex) {
        String[] hitTokens = rareWordHit.getTokens();
        int hit = 0;

        for(int i = termStartIndex; i < termEndIndex + 1; ++i) {
            if (!hitTokens[hit].equals(((FastLookupToken)allTokens.get(i)).getText()) && !hitTokens[hit].equals(((FastLookupToken)allTokens.get(i)).getVariant())) {
                return false;
            }

            ++hit;
        }

        return true;
    }

    public static AnalysisEngineDescription createAnnotatorDescription() throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(DefaultJCasTermAnnotatorThreadSafe.class, new Object[0]);
    }

    public static AnalysisEngineDescription createAnnotatorDescription(String descriptorPath) throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(DefaultJCasTermAnnotatorThreadSafe.class, new Object[]{"DictionaryDescriptor", descriptorPath});
    }

//    @Override
//    public void process(JCas jcas) throws AnalysisEngineProcessException {
//        while (LOCK.getAndSet(true)) { // Flag was already true i.e. something else currently accessing
//            synchronized (LOCK) {
//                try {
//                    LOCK.wait(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        // Acquired lock and atomically changed flag from false to true
//        super.process(jcas);
//        LOCK.set(false);
//        synchronized (LOCK) {
//            LOCK.notifyAll();
//        }
//    }
}
