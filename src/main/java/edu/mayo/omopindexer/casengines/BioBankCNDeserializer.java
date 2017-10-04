package edu.mayo.omopindexer.casengines;

import edu.mayo.omopindexer.types.BioBankCNHeader;
import edu.mayo.omopindexer.types.BioBankCNSectionHeader;
import org.apache.commons.io.FileUtils;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PipeBitInfo(
        name = "BioBank CN Reader",
        description = "Specialized deserializer for reading BioBank clinical notes, adapted from UIMA's FilesInCollectionReader. " +
                "Do not use on other data sets or outside of this project, it will not work.",
        role = PipeBitInfo.Role.READER,
        products = {PipeBitInfo.TypeProduct.DOCUMENT_ID}
)
public class BioBankCNDeserializer extends CollectionReader_ImplBase {

    public static final String PARAM_INPUTDIR = "InputDirectory";
    private static final Pattern HEADER_PATTERN = Pattern.compile("^DOC_LINK_ID.+$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private ArrayList<File> mFiles;
    private int mCurrentIndex;
    private File inputDir;
    private File completeDir;

    private String queueDocID;
    private String readQueue1; // Does not strip section headers
    int numSectionsRead;


    @Override
    public void initialize() throws ResourceInitializationException {
        inputDir = new File(((String)this.getConfigParameterValue("InputDirectory")).trim());
        this.mCurrentIndex = 0;
        if (inputDir.exists() && inputDir.isDirectory()) {
            this.mFiles = new ArrayList<>();
            File[] files = inputDir.listFiles();
            if (files == null) {
                throw new ResourceInitializationException("directory_not_found", new Object[]{"InputDirectory", this.getMetaData().getName(), inputDir.getPath()});
            }
            for(int i = 0; i < files.length; ++i) {
                if (!files[i].isDirectory() && !files[i].getName().equalsIgnoreCase(".DS_STORE")) { // Thanks Mac OS
                    this.mFiles.add(files[i]);
                }
            }
        } else {
            throw new ResourceInitializationException("directory_not_found", new Object[]{"InputDirectory", this.getMetaData().getName(), inputDir.getPath()});
        }
        completeDir = new File(inputDir.getParentFile(), "done");
        completeDir.mkdirs();
    }

    @Override
    public void getNext(CAS aCas) throws IOException, CollectionException {
        try {
            JCas cas = aCas.getJCas();
            String path = mFiles.get(mCurrentIndex).getAbsolutePath();
            if (readQueue1 == null) { // Initialize queue
                // Populate document ID
                File f = mFiles.get(mCurrentIndex);
                InputStream fileInputStream = new FileInputStream(f);
                queueDocID = createDocID(f);

                // Fill the queue text
                byte[] contents = new byte[(int)f.length()];
                fileInputStream.read(contents);
                readQueue1 = new String(contents);
                numSectionsRead = 1;
            }

            BioBankCNHeader header = new BioBankCNHeader(cas);
            Matcher m = HEADER_PATTERN.matcher(readQueue1);
            String sectionName = "";
            String sectionID = "";
            if (m.find()) { // Has a header (should always be true)
                String headerText = m.group();
                int offsetEnd = m.end();
                String text;
                int temp = numSectionsRead; // Temp value for iteration
                Pattern section = Pattern.compile("^([^\\n:]+):([0-9]+):$", Pattern.MULTILINE);
                if (m.find()) { // has another header after
                    int start = m.start(); // starting index
                    text = readQueue1.substring(offsetEnd, start); // Content between first and second header
                    Matcher sectionMatcher = section.matcher(text);
                    boolean flag = false;
                    while (temp > 0) { // Skip ahead to current section
                        if (!sectionMatcher.find()) {
                            if (temp == 1 && !flag) { // Empty section (some consistency would be nice...)
                                numSectionsRead = 1;
                                readQueue1 = readQueue1.substring(m.start());
                                getNext(cas.getCas());
                                return;
                            }
                        }
                        temp -= 1;
                        flag = true;
                    }
                    int startIndex = sectionMatcher.end() + offsetEnd;
                    int endIndex;
                    sectionName = sectionMatcher.group(1);
                    sectionID = sectionMatcher.group(2);
                    if (sectionMatcher.find()) { // Has another section heading
                        endIndex = sectionMatcher.start() + offsetEnd;
                        text = readQueue1.substring(startIndex, endIndex);
                        numSectionsRead++;
                    } else { // No more section headings under this header;
                        text = readQueue1.substring(startIndex, start); // Go from start of section to beginning of header
                        numSectionsRead = 1;
                        readQueue1 = readQueue1.substring(start);
                    }
                } else {
                    text = readQueue1.substring(offsetEnd); // After first header
                    Matcher sectionMatcher = section.matcher(text);
                    boolean flag = false;
                    while (temp > 0) { // Skip ahead to current section
                        if (!sectionMatcher.find()) {
                            if (temp == 1 && !flag) { // Empty section (some consistency would be nice...)
                                numSectionsRead = 1;
                                readQueue1 = readQueue1.substring(m.start()); // Just skip the last section
                                getNext(cas.getCas());
                                return;
                            }
                        }
                        temp -= 1;
                        flag = true;
                    }
                    int startIndex = sectionMatcher.end() + offsetEnd;
                    int endIndex;
                    sectionName = sectionMatcher.group(1);
                    sectionID = sectionMatcher.group(2);
                    if (sectionMatcher.find()) { // Has another section heading
                        endIndex = sectionMatcher.start() + offsetEnd;
                        text = readQueue1.substring(startIndex, endIndex);
                        numSectionsRead++;
                    } else { // No more section headings under this header;
                        text = readQueue1.substring(startIndex);
                        numSectionsRead = 1;
                        readQueue1 = null; // Document complete, reset queue and advance
                        File toDelete = mFiles.get(mCurrentIndex);
                        FileUtils.copyFile(toDelete, new File(completeDir, toDelete.getName()));
                        toDelete.delete();
                        mCurrentIndex++;

                    }
                }
                cas.setDocumentText(text.trim());
                Pattern mcnPattern = Pattern.compile("MCN:([^\\|]+)");
                Pattern docLinkPattern = Pattern.compile("DOC_LINK_ID:([^\\|]+)");
                Pattern docRevisionPattern = Pattern.compile("DOC_REVISION_ID:([^\\|]+)");
                Pattern cn1Pattern = Pattern.compile("cn1_event_cd:([^\\|]+)");
                Pattern timePattern = Pattern.compile("encounter_tmr:([0-9]{4})([0-9]{2})([0-9]{2})T[0-9]+", Pattern.CASE_INSENSITIVE);
                String mcn = "";
                m = mcnPattern.matcher(headerText);
                if (m.find()) {
                    mcn = m.group(1);
                }
                String docLink = "";
                m = docLinkPattern.matcher(headerText);
                if (m.find()) {
                    docLink = m.group(1);
                }
                String docRev = "";
                m = docRevisionPattern.matcher(headerText);
                if (m.find()) {
                    docRev = m.group(1);
                }
                String cn1 = "";
                m = cn1Pattern.matcher(headerText);
                if (m.find()) {
                    cn1 = m.group(1);
                }
                String timestamp = "";
                m = timePattern.matcher(headerText);
                if (m.find()) {
                    String year = m.group(1);
                    String month = m.group(2);
                    String day = m.group(3);
                    timestamp = month + "_" + day + "_" + year;
                }
                DocumentID documentID = new DocumentID(cas);
                documentID.setDocumentID(mcn + "_" + docLink + "_" + docRev + "_" + cn1 + "_" + timestamp + "_" + sectionID);
                documentID.addToIndexes();
                BioBankCNSectionHeader sectionHeader = new BioBankCNSectionHeader(cas);
                header.setFileloc(path);
                sectionHeader.setSectionID(sectionID);
                sectionHeader.setSectionName(sectionName);
                sectionHeader.addToIndexes();
                header.setValue(headerText);
                getLogger().log(Level.INFO, documentID.getDocumentID());
            }
            header.addToIndexes();

        } catch (CASException e) {
            e.printStackTrace();
        }



    }

    private String createDocID(File file) {
        String docID = file.getPath();
        if (!this.inputDir.getPath().endsWith("" + File.separator) && !this.inputDir.getPath().equals("")) {
            docID = docID.substring(this.inputDir.getPath().length() + 1);
        } else {
            docID = docID.substring(this.inputDir.getPath().length());
        }
        return docID;
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return this.mCurrentIndex < this.mFiles.size() || readQueue1 != null;
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(this.mCurrentIndex, this.mFiles.size(), "entities")};

    }

    @Override
    public void close() throws IOException {

    }
}
