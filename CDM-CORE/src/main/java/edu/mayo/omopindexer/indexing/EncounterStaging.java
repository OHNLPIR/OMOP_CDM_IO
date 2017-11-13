package edu.mayo.omopindexer.indexing;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.mayo.omopindexer.model.GeneratedEncounter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Provides a staging and aggregation of encounter information by ID.
 */
public class EncounterStaging {
    private static LoadingCache<String, GeneratedEncounter> STAGING;

    static {
        STAGING = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build(new CacheLoader<String, GeneratedEncounter>() {
                    @Override
                    public GeneratedEncounter load(String key) throws Exception {
                        String[] parsed = key.split(":");
                        if (parsed.length != 3) {
                            throw new RuntimeException("Malformed Encounter ID: " + key);
                        }
                        return new GeneratedEncounter(parsed[0], key, Long.valueOf(parsed[1]), Long.valueOf(parsed[2]));
                    }
                });
    }


    public static GeneratedEncounter get(String encounterID) {
        try {
            return STAGING.get(encounterID);
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

}
