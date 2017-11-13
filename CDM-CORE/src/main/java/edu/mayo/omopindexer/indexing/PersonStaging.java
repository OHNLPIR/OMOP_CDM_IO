package edu.mayo.omopindexer.indexing;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.mayo.omopindexer.model.CDMPerson;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This class provides staging for demographic information on individual persons. Assume all access to the same patient
 * would be threadsafe
 */
public class PersonStaging {
    private static LoadingCache<String, CDMPerson> STAGING;

    static {
        STAGING = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build(new CacheLoader<String, CDMPerson>() {
                    @Override
                    public CDMPerson load(String personID) throws Exception {
                        return new CDMPerson(personID, null, null, null, null);
                    }
                });
    }


    public static CDMPerson get(String personID) {
        try {
            return STAGING.get(personID);
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }
}
