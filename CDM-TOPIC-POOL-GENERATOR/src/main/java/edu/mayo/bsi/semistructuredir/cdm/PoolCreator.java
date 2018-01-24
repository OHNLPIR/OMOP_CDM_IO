package edu.mayo.bsi.semistructuredir.cdm;

import joptsimple.internal.Strings;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PoolCreator {
    public static void main(String... args) throws IOException {
        File poolFolder = new File("pools");
        File outFolder = new File("pools_out");
        boolean mkdirs = outFolder.exists() || (!outFolder.exists() && outFolder.mkdirs());
        if (!mkdirs) {
            throw new IllegalStateException("Could not create output folder");
        }
        DecimalFormat tF = new DecimalFormat("00");
        for (int i = 0; i < 100; i++) {
            File cdmPoolFile = new File(poolFolder, tF.format(i) + ".pool");
            if (!cdmPoolFile.exists()) {
                continue;
            }
            File bm25PoolFile = new File(poolFolder, tF.format(i) + "_BM25.pool");
            File tfidfPoolFile = new File(poolFolder, tF.format(i) + "_classic.pool");
            File mrfPoolFile = new File(poolFolder, tF.format(i) + "_mrf.pool");
            File lmDirichletPoolFile = new File(poolFolder, tF.format(i) + "_LMDirichlet.pool");
            ArrayList<String> cdmPool = new ArrayList<>(Files.readAllLines(cdmPoolFile.toPath()));
            ArrayList<String> bm25Pool = new ArrayList<>(Files.readAllLines(bm25PoolFile.toPath()));
            ArrayList<String> tfidfPool = new ArrayList<>(Files.readAllLines(tfidfPoolFile.toPath()));
            ArrayList<String> mrfPool = new ArrayList<>(Files.readAllLines(mrfPoolFile.toPath()));
            ArrayList<String> lmDirichletPool = new ArrayList<>(Files.readAllLines(lmDirichletPoolFile.toPath()));
            LinkedHashMap<String, AtomicInteger> out = new LinkedHashMap<>();
            addEntries(out, cdmPool);
            addEntries(out, bm25Pool);
            addEntries(out, tfidfPool);
            addEntries(out, mrfPool);
            addEntries(out, lmDirichletPool);
            List<String> sorted = new LinkedList<>();
            sorted.add("Document ID\tMRN\tDocLinkID\tRevision\tEvent\tActivity_DTM\tSection ID\tHits");
            out.entrySet().stream().sorted(Map.Entry.comparingByValue((a1, a2) -> Integer.compare(a2.get(), a1.get())))
                    .forEach(e -> {
                        String docID = e.getKey();
                        String[] parsed = docID.split("_");
                        String mrn = parsed[0];
                        String linkId = parsed[1];
                        String rev= parsed[2];
                        String event = parsed[3];
                        String dtm = parsed[4] + "/" + parsed[5] + "/" + parsed[6];
                        String secID = parsed[7];
                        int hits = e.getValue().get();
                        sorted.add(Strings.join(new String[] {
                                docID, mrn, linkId, rev, event, dtm, secID, hits + ""
                        }, "\t"));
                    }
            );
            FileWriter outFile = new FileWriter(new File(outFolder, tF.format(i) + ".pool"));
            for (String s : sorted) {
                outFile.write(s + "\n");
            }
            outFile.flush();
            outFile.close();
        }
    }

    private static void addEntries(Map<String, AtomicInteger> out, ArrayList<String> pool) {
        // First 15
        for (int i = 0; i < pool.size() && i < 15; i++) {
            out.computeIfAbsent(pool.get(i).split("\t")[0], k -> new AtomicInteger(0)).incrementAndGet();
        }
        // 20% of remainder
        if (pool.size() > 15) {
            int remainder = Math.min(pool.size(), 100) - 15;
            long twentyPercent = Math.round(Math.floor(remainder * .2));
            HashSet<Integer> accessedRandoms = new HashSet<>();
            Random rand = new Random();
            for (int i = 0; i < twentyPercent; i++) {
                int next = rand.nextInt(Math.min(pool.size(), 100));
                while (accessedRandoms.contains(next)) {
                    next = rand.nextInt(Math.min(pool.size(), 100));
                }
                accessedRandoms.add(next);
                out.computeIfAbsent(pool.get(i + 15).split("\t")[0], k -> new AtomicInteger(0)).incrementAndGet();
            }
        }

    }
}
