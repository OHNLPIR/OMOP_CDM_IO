package edu.mayo.omopindexer.io;

import edu.mayo.omopindexer.types.BioBankCNHeader;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PipeBitInfo(
        name = "BioBank CN Reader",
        description = "Specialized deserializer for reading BioBank clinical notes, adapted from UIMA's FilesInCollectionReader",
        role = PipeBitInfo.Role.READER,
        products = {PipeBitInfo.TypeProduct.DOCUMENT_ID}
)
public class BioBankCNDeserializer extends CollectionReader_ImplBase {

    public static final String PARAM_INPUTDIR = "InputDirectory";
    private static final Pattern HEADER_PATTERN = Pattern.compile("(?:[\\w]+:[0-9\\w-:. ]+\\|){17}CN_CDA_MCR\\|(?:[\\w]+:[0-9\\w-:. ]+\\|?){2}", Pattern.CASE_INSENSITIVE);
    private ArrayList<File> mFiles;
    private int mCurrentIndex;
    private File inputDir;

    private String queueDocID;
    private String readQueue;


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
                if (!files[i].isDirectory() && files[i].getPath().toLowerCase().endsWith(".txt")) {
                    this.mFiles.add(files[i]);
                }
            }

        } else {
            throw new ResourceInitializationException("directory_not_found", new Object[]{"InputDirectory", this.getMetaData().getName(), inputDir.getPath()});
        }
    }

    @Override
    public void getNext(CAS aCas) throws IOException, CollectionException {
        try {
            JCas cas = aCas.getJCas();
            if (readQueue == null) { // Initialize queue
                // Populate document ID
                File f = mFiles.get(mCurrentIndex);
                InputStream fileInputStream = new FileInputStream(f);
                queueDocID = createDocID(f);

                // Fill the queue text
                byte[] contents = new byte[(int)f.length()];
                fileInputStream.read(contents);
                readQueue = new String(contents);
            }

            BioBankCNHeader header = new BioBankCNHeader(cas);
            Matcher m = HEADER_PATTERN.matcher(readQueue);
            if (m.find()) { // Has a header (should always be true)
                String headerText = m.group();
                int offsetEnd = m.end();
                String text;
                if (m.find()) { // has another header after
                    int start = m.start(); // starting index
                    text = readQueue.substring(offsetEnd, start); // Content between first and second header
                    readQueue = readQueue.substring(start);
                } else {
                    text = readQueue.substring(offsetEnd); // After first header
                    readQueue = null; // Document complete, reset queue and advance
                    mCurrentIndex++;
                }
                cas.setDocumentText(text);
                Pattern patientIDPattern = Pattern.compile("PATIENT_ID:([^\\|]+)");
                String patientID = "";
                Pattern docIDPattern = Pattern.compile("DOC_ID:([^\\|]+)");
                String docID = "";
                Matcher m2 = patientIDPattern.matcher(headerText);
                if (m2.find()) {
                    patientID = m2.group(1);
                }
                m2 = docIDPattern.matcher(headerText);
                if (m2.find()) {
                    docID = m2.group(1);
                }
                DocumentID documentID = new DocumentID(cas);
                documentID.setDocumentID(patientID + "_" + docID + "_" + queueDocID);
                documentID.addToIndexes();
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
        return this.mCurrentIndex < this.mFiles.size() || readQueue != null;
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(this.mCurrentIndex, this.mFiles.size(), "entities")};

    }

    @Override
    public void close() throws IOException {

    }
}
