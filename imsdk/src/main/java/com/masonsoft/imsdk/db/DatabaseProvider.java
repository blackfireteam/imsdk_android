package com.masonsoft.imsdk.db;


import com.idonans.core.Singleton;

import java.util.HashMap;
import java.util.Map;

public class DatabaseProvider {

    private static final Singleton<DatabaseProvider> INSTANCE = new Singleton<DatabaseProvider>() {
        @Override
        protected DatabaseProvider create() {
            return new DatabaseProvider();
        }
    };

    public static DatabaseProvider getInstance() {
        return INSTANCE.get();
    }

    private final Map<String, DatabaseHelper> mDBHelpers = new HashMap<>();

    private DatabaseProvider() {
    }

    public DatabaseHelper getDBHelper(long sessionUserId) {
        return getDBHelper("uid:" + sessionUserId);
    }

    public DatabaseHelper getDBHelper(String sessionNamespace) {
        synchronized (mDBHelpers) {
            String key = String.valueOf(sessionNamespace);
            DatabaseHelper dbHelper = mDBHelpers.get(key);
            if (dbHelper == null) {
                dbHelper = new DatabaseHelper(sessionNamespace);
                mDBHelpers.put(key, dbHelper);
            }
            return dbHelper;
        }
    }

}
