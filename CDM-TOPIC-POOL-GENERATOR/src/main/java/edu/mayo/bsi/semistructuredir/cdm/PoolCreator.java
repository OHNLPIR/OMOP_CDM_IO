package edu.mayo.bsi.semistructuredir.cdm;

import joptsimple.internal.Strings;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class PoolCreator {
    public static void main(String... args) throws IOException {
        List<String> sections = Files.readAllLines(new File("sections.tsv").toPath());
        Map<String, String> sectionLookup = new HashMap();
        sections.forEach(s -> sectionLookup.put(s.split("\t")[0], s.split("\t")[1]));
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
            System.out.println("Creating pool for topic " + i);
            File bm25PoolFile = new File(poolFolder, tF.format(i) + "_BM25.pool");
            File tfidfPoolFile = new File(poolFolder, tF.format(i) + "_classic.pool");
            File mrfPoolFile = new File(poolFolder, tF.format(i) + "_mrf.pool");
            File lmDirichletPoolFile = new File(poolFolder, tF.format(i) + "_LMDirichlet.pool");
            ArrayList<String> cdmPool = new ArrayList<>(Files.readAllLines(cdmPoolFile.toPath()));
            ArrayList<String> bm25Pool = new ArrayList<>(Files.readAllLines(bm25PoolFile.toPath()));
            ArrayList<String> tfidfPool = new ArrayList<>(Files.readAllLines(tfidfPoolFile.toPath()));
            ArrayList<String> mrfPool = new ArrayList<>(Files.readAllLines(mrfPoolFile.toPath()));
            ArrayList<String> lmDirichletPool = new ArrayList<>(Files.readAllLines(lmDirichletPoolFile.toPath()));
            HashSet<String> docIDs = new HashSet<>();
            addAllDocIDs(cdmPool, docIDs);
            addAllDocIDs(bm25Pool, docIDs);
            addAllDocIDs(tfidfPool, docIDs);
            addAllDocIDs(mrfPool, docIDs);
            addAllDocIDs(lmDirichletPool, docIDs);
            Settings settings = Settings.builder() // TODO cleanup
                    .put("cluster.name", "elasticsearch").build();
            // Identify duplicates
            Map<String, Set<String>> docTextToDocIDsMap = new HashMap<>();
            try (TransportClient client = new PreBuiltTransportClient(settings).addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9310))) {
                SearchResponse resp = client.prepareSearch("BioBank").setFetchSource("RawText", null)
                        .setQuery(QueryBuilders.idsQuery("Document")
                                .addIds(docIDs.toArray(new String[docIDs.size()])))
                        .setSize(5000) // Maximum Size assuming no duplicates
                        .execute()
                        .get();
                for (SearchHit hit : resp.getHits()) {
                    String text = hit.getSource().get("RawText").toString();
                    docTextToDocIDsMap.computeIfAbsent(text, k -> new HashSet<>()).add(hit.getId());
                }
            } catch (UnknownHostException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            Map<String, String> docIDtoConcatenatedDocStringMap = new HashMap<>(); // Share a common document ID in the case of duplicates
            Map<String, String> docIDtoDocTextMap = new HashMap<>();
            for (Map.Entry<String, Set<String>> e : docTextToDocIDsMap.entrySet()) {
                String concatenatedDocID = String.join("|", e.getValue());
                for (String docID : e.getValue()) {
                    docIDtoConcatenatedDocStringMap.put(docID, concatenatedDocID);
                    docIDtoDocTextMap.put(docID, e.getKey());
                }
            }
            clean(cdmPool, docIDtoConcatenatedDocStringMap);
            clean(bm25Pool, docIDtoConcatenatedDocStringMap);
            clean(tfidfPool, docIDtoConcatenatedDocStringMap);
            clean(mrfPool, docIDtoConcatenatedDocStringMap);
            clean(lmDirichletPool, docIDtoConcatenatedDocStringMap);
            LinkedHashMap<String, AtomicInteger> out = new LinkedHashMap<>();
            addEntries(out, cdmPool);
            addEntries(out, bm25Pool);
            addEntries(out, tfidfPool);
            addEntries(out, mrfPool);
            addEntries(out, lmDirichletPool);
            List<String> result = new LinkedList<>();
            result.add("Relevance\tDocument ID\tDate\tSection\tComment\tMCN\tDoc Link ID\tRevision\tDocument Type\tMonth\tDay\tYear\tSection ID\tText");
            Map<String, List<String>> mrnToDocumentsMap = new HashMap<>();
            out.forEach((docID, value) -> {
                String[] parsed;
                String lookupDocID;
                if (docID.contains("|")) {
                    parsed = docID.split("\\|")[0].split("_");
                    lookupDocID = docID.split("\\|")[0];
                } else {
                    lookupDocID = docID;
                    parsed = docID.split("_");
                }
                if (parsed.length < 8) {
                    System.out.println(docID);
                }
                String mrn = parsed[0];
                String linkId = parsed[1];
                String rev = parsed[2];
                String event = parsed[3];
                String dtm = parsed[4] + "/" + parsed[5] + "/" + parsed[6];
                String secID = parsed[7];
                String secName = sectionLookup.get(secID);
                if (secName != null) {
                    secName = secName.substring(secName.indexOf("_") + 1);
                } else {
                    secName = "Not defined in http://mayoweb.mayo.edu/ddqb/NoteSectionEntity.html";
                }
                mrnToDocumentsMap.computeIfAbsent(mrn, k -> new LinkedList<>()).add(Strings.join(new String[]{
                        " ", docID, dtm, secName, "", mrn, linkId, rev, event, parsed[4], parsed[5], parsed[6], secID,
                        docIDtoDocTextMap.get(lookupDocID).replace("\t", "\\t").replace("\n", "\\n")
                }, "\t"));
            });
            ArrayList<List<String>> shuffledPatients = new ArrayList<>(mrnToDocumentsMap.values());
            Collections.shuffle(shuffledPatients);
            FileWriter outFile = new FileWriter(new File(outFolder, tF.format(i) + ".pool"));
            for (List<String> docs : shuffledPatients) {
                Collections.shuffle(docs);
                result.addAll(docs);
            }
            for (String s : result) {
                outFile.write(s + "\n");
            }
            outFile.flush();
            outFile.close();
        }
    }

    private static void clean(ArrayList<String> pool, Map<String, String> docIDtoConcatenatedDocStringMap) {
        // Replace all docID+score with docIDs (concatenated if necessary)
        for (int i = 0; i < pool.size(); i++) {
            String docID = pool.get(i).split("\t")[0];
            pool.set(i, docIDtoConcatenatedDocStringMap.getOrDefault(docID, docID));
        }
        // Remove duplicate documents by marking null and add frequencies to map
        Set<String> metDocIDs = new HashSet<>();
        Map<String, AtomicInteger> docFrequencies = new HashMap<>();
        for (int i = 0; i < pool.size(); i++) {
            String docID = pool.get(i);
            if (!metDocIDs.add(docID)) { // Already met this docID
                docFrequencies.get(docID).incrementAndGet();
                pool.set(i, null);
            } else {
                docFrequencies.computeIfAbsent(docID, k -> new AtomicInteger(0)).incrementAndGet();
            }
        }
        // Make copy (not at all efficient, revisit later) and re-add to original pool with frequencies
        List<String> copy = new ArrayList<>(pool);
        pool.clear();
        for (String docID : copy) {
            if (docID != null) {
                pool.add(docID + "\t" + docFrequencies.getOrDefault(docID, new AtomicInteger(0)).get());
            }
        }
    }

    private static void addAllDocIDs(ArrayList<String> pool, HashSet<String> docIDs) {
        for (String s : pool) {
            docIDs.add(s.split("\t")[0]);
        }
    }

    private static void addEntries(Map<String, AtomicInteger> out, ArrayList<String> pool) {
        // First 15
        for (int i = 0; i < pool.size() && i < 15; i++) {
            out.computeIfAbsent(pool.get(i).split("\t")[0], k -> new AtomicInteger(0))
                    .addAndGet(Integer.valueOf(pool.get(i).split("\t")[1]));
        }
        // 20% of remainder
        if (pool.size() > 15) {
            int remainder = Math.min(pool.size(), 100) - 15;
            long twentyPercent = Math.round(Math.floor(remainder * .2));
            HashSet<Integer> accessedRandoms = new HashSet<>();
            Random rand = new Random();
            for (int i = 0; i < twentyPercent; i++) {
                int next = rand.nextInt(Math.min(pool.size(), 85));
                while (accessedRandoms.contains(next)) {
                    next = rand.nextInt(Math.min(pool.size(), 85));
                }
                accessedRandoms.add(next);
                out.computeIfAbsent(pool.get(next + 15).split("\t")[0], k -> new AtomicInteger(0)).addAndGet(Integer.valueOf(pool.get(next + 15).split("\t")[1]));
            }
        }

    }
}
