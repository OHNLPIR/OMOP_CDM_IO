package edu.mayo.omopindexer.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Date;

public class CDMNLPArtifact implements CDMModel {

    private String noteId;
    private String section_concept_id;
    private String snippet;
    private int offset;
    private String lexicalVariant;
    private int noteNlpConceptID;
    private int noteNlpSourceConceptID;
    private String nlpSystem;
    private Date nlpDate;
    private Date nlpDatetime;
    private char termExists;
    private String termTemporal;
    private String termModifiers;

    @Override
    @JsonIgnore
    public String getModelTypeName() {
        return "NoteNLP";
    }

    @Override
    @JsonIgnore
    public JSONObject getAsJSON() {
        // TODO should probably convert everything to use jackson throughout honestly...
        ObjectMapper om = new ObjectMapper();
        JsonNode node = om.valueToTree(this);
        try {
            String jsonText = om.writeValueAsString(node);
            return new JSONObject(new JSONTokener(jsonText));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    @JsonIgnore
    public JSONObject getJSONMapping() {
        JSONObject ret = new JSONObject();
        ret.put("noteId", constructTypeObject("string"));
        ret.put("section_concept_id", constructTypeObject("string"));
        ret.put("snippet", constructTypeObject("string"));
        ret.put("offset", constructTypeObject("int"));
        ret.put("lexicalVariant", constructTypeObject("string"));
        ret.put("noteNlpConceptID", constructTypeObject("int"));
        ret.put("noteNlpSourceConceptID", constructTypeObject("int"));
        ret.put("nlpSystem", constructTypeObject("string"));
        ret.put("nlpDate", constructTypeObject("date"));
        ret.put("nlpDatetime", constructTypeObject("date"));
        ret.put("termExists", constructTypeObject("string"));
        ret.put("termTemporal", constructTypeObject("string"));
        ret.put("termModifiers", constructTypeObject("string"));
        return ret;
    }

    private JSONObject constructTypeObject(String type) {
        JSONObject ret = new JSONObject();
        ret.put("type", type);
        return ret;
    }

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public String getSection_concept_id() {
        return section_concept_id;
    }

    public void setSection_concept_id(String section_concept_id) {
        this.section_concept_id = section_concept_id;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String getLexicalVariant() {
        return lexicalVariant;
    }

    public void setLexicalVariant(String lexicalVariant) {
        this.lexicalVariant = lexicalVariant;
    }

    public int getNoteNlpConceptID() {
        return noteNlpConceptID;
    }

    public void setNoteNlpConceptID(int noteNlpConceptID) {
        this.noteNlpConceptID = noteNlpConceptID;
    }

    public int getNoteNlpSourceConceptID() {
        return noteNlpSourceConceptID;
    }

    public void setNoteNlpSourceConceptID(int noteNlpSourceConceptID) {
        this.noteNlpSourceConceptID = noteNlpSourceConceptID;
    }

    public String getNlpSystem() {
        return nlpSystem;
    }

    public void setNlpSystem(String nlpSystem) {
        this.nlpSystem = nlpSystem;
    }

    public Date getNlpDate() {
        return nlpDate;
    }

    public void setNlpDate(Date nlpDate) {
        this.nlpDate = nlpDate;
    }

    public Date getNlpDatetime() {
        return nlpDatetime;
    }

    public void setNlpDatetime(Date nlpDatetime) {
        this.nlpDatetime = nlpDatetime;
    }

    public char getTermExists() {
        return termExists;
    }

    public void setTermExists(char termExists) {
        this.termExists = termExists;
    }

    public String getTermTemporal() {
        return termTemporal;
    }

    public void setTermTemporal(String termTemporal) {
        this.termTemporal = termTemporal;
    }

    public String getTermModifiers() {
        return termModifiers;
    }

    public void setTermModifiers(String termModifiers) {
        this.termModifiers = termModifiers;
    }
}
