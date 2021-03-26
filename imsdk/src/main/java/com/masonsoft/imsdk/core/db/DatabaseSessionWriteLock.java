package com.masonsoft.imsdk.core.db;

import androidx.annotation.NonNull;

import com.idonans.core.Singleton;
import com.idonans.core.thread.TaskQueue;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据库内容写入时，每一个登录用户至多只有一个线程在写入。
 */
public class DatabaseSessionWriteLock {

    private static final Singleton<DatabaseSessionWriteLock> INSTANCE = new Singleton<DatabaseSessionWriteLock>() {
        @Override
        protected DatabaseSessionWriteLock create() {
            return new DatabaseSessionWriteLock();
        }
    };

    public static DatabaseSessionWriteLock getInstance() {
        return INSTANCE.get();
    }

    private final Map<DatabaseHelper, TaskQueue> mSessionWriteQueue = new HashMap<>();

    private DatabaseSessionWriteLock() {
    }

    @NonNull
    public Object getSessionWriteLock(@NonNull DatabaseHelper databaseHelper) {
        return databaseHelper;
    }

}
