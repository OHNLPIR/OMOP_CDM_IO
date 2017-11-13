package org.apache.ctakes.perf;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Serves as an annotation cache which keeps track of annotation begin/ends in a given document so as to achieve
 * O(klogn) instead of O(n^2) collision checking
 * <p>
 * Adapted from LayeredLanguageIR project
 *
 * @author Andrew Wen
 */
public class AnnotationCache {

    // Cache as a static variable across index generations where possible
    private static Cache<String, AnnotationTree> ANN_CACHE;

    static {
        ANN_CACHE = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build();
    }

    public static AnnotationTree getAnnotationCache(String meta, JCas cas) {
        try {
            return ANN_CACHE.get(meta, () -> loadAnnotationCache(cas));
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static AnnotationTree getAnnotationCache(String meta, int docLength, Collection<? extends Annotation> items) {
        try {
            return ANN_CACHE.get(meta, () -> loadAnnotationCache(docLength, items));
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static AnnotationTree loadAnnotationCache(JCas cas) {
        FSIterator<TOP> it = cas.getJFSIndexRepository().getAllIndexedFS(Annotation.type);
        Collection<Annotation> anns = new LinkedList<>();
        while (it.hasNext()) {
            anns.add((Annotation) it.next());
        }
        return loadAnnotationCache(cas.getDocumentText().length(), anns);
    }

    private static AnnotationTree loadAnnotationCache(int docLength, Collection<? extends Annotation> items) {
        AnnotationTree currCache = new AnnotationNode(0, docLength);
        for (Annotation ann : items) {
            currCache.insert(ann);
        }
        return currCache;
    }

    public static void removeAnnotationCache(String meta) {
        ANN_CACHE.invalidate(meta);
    }

    /**
     * Clears out this given annotation cache, and reindexes using the given cas
     *
     * @param meta A meta identifier for the given cas
     * @param cas  The cas to reindex
     */

    public static void refreshCache(String meta, JCas cas) {
        AnnotationTree tree = getAnnotationCache(meta, cas);
        if (tree == null) {
            throw new NullPointerException(); // Should not ever happen
        }
        tree.clear();
        for (FSIterator<TOP> it = cas.getJFSIndexRepository().getAllIndexedFS(Annotation.type); it.hasNext(); ) {
            Annotation ann = (Annotation) it.next();
            tree.insert(ann);
        }
    }

    public static abstract class AnnotationTree {
        AtomicBoolean LCK = new AtomicBoolean(false);

        public abstract void insert(Annotation ann);

        public abstract void remove(Annotation ann); // Shouldn't be necessary but implement it just in case

        /**
         * @return a list of T constrained by the given bounds
         */
        public abstract <T extends Annotation> Collection<T> getCovering(int start, int end, Class<T> clazz);

        /**
         * @return a list of T constraining the given bounds
         */
        public abstract <T extends Annotation> Collection<T> getCovered(int start, int end, Class<T> clazz);

        public abstract <T extends Annotation> Collection<T> getCollisions(int start, int end, Class<T> clazz);

        public abstract void clear();

        /**
         * Locks the tree, guaranteeing mutex
         *
         * @return true if lock acquired, false if not
         */
        public boolean lock() {
            return !LCK.getAndSet(true);
        }
    }

    // TODO: better thread safety / should not be an issue due to only one doc per thread but is good practice
    private static class AnnotationNode extends AnnotationTree {

        private static int MIN_LEAF_SIZE = 20;

        private AnnotationTree left;
        private AnnotationTree right;
        private int split;

        public AnnotationNode(int start, int end) {
            split = (start + end) / 2;
            if ((split - start) > MIN_LEAF_SIZE) {
                left = new AnnotationNode(start, split);
                right = new AnnotationNode(split + 1, end);
            } else {
                left = new AnnotationLeaf();
                right = new AnnotationLeaf();
            }
        }

        @Override
        public void insert(Annotation ann) {
            if (ann.getBegin() <= split) {
                left.insert(ann);
            }
            if (ann.getEnd() > split) {
                right.insert(ann);
            }
        }

        @Override
        public void remove(Annotation ann) {
            if (ann.getBegin() <= split) {
                left.remove(ann);
            }
            if (ann.getEnd() > split) {
                right.remove(ann);
            }
        }

        @Override
        public <T extends Annotation> Collection<T> getCovering(int start, int end, Class<T> clazz) {
            LinkedHashSet<T> build = new LinkedHashSet<>();
            if (start <= split) {
                build.addAll(left.getCovering(start, end, clazz));
            }
            if (end > split) {
                build.addAll(right.getCovering(start, end, clazz));
            }
            return build;
        }

        @Override
        public <T extends Annotation> Collection<T> getCovered(int start, int end, Class<T> clazz) {
            LinkedHashSet<T> build = new LinkedHashSet<>();
            if (start <= split) {
                build.addAll(left.getCovered(start, end, clazz));
            }
            if (end > split) {
                build.addAll(right.getCovered(start, end, clazz));
            }
            return build;
        }

        @Override
        public <T extends Annotation> Collection<T> getCollisions(int start, int end, Class<T> clazz) {
            LinkedHashSet<T> build = new LinkedHashSet<>();
            if (start <= split) {
                build.addAll(left.getCollisions(start, end, clazz));
            }
            if (end > split) {
                build.addAll(right.getCollisions(start, end, clazz));
            }
            return build;
        }

        @Override
        public void clear() {
            this.left = null;
            this.right = null;
        }
    }


    private static class AnnotationLeaf extends AnnotationTree {

        private LinkedList<Annotation> annColl;

        public AnnotationLeaf() {
            annColl = new LinkedList<>();
        }

        @Override
        public void insert(Annotation ann) {
            annColl.add(ann);
        }

        @Override
        public void remove(Annotation ann) {
            annColl.remove(ann);
        }

        @Override
        public <T extends Annotation> Collection<T> getCovering(int start, int end, Class<T> clazz) {
            LinkedList<T> ret = new LinkedList<>();
            for (Annotation ann : annColl) {
                if (ann.getBegin() >= start && ann.getEnd() <= end) {
                    if (clazz.isInstance(ann)) {
                        ret.add((T) ann);
                    }
                }
            }
            return ret;
        }

        @Override
        public <T extends Annotation> Collection<T> getCovered(int start, int end, Class<T> clazz) {
            LinkedList<T> ret = new LinkedList<>();
            for (Annotation ann : annColl) {
                if (ann.getBegin() <= start && ann.getEnd() >= end) {
                    if (clazz.isInstance(ann)) {
                        ret.add((T) ann);
                    }
                }
            }
            return ret;
        }

        @Override
        public <T extends Annotation> Collection<T> getCollisions(int start, int end, Class<T> clazz) {
            LinkedList<T> ret = new LinkedList<>();
            for (Annotation ann : annColl) {
                if ((ann.getBegin() <= start && ann.getEnd() > start) || (ann.getBegin() >= start
                        && ann.getBegin() <= end)) {
                    if (clazz.isInstance(ann)) {
                        ret.add((T) ann);
                    }
                }
            }
            return ret;
        }

        @Override
        public void clear() {
            this.annColl = null;
        }
    }

}