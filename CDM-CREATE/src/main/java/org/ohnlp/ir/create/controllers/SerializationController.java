package org.ohnlp.ir.create.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ohnlp.ir.create.Connections;
import org.ohnlp.ir.create.model.serialization.Judgement;
import org.ohnlp.ir.create.model.serialization.SerializationModel;
import org.ohnlp.ir.create.model.serialization.SerializationRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.sql.*;
import java.util.*;

@Controller
public class SerializationController {

    @RequestMapping(value = "/_save", method = RequestMethod.POST)
    public @ResponseBody
    void saveQuery(@RequestBody SerializationRequest req) {
        String user = req.getUsername();
        String queryName = req.getQueryName();
        String unstructured = req.getUnstructured();
        String structured = req.getStructured().toString();
        String cdm = req.getCdm().toString();
        List<Judgement> judgements = new LinkedList<>();
        req.getDocJudgements().forEach((key, value) -> {
            Judgement judgement = new Judgement();
            judgement.setDocument(key);
            judgement.setRelevance(value);
            judgement.setDocJudgement(true);
            judgements.add(judgement);
        });
        req.getPatientJudgements().forEach((key, value) -> {
            Judgement judgement = new Judgement();
            judgement.setDocument(key);
            judgement.setRelevance(value);
            judgement.setDocJudgement(false);
            judgements.add(judgement);
        });
        serializeSearch(user, queryName, unstructured, structured, cdm, judgements);
    }

    @RequestMapping(value = "/_savelist", method = RequestMethod.POST)
    public @ResponseBody
    Map<String, String> getSavedQueries() {
        try (Connection conn = Connections.getConnection()) {
            Map<String, String> ret = new LinkedHashMap<>();
            ResultSet rs = conn.prepareStatement("SELECT QUERY_NAME, UNSTRUCTURED_QUERY FROM QUERYS ORDER BY QUERY_NAME").executeQuery();
            while (rs.next()) {
                ret.put(rs.getString("QUERY_NAME"), rs.getString("UNSTRUCTURED_QUERY"));
            }
            return ret;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @RequestMapping(value = "/_load", method = RequestMethod.POST)
    public @ResponseBody
    SerializationModel loadQuery(@RequestBody SerializationRequest req) {
        return retrieveModel(SecurityContextHolder.getContext().getAuthentication().getName(), req.getQueryName());
    }

    @RequestMapping(value = "/_delete", method = RequestMethod.POST)
    public @ResponseBody
    Map<String, String> deleteQuery(@RequestBody String name) {
        try (Connection conn = Connections.getConnection()) {
            conn.setAutoCommit(false);
            PreparedStatement getQueryFK = conn.prepareStatement("SELECT QUERY_FK FROM QUERYS WHERE QUERY_NAME = ?");
            getQueryFK.setString(1, name.toLowerCase());
            ResultSet rs = getQueryFK.executeQuery();
            int QUERY_FK;
            if (rs.next()) {
                QUERY_FK = rs.getInt("QUERY_FK");
            } else {
                conn.setAutoCommit(true);
                return getSavedQueries();
            }
            PreparedStatement getResultsFK = conn.prepareStatement("SELECT STATE_FK, RESULTS_FK FROM SAVED_STATES WHERE QUERY_FK = ?");
            getResultsFK.setInt(1, QUERY_FK);
            List<Integer> resultsToDelete = new LinkedList<>();
            rs = getResultsFK.executeQuery();
            while (rs.next()) {
                resultsToDelete.add(rs.getInt("RESULTS_FK"));
            }

            PreparedStatement removeState = conn.prepareStatement("DELETE FROM SAVED_STATES WHERE QUERY_FK = ?");
            removeState.setInt(1, QUERY_FK);
            removeState.executeUpdate();

            PreparedStatement removeEntry = conn.prepareStatement("DELETE FROM RESULTS_ENTRY WHERE RESULTS_FK=?");
            for (int i : resultsToDelete) {
                removeEntry.setInt(1, i);
                removeEntry.executeUpdate();
            }

            PreparedStatement removeResultLink = conn.prepareStatement("DELETE FROM RESULTS_LINK WHERE RESULTS_FK=?");
            for (int i : resultsToDelete) {
                removeResultLink.setInt(1, i);
                removeResultLink.executeUpdate();
            }

            PreparedStatement removeQuery = conn.prepareStatement("DELETE FROM QUERYS WHERE QUERY_FK = ?");
            removeQuery.setInt(1, QUERY_FK);
            removeQuery.executeUpdate();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return getSavedQueries();
    }


    private SerializationModel retrieveModel(String user, String name) {
        SerializationModel ret = new SerializationModel();
        try (Connection conn = Connections.getConnection()) {
            conn.setAutoCommit(false);
            PreparedStatement getQuery = conn.prepareStatement("SELECT * FROM QUERYS WHERE QUERY_NAME = ?");
            getQuery.setString(1, name.toLowerCase());
            ResultSet rs = getQuery.executeQuery();
            if (!rs.next()) {
                conn.commit();
                conn.setAutoCommit(true);
                return null;
            }
            ret.setUnstructured(rs.getString("UNSTRUCTURED_QUERY"));
            ret.setStructured(new ObjectMapper().readTree(rs.getString("STRUCTURED_QUERY")));
            ret.setCdm(new ObjectMapper().readTree(rs.getString("CDM_QUERY")));
            int QUERY_FK = rs.getInt("QUERY_FK");
            PreparedStatement getResultFK = conn.prepareStatement("SELECT * FROM SAVED_STATES WHERE USER = ? AND QUERY_FK = ?");
            getResultFK.setString(1, user.toLowerCase());
            getResultFK.setInt(2, QUERY_FK);
            rs = getResultFK.executeQuery();
            if (!rs.next()) {
                conn.commit();
                conn.setAutoCommit(true);
                return ret;
            }
            int RESULTS_FK = rs.getInt("RESULTS_FK");
            PreparedStatement getEntries = conn.prepareStatement("SELECT * FROM RESULTS_ENTRY WHERE RESULTS_FK = ?");
            getEntries.setInt(1, RESULTS_FK);
            rs = getEntries.executeQuery();
            Collection<Judgement> judgements = new LinkedList<>();
            while (rs.next()) {
                Judgement judgement = new Judgement();
                judgement.setDocument(rs.getString("DOCID"));
                judgement.setRelevance(rs.getInt("RELEVANCE_JUDGMENT"));
                judgement.setDocJudgement(rs.getBoolean("JUDGEMENT_TYPE"));
                judgements.add(judgement);
            }
            ret.setHits(judgements);
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return null;
        }
        return ret;
    }

    private void serializeSearch(String user, String name,
                                String unstructuredQuery, String structuredQuery,
                                String cdmQuery, List<Judgement> results) {
        try (Connection conn = Connections.getConnection()) {
            conn.setAutoCommit(false);
            // Find or create query definition
            PreparedStatement selectExisting = conn.prepareStatement("SELECT * FROM QUERYS Q " +
                    "WHERE Q.QUERY_NAME = ?");
            selectExisting.setString(1, name.toLowerCase());
            ResultSet rs = selectExisting.executeQuery();
            int QUERY_FK = 0;
            boolean matched = false;
            if (rs.next()) {
                matched = true;
                QUERY_FK = rs.getInt("QUERY_FK");
            }
            if (matched) {
                PreparedStatement updatePS = conn.prepareStatement("UPDATE QUERYS SET UNSTRUCTURED_QUERY=?, STRUCTURED_QUERY=?, CDM_QUERY=? WHERE QUERY_FK=?");
                updatePS.setString(1, unstructuredQuery);
                updatePS.setString(2, structuredQuery);
                updatePS.setString(3, cdmQuery);
                updatePS.setInt(4, QUERY_FK);
                updatePS.execute();
            } else {
                PreparedStatement addNewQuery = conn.prepareStatement("INSERT INTO QUERYS " +
                        "(QUERY_NAME, UNSTRUCTURED_QUERY, STRUCTURED_QUERY, CDM_QUERY) VALUES (?, ?, ?, ?)");
                addNewQuery.setString(1, name.toLowerCase());
                addNewQuery.setString(2, unstructuredQuery);
                addNewQuery.setString(3, structuredQuery);
                addNewQuery.setString(4, cdmQuery);
                addNewQuery.execute();
                rs = addNewQuery.getGeneratedKeys();
                if (rs.next()) {
                    QUERY_FK = rs.getInt(1);
                } else {
                    throw new IllegalStateException();
                }
            }
            // Find or create state and result link key
            PreparedStatement selectLinkedResults = conn.prepareStatement("SELECT RESULTS_FK FROM SAVED_STATES WHERE \"USER\"=? AND QUERY_FK=?");
            selectLinkedResults.setString(1, user.toLowerCase());
            selectLinkedResults.setInt(2, QUERY_FK);
            rs = selectLinkedResults.executeQuery();
            int RESULTS_FK = 0;
            if (rs.next()) {
                RESULTS_FK = rs.getInt("RESULTS_FK");
            } else {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO RESULTS_LINK DEFAULT VALUES");
                ps.execute();
                rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    RESULTS_FK = rs.getInt(1);
                }
                PreparedStatement createState = conn.prepareStatement("INSERT INTO SAVED_STATES (USER, QUERY_FK, RESULTS_FK) VALUES (?, ?, ?)");
                createState.setString(1, user.toLowerCase());
                createState.setInt(2, QUERY_FK);
                createState.setInt(3, RESULTS_FK);
                createState.execute();
            }
            // Cleanup existing entries
            PreparedStatement removeEntries = conn.prepareStatement("DELETE FROM RESULTS_ENTRY WHERE RESULTS_FK = ?");
            removeEntries.setInt(1, RESULTS_FK);
            removeEntries.executeUpdate();
            for (Judgement judgement : results) {
                PreparedStatement newResult = conn.prepareStatement("INSERT INTO RESULTS_ENTRY (RESULTS_FK, DOCID, RELEVANCE_JUDGMENT, JUDGEMENT_TYPE) VALUES (?, ?, ?, ?)");
                newResult.setInt(1, RESULTS_FK);
                newResult.setString(2, judgement.getDocument());
                newResult.setBoolean(4, judgement.getDocJudgement());
                if (judgement.getRelevance() != null) {
                    newResult.setInt(3, judgement.getRelevance());
                }
                newResult.execute();
            }
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
