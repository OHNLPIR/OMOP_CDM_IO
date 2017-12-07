package edu.mayo.bsi.semistructuredir.topicmodeling;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractTopicModelingDocs{
    private static final Pattern HEADER_PATTERN = Pattern.compile("^DOC_LINK_ID.+$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern SECTION_PATTERN = Pattern.compile("^([^\\n:]+):([0-9]+):$", Pattern.MULTILINE);

    /**
     * Dirty/undocumented demonstration code, not for production TODO
     *
     * @throws IOException If file cannot be written
     */
    public static void main(String... args) throws IOException, InterruptedException {

        ExecutorService pool = Executors.newFixedThreadPool(20);
        File dataDir = new File("data");
        File outDir = new File("out");
        outDir.mkdirs();
        for (File f : dataDir.listFiles()) {
            pool.submit(() -> {
                try {
                    InputStream fileInputStream = new FileInputStream(f);
                    // Fill the queue text
                    byte[] contents = new byte[(int) f.length()];
                    fileInputStream.read(contents);
                    String doc = new String(contents);
                    Matcher headers = HEADER_PATTERN.matcher(doc);
                    Matcher nextHeaderFinder = HEADER_PATTERN.matcher(doc);
                    while (headers.find()) { // Find every header
                        String note;
                        if (nextHeaderFinder.find(headers.end())) { // Has another header
                            note = doc.substring(headers.end(), nextHeaderFinder.start());
                        } else {
                            note = doc.substring(headers.end());
                        }
                        String header = headers.group();
                        Matcher sectionMatcher = SECTION_PATTERN.matcher(note);
                        Matcher nextSectionFinder = SECTION_PATTERN.matcher(note);
                        while (sectionMatcher.find()) {
                            String sectionText;
                            String sectionID = sectionMatcher.group(2);
                            if (nextSectionFinder.find(sectionMatcher.end())) {
                                sectionText = note.substring(sectionMatcher.end(), nextSectionFinder.start());
                            } else {
                                sectionText = note.substring(sectionMatcher.end());
                            }
                            // Make file
                            Pattern mcnPattern = Pattern.compile("MCN:([^\\|]+)");
                            Pattern docLinkPattern = Pattern.compile("DOC_LINK_ID:([^\\|]+)");
                            Pattern docRevisionPattern = Pattern.compile("DOC_REVISION_ID:([^\\|]+)");
                            Pattern cn1Pattern = Pattern.compile("cn1_event_cd:([^\\|]+)");
                            Pattern timePattern = Pattern.compile("encounter_tmr:([0-9]{4})([0-9]{2})([0-9]{2})T[0-9]+", Pattern.CASE_INSENSITIVE);
                            Matcher m;
                            String mcn = "";
                            m = mcnPattern.matcher(header);
                            if (m.find()) {
                                mcn = m.group(1);
                            }
                            String docLink = "";
                            m = docLinkPattern.matcher(header);
                            if (m.find()) {
                                docLink = m.group(1);
                            }
                            String docRev = "";
                            m = docRevisionPattern.matcher(header);
                            if (m.find()) {
                                docRev = m.group(1);
                            }
                            String cn1 = "";
                            m = cn1Pattern.matcher(header);
                            if (m.find()) {
                                cn1 = m.group(1);
                            }
                            String timestamp = "";
                            m = timePattern.matcher(header);
                            if (m.find()) {
                                String year = m.group(1);
                                String month = m.group(2);
                                String day = m.group(3);
                                timestamp = month + "_" + day + "_" + year;
                            }
                            String docID = mcn + "_" + docLink + "_" + docRev + "_" + cn1 + "_" + timestamp + "_" + sectionID;
                            File outFile = new File(outDir, docID);
                            FileWriter writer = new FileWriter(outFile);
                            writer.write(sectionText);
                            writer.flush();
                            writer.close();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace(); // Not the best idea but we're not going for production level implementation here
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(10000, TimeUnit.DAYS);
    }
}
