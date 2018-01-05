package org.ohnlp.ir.emirs.model;

import org.springframework.stereotype.Component;

/**
 * Represents a document of interest
 */
@Component
public class Document {
    /**
     * An internal document ID
     */
    private String docLinkId;
    /**
     * The revision code of the document
     */
    private String revision;
    /**
     * The document type
     */
    private String docType;
    /**
     * The actual text of the document itself
     */
    private String text;
    /**
     * The section name of the document
     */
    private String sectionName;

    private String sectionID;

    private String indexDocID;

    public String getIndexDocID() {
        return indexDocID;
    }

    public void setIndexDocID(String indexDocID) {
        this.indexDocID = indexDocID;
    }

    public String getDocLinkId() {
        return docLinkId;
    }

    public void setDocLinkId(String docLinkId) {
        this.docLinkId = docLinkId;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getSectionID() {
        return sectionID;
    }

    public void setSectionID(String sectionID) {
        this.sectionID = sectionID;
    }
}
