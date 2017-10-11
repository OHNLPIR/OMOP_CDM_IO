package edu.mayo.omopindexer.vocabs;

import org.sqlite.SQLiteConfig;

import java.sql.*;
import java.util.*;


/**
 * Contains Utility Methods Pertaining to Operations with SNOMED Clinical Term Codes <br>
 * <br>
 * This class and all static accesses are Thread-Safe
 */
public class SNOMEDCTUtils {
    private static Map<String, Collection<String>> PARENTS_TO_CHILD_MAP = new HashMap<>();

    static { // Static initializers are thread-safe by default, we do not need to worry about locking
        String connURL = "jdbc:sqlite:SNOMEDCT_US/SNOMEDCT_US.sqlite";
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.setReadOnly(true);
            System.out.println("Importing SNOMED Vocabulary");
            Connection snomedConn = DriverManager.getConnection(connURL, config.toProperties());
            System.out.println("Done");
            Statement s = snomedConn.createStatement();
            ResultSet rs = s.executeQuery("SELECT sourceId, destinationId FROM Relationship WHERE typeId=116680003"); // source = child, destination = parent
            HashMap<String, Collection<String>> tempDefs = new HashMap<>();
            while (rs.next()) {
                tempDefs.computeIfAbsent(rs.getString("destinationId"), k -> new HashSet<>()).add(rs.getString("sourceId"));
            }
            for (Map.Entry<String, Collection<String>> e : tempDefs.entrySet()) {
                String parentID = e.getKey();
                PARENTS_TO_CHILD_MAP.put(parentID, generateChildrenCodes(parentID, tempDefs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Recursively generates a set containing all possible child codes of a given concept code
    private static Set<String> generateChildrenCodes(String code, Map<String, Collection<String>> defs) {
        HashSet<String> ret = new HashSet<>();
        for (String s : defs.getOrDefault(code, new HashSet<>())) {
            ret.addAll(generateChildrenCodes(s, defs));
        }
        return ret;
    }

    /**
     * Checks if a given child concept is a subclass of the given parent concept
     *
     * @param childCode  The child concept to check
     * @param parentCode The parent concept to check
     * @return True if parentCode is the same as or a parent of childCode
     */
    public static boolean isChild(String childCode, String parentCode) {
        return PARENTS_TO_CHILD_MAP.getOrDefault(parentCode, new HashSet<>()).contains(childCode) || childCode.equals(parentCode);
    }
}
