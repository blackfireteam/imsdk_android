package com.masonsoft.imsdk.core.db;

import com.masonsoft.imsdk.core.IMProcessValidator;

import java.util.HashMap;
import java.util.Map;

import io.github.idonans.core.Singleton;

/**
 * @since 1.0
 */
public class DatabaseProvider {

    private static final Singleton<DatabaseProvider> INSTANCE = new Singleton<DatabaseProvider>() {
        @Override
        protected DatabaseProvider create() {
            return new DatabaseProvider();
        }
    };

    public static DatabaseProvider getInstance() {
        IMProcessValidator.validateProcess();

        return INSTANCE.get();
    }

    private final Map<String, DatabaseHelper> mDBHelpers = new HashMap<>();

    private DatabaseProvider() {
    }

    public DatabaseHelper getDBHelper(long sessionUserId) {
        return getDBHelper("uid:" + sessionUserId);
    }

    public DatabaseHelper getDBHelper(String sessionNamespace) {
        String key = String.valueOf(sessionNamespace);

        DatabaseHelper cache = mDBHelpers.get(key);
        if (cache != null) {
            return cache;
        }

        synchronized (mDBHelpers) {
            cache = mDBHelpers.get(key);
            if (cache == null) {
                cache = new DatabaseHelper(sessionNamespace);
                mDBHelpers.put(key, cache);
            }
            return cache;
        }
    }

}
