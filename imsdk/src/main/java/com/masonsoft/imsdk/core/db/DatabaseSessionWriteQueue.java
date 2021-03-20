package com.masonsoft.imsdk.core.db;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.idonans.core.Singleton;
import com.idonans.core.thread.TaskQueue;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.RuntimeMode;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据库内容写入时，每一个登录用户至多只有一个线程在写入。
 */
public class DatabaseSessionWriteQueue {

    private static final Singleton<DatabaseSessionWriteQueue> INSTANCE = new Singleton<DatabaseSessionWriteQueue>() {
        @Override
        protected DatabaseSessionWriteQueue create() {
            return new DatabaseSessionWriteQueue();
        }
    };

    private final Map<DatabaseHelper, TaskQueue> mSessionWriteQueue = new HashMap<>();

    private DatabaseSessionWriteQueue() {
    }

    @NonNull
    public TaskQueue getSessionWriteQueue(@NonNull DatabaseHelper databaseHelper) {
        TaskQueue queue = mSessionWriteQueue.get(databaseHelper);
        if (queue != null) {
            return queue;
        }

        synchronized (mSessionWriteQueue) {
            queue = mSessionWriteQueue.get(databaseHelper);
            if (queue != null) {
                return queue;
            }
            queue = new TaskQueue(1) {
                @Override
                public void setMaxCount(int maxCount) {
                    // 总是设置为 1 个线程
                    super.setMaxCount(1);
                }
            };
            mSessionWriteQueue.put(databaseHelper, queue);
            return queue;
        }
    }

    /**
     * 在指定会话上执行一组事务任务
     */
    public void executeTransaction(@NonNull DatabaseHelper databaseHelper, @NonNull OnTransactionListener listener) {
        getSessionWriteQueue(databaseHelper).enqueue(() -> {
            try {
                SQLiteDatabase db = databaseHelper.getDBHelper().getWritableDatabase();
                db.beginTransaction();
                try {
                    listener.onTransaction(databaseHelper, db);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            } catch (Throwable e) {
                IMLog.e(e);
                listener.onError(e);
                RuntimeMode.throwIfDebug(e);
            }
        });
    }

    public interface OnTransactionListener {
        void onTransaction(@NonNull DatabaseHelper databaseHelper, @NonNull SQLiteDatabase db) throws Throwable;

        void onError(@NonNull Throwable e);
    }

}
