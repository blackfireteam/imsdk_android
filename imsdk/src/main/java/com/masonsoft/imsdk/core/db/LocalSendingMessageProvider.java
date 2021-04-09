package com.masonsoft.imsdk.core.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

import com.idonans.core.Singleton;
import com.idonans.core.util.IOUtil;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMProcessValidator;
import com.masonsoft.imsdk.core.RuntimeMode;
import com.masonsoft.imsdk.core.observable.MessageObservable;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地消息发送队列表。
 *
 * @since 1.0
 */
public class LocalSendingMessageProvider {

    private static final Singleton<LocalSendingMessageProvider> INSTANCE = new Singleton<LocalSendingMessageProvider>() {
        @Override
        protected LocalSendingMessageProvider create() {
            return new LocalSendingMessageProvider();
        }
    };

    public static LocalSendingMessageProvider getInstance() {
        IMProcessValidator.validateProcess();

        return INSTANCE.get();
    }

    private static class MemoryFullCache {

        private static final MemoryFullCache DEFAULT = new MemoryFullCache();

        private static final int MEMORY_CACHE_SIZE = 300;
        @NonNull
        private final LruCache<String, LocalSendingMessage> mFullCaches = new LruCache<>(MEMORY_CACHE_SIZE);

        private String buildKey(long sessionUserId, long localSendingMessageLocalId) {
            return sessionUserId + "_" + localSendingMessageLocalId;
        }

        private String buildKeyWithTargetMessage(long sessionUserId, int conversationType, long targetUserId, long messageLocalId) {
            return sessionUserId + "_" + conversationType + "_" + targetUserId + "_" + messageLocalId;
        }

        private void addFullCache(long sessionUserId, @NonNull LocalSendingMessage localSendingMessage) {
            try {
                {
                    final String key = buildKey(sessionUserId, localSendingMessage.localId.get());
                    mFullCaches.put(key, localSendingMessage);
                }
                {
                    // 同时缓存 by targetMessage
                    final String key = buildKeyWithTargetMessage(
                            sessionUserId,
                            localSendingMessage.conversationType.get(),
                            localSendingMessage.targetUserId.get(),
                            localSendingMessage.messageLocalId.get());
                    mFullCaches.put(key, localSendingMessage);
                }
            } catch (Throwable e) {
                IMLog.e(e);
                RuntimeMode.throwIfDebug(e);
            }
        }

        /**
         * 清空所有缓存内容
         */
        private void clear() {
            mFullCaches.evictAll();
        }

        private void removeFullCache(long sessionUserId, long localSendingMessageLocalId) {
            final String key = buildKey(sessionUserId, localSendingMessageLocalId);
            final LocalSendingMessage cache = mFullCaches.get(key);
            if (cache != null) {
                removeFullCacheInternal(sessionUserId, cache);
            }
        }

        private void removeFullCacheInternal(long sessionUserId, @NonNull LocalSendingMessage localSendingMessage) {
            try {
                {
                    final String key = buildKey(sessionUserId, localSendingMessage.localId.get());
                    mFullCaches.remove(key);
                }
                {
                    // 同时删除 by targetMessage
                    final String key = buildKeyWithTargetMessage(
                            sessionUserId,
                            localSendingMessage.conversationType.get(),
                            localSendingMessage.targetUserId.get(),
                            localSendingMessage.messageLocalId.get());
                    mFullCaches.remove(key);
                }
            } catch (Throwable e) {
                IMLog.e(e);
                RuntimeMode.throwIfDebug(e);
            }
        }

        @Nullable
        private LocalSendingMessage getFullCache(long sessionUserId, long localSendingMessageLocalId) {
            final String key = buildKey(sessionUserId, localSendingMessageLocalId);
            return mFullCaches.get(key);
        }

        @Nullable
        private LocalSendingMessage getFullCacheWithTargetMessage(long sessionUserId, int conversationType, long targetUserId, long messageLocalId) {
            final String key = buildKeyWithTargetMessage(sessionUserId, conversationType, targetUserId, messageLocalId);
            return mFullCaches.get(key);
        }

    }

    private LocalSendingMessageProvider() {
    }

    /**
     * 查询一定数量的待发送的消息
     */
    @NonNull
    public List<LocalSendingMessage> getIdleMessageList(
            final long sessionUserId,
            final int limit) {
        final ColumnsSelector<LocalSendingMessage> columnsSelector = LocalSendingMessage.COLUMNS_SELECTOR_ALL;

        final List<LocalSendingMessage> items = new ArrayList<>();
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            final StringBuilder selection = new StringBuilder();
            final List<String> selectionArgs = new ArrayList<>();

            selection.append(" " + DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_SEND_STATUS + "=? ");
            selectionArgs.add(String.valueOf(IMConstants.SendStatus.IDLE));

            selection.append(" and " + DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_ABORT_ID + "=? ");
            selectionArgs.add(String.valueOf(IMConstants.AbortId.RESET));

            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_LOCAL_SENDING_MESSAGE,
                    columnsSelector.queryColumns(),
                    selection.toString(),
                    selectionArgs.toArray(new String[]{}),
                    null,
                    null,
                    DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_LAST_MODIFY_MS + " asc",
                    String.valueOf(limit)
            );

            while (cursor.moveToNext()) {
                LocalSendingMessage item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                items.add(item);
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        IMLog.v("getIdleMessageList found %s localSendingMessage with sessionUserId:%s, limit:%s",
                items.size(), sessionUserId, limit);
        return items;
    }

    /**
     * @return 没有找到返回 null
     */
    @Nullable
    public LocalSendingMessage getLocalSendingMessage(
            final long sessionUserId,
            final long localSendingMessageLocalId) {
        final ColumnsSelector<LocalSendingMessage> columnsSelector = LocalSendingMessage.COLUMNS_SELECTOR_ALL;

        final LocalSendingMessage cache = MemoryFullCache.DEFAULT.getFullCache(sessionUserId, localSendingMessageLocalId);
        if (cache != null) {
            IMLog.v("getLocalSendingMessage cache hit sessionUserId:%s, localSendingMessageLocalId:%s",
                    sessionUserId,
                    localSendingMessageLocalId);
            return cache;
        }

        IMLog.v("getLocalSendingMessage cache miss, try read from db, sessionUserId:%s, localSendingMessageLocalId:%s",
                sessionUserId,
                localSendingMessageLocalId);

        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            //noinspection StringBufferReplaceableByString
            final StringBuilder selection = new StringBuilder();
            final List<String> selectionArgs = new ArrayList<>();

            selection.append(" " + DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_ID + "=? ");
            selectionArgs.add(String.valueOf(localSendingMessageLocalId));

            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_LOCAL_SENDING_MESSAGE,
                    columnsSelector.queryColumns(),
                    selection.toString(),
                    selectionArgs.toArray(new String[]{}),
                    null,
                    null,
                    null,
                    "1"
            );

            if (cursor.moveToNext()) {
                final LocalSendingMessage result = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                IMLog.v("localSendingMessage found with sessionUserId:%s, localSendingMessageLocalId:%s",
                        sessionUserId, localSendingMessageLocalId);

                MemoryFullCache.DEFAULT.addFullCache(sessionUserId, result);
                return result;
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        // localSendingMessage not found
        IMLog.v("localSendingMessage not found with sessionUserId:%s, localSendingMessageLocalId:%s",
                sessionUserId, localSendingMessageLocalId);
        return null;
    }

    /**
     * @return 没有找到返回 null
     */
    @Nullable
    public LocalSendingMessage getLocalSendingMessageByTargetMessage(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId,
            final long messageLocalId) {
        IMConstants.ConversationType.check(conversationType);

        final ColumnsSelector<LocalSendingMessage> columnsSelector = LocalSendingMessage.COLUMNS_SELECTOR_ALL;

        final LocalSendingMessage cache = MemoryFullCache.DEFAULT.getFullCacheWithTargetMessage(
                sessionUserId, conversationType, targetUserId, messageLocalId);
        if (cache != null) {
            IMLog.v("getLocalSendingMessageByTargetMessage cache hit sessionUserId:%s, conversationType:%s, targetUserId:%s, messageLocalId:%s",
                    sessionUserId,
                    conversationType,
                    targetUserId,
                    messageLocalId);
            return cache;
        }

        IMLog.v("getLocalSendingMessageByTargetMessage cache miss, try read from db, sessionUserId:%s, conversationType:%s, targetUserId:%s, messageLocalId:%s",
                sessionUserId,
                conversationType,
                targetUserId,
                messageLocalId);

        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            final StringBuilder selection = new StringBuilder();
            final List<String> selectionArgs = new ArrayList<>();

            selection.append(" " + DatabaseHelper.ColumnsLocalSendingMessage.C_CONVERSATION_TYPE + "=? ");
            selectionArgs.add(String.valueOf(conversationType));

            selection.append(" and " + DatabaseHelper.ColumnsLocalSendingMessage.C_TARGET_USER_ID + "=? ");
            selectionArgs.add(String.valueOf(targetUserId));

            selection.append(" and " + DatabaseHelper.ColumnsLocalSendingMessage.C_MESSAGE_LOCAL_ID + "=? ");
            selectionArgs.add(String.valueOf(messageLocalId));

            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_LOCAL_SENDING_MESSAGE,
                    columnsSelector.queryColumns(),
                    selection.toString(),
                    selectionArgs.toArray(new String[]{}),
                    null,
                    null,
                    null,
                    "1"
            );

            if (cursor.moveToNext()) {
                final LocalSendingMessage result = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                IMLog.v("localSendingMessage found with sessionUserId:%s, conversationType:%s, targetUserId:%s, messageLocalId:%s",
                        sessionUserId,
                        conversationType,
                        targetUserId,
                        messageLocalId);

                MemoryFullCache.DEFAULT.addFullCache(sessionUserId, result);
                return result;
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        // localSendingMessage not found
        IMLog.v("localSendingMessage not found with sessionUserId:%s, conversationType:%s, targetUserId:%s, messageLocalId:%s",
                sessionUserId,
                conversationType,
                targetUserId,
                messageLocalId);
        return null;
    }

    /**
     * 插入一条 LocalSendingMessage。插入成功时，会自动设置 LocalSendingMessage 的 localId
     *
     * @return 插入成功返回 true, 否则返回 false.
     */
    public boolean insertLocalSendingMessage(
            final long sessionUserId,
            final LocalSendingMessage localSendingMessage) {
        if (localSendingMessage == null) {
            IMLog.e(new IllegalArgumentException("localSendingMessage is null"));
            return false;
        }

        if (!localSendingMessage.localId.isUnset()) {
            IMLog.e(
                    new IllegalArgumentException("invalid localSendingMessage localId"),
                    "localSendingMessage localId:%s, you cat not set localId by yourself for insert, or may be want updateLocalSendingMessage",
                    localSendingMessage.localId.get()
            );
            return false;
        }

        if (localSendingMessage.conversationType.isUnset()) {
            IMLog.e(
                    new IllegalArgumentException("invalid localSendingMessage conversationType"),
                    "localSendingMessage conversationType is unset"
            );
            return false;
        }
        IMConstants.ConversationType.check(localSendingMessage.conversationType.get());

        if (localSendingMessage.targetUserId.isUnset()) {
            IMLog.e(
                    new IllegalArgumentException("invalid localSendingMessage targetUserId"),
                    "localSendingMessage targetUserId is unset"
            );
            return false;
        }

        if (localSendingMessage.messageLocalId.isUnset()) {
            IMLog.e(
                    new IllegalArgumentException("invalid localSendingMessage messageLocalId"),
                    "localSendingMessage messageLocalId is unset"
            );
            return false;
        }

        // 设置 last modify
        localSendingMessage.localLastModifyMs.set(System.currentTimeMillis());

        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            long rowId = db.insert(
                    DatabaseHelper.TABLE_NAME_LOCAL_SENDING_MESSAGE,
                    null,
                    localSendingMessage.toContentValues());
            if (rowId == -1) {
                IMLog.e(
                        new IllegalAccessError("insert localSendingMessage fail"),
                        "fail to insert localSendingMessage with sessionUserId:%s, conversationType:%s, targetUserId:%s, messageLocalId:%s",
                        sessionUserId,
                        localSendingMessage.conversationType.get(),
                        localSendingMessage.targetUserId.get(),
                        localSendingMessage.messageLocalId.get()
                );
                return false;
            }

            // 自增主键
            localSendingMessage.localId.set(rowId);
            MessageObservable.DEFAULT.notifyMessageChanged(
                    sessionUserId,
                    localSendingMessage.conversationType.get(),
                    localSendingMessage.targetUserId.get(),
                    localSendingMessage.messageLocalId.get()
            );
            return true;
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        }
        return false;
    }

    /**
     * 删除所有发送状态为成功的消息
     */
    public boolean removeAllSuccessMessage(final long sessionUserId) {
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            //noinspection StringBufferReplaceableByString
            final StringBuilder where = new StringBuilder();
            final List<String> whereArgs = new ArrayList<>();

            where.append(" " + DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_SEND_STATUS + "=? ");
            whereArgs.add(String.valueOf(IMConstants.SendStatus.SUCCESS));

            int rowsAffected = db.delete(
                    DatabaseHelper.TABLE_NAME_LOCAL_SENDING_MESSAGE,
                    where.toString(),
                    whereArgs.toArray(new String[]{})
            );
            if (rowsAffected > 0) {
                IMLog.v(
                        "removeAllSuccessMessage with sessionUserId:%s affected %s rows",
                        sessionUserId,
                        rowsAffected
                );

                MemoryFullCache.DEFAULT.clear();
                MessageObservable.DEFAULT.notifyMultiMessageChanged(sessionUserId);
                return true;
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        }
        return false;
    }

    /**
     * 将所有未发送成功消息都设置为发送失败
     */
    public boolean updateMessageToFailIfNotSuccess(final long sessionUserId) {
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            final ContentValues contentValuesUpdate = new ContentValues();
            contentValuesUpdate.put(DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_SEND_STATUS, IMConstants.SendStatus.FAIL);
            contentValuesUpdate.put(DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_ABORT_ID, IMConstants.AbortId.RESET);
            contentValuesUpdate.put(DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_LAST_MODIFY_MS, System.currentTimeMillis());

            final StringBuilder where = new StringBuilder();
            final List<String> whereArgs = new ArrayList<>();

            where.append(" " + DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_SEND_STATUS + "!=? ");
            whereArgs.add(String.valueOf(IMConstants.SendStatus.SUCCESS));

            where.append(" and " + DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_SEND_STATUS + "!=? ");
            whereArgs.add(String.valueOf(IMConstants.SendStatus.FAIL));

            int rowsAffected = db.update(
                    DatabaseHelper.TABLE_NAME_LOCAL_SENDING_MESSAGE,
                    contentValuesUpdate,
                    where.toString(),
                    whereArgs.toArray(new String[]{})
            );
            if (rowsAffected > 0) {
                IMLog.v(
                        "updateMessageToFailIfNotSuccess with sessionUserId:%s affected %s rows",
                        sessionUserId,
                        rowsAffected
                );

                MemoryFullCache.DEFAULT.clear();
                MessageObservable.DEFAULT.notifyMultiMessageChanged(sessionUserId);
                return true;
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        }
        return false;
    }

    /**
     * 更新一条 LocalSendingMessage
     *
     * @return 更新成功返回 true, 否则返回 false.
     */
    public boolean updateLocalSendingMessage(
            final long sessionUserId,
            final LocalSendingMessage localSendingMessage) {
        if (localSendingMessage == null) {
            IMLog.e(new IllegalArgumentException("localSendingMessage is null"));
            return false;
        }

        if (localSendingMessage.localId.isUnset()) {
            IMLog.e(
                    new IllegalArgumentException("invalid localSendingMessage localId"),
                    "localSendingMessage localId is unset"
            );
            return false;
        }

        // 设置 last modify
        localSendingMessage.localLastModifyMs.set(System.currentTimeMillis());

        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            //noinspection StringBufferReplaceableByString
            final StringBuilder where = new StringBuilder();
            final List<String> whereArgs = new ArrayList<>();

            where.append(" " + DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_ID + "=? ");
            whereArgs.add(String.valueOf(localSendingMessage.localId.get()));

            int rowsAffected = db.update(
                    DatabaseHelper.TABLE_NAME_LOCAL_SENDING_MESSAGE,
                    localSendingMessage.toContentValues(),
                    where.toString(),
                    whereArgs.toArray(new String[]{}));
            if (rowsAffected != 1) {
                IMLog.e(
                        new IllegalAccessError("unexpected update localSendingMessage"),
                        "update localSendingMessage with sessionUserId:%s, localSendingMessage localId:%s, affected %s rows",
                        sessionUserId,
                        localSendingMessage.localId.get(),
                        rowsAffected
                );
            }

            final LocalSendingMessage cache = getLocalSendingMessage(sessionUserId, localSendingMessage.localId.get());
            MemoryFullCache.DEFAULT.removeFullCache(sessionUserId, localSendingMessage.localId.get());
            if (rowsAffected > 0 && cache != null) {
                MessageObservable.DEFAULT.notifyMessageChanged(
                        sessionUserId,
                        cache.conversationType.get(),
                        cache.targetUserId.get(),
                        cache.messageLocalId.get()
                );
            } else if (cache == null) {
                IMLog.e(
                        new IllegalAccessError("unexpected localSendingMessage not found"),
                        "getLocalSendingMessage return null with sessionUserId:%s, localSendingMessage localId:%s",
                        sessionUserId,
                        localSendingMessage.localId.get()
                );
            }
            return rowsAffected > 0;
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        }
        return false;
    }

    /**
     * 物理删除指定记录
     *
     * @return 删除成功返回 true, 否则返回 false.
     */
    public boolean removeLocalSendingMessage(
            final long sessionUserId,
            final long localId) {

        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            //noinspection StringBufferReplaceableByString
            final StringBuilder where = new StringBuilder();
            final List<String> whereArgs = new ArrayList<>();

            where.append(" " + DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_ID + "=? ");
            whereArgs.add(String.valueOf(localId));

            final LocalSendingMessage cache = getLocalSendingMessage(sessionUserId, localId);

            int rowsAffected = db.delete(
                    DatabaseHelper.TABLE_NAME_LOCAL_SENDING_MESSAGE,
                    where.toString(),
                    whereArgs.toArray(new String[]{})
            );
            if (rowsAffected != 1) {
                IMLog.e(
                        new IllegalAccessException("remove localSendingMessage fail"),
                        "unexpected. remove localSendingMessage with sessionUserId:% localSendingMessage localId:%s affected %s rows",
                        sessionUserId,
                        localId,
                        rowsAffected
                );
            } else {
                IMLog.v("remove localSendingMessage with sessionUserId:%s localSendingMessage localId:%s affected %s rows",
                        sessionUserId,
                        localId,
                        rowsAffected);
            }

            MemoryFullCache.DEFAULT.removeFullCache(sessionUserId, localId);
            if (cache != null) {
                MessageObservable.DEFAULT.notifyMessageChanged(
                        sessionUserId,
                        cache.conversationType.get(),
                        cache.targetUserId.get(),
                        cache.messageLocalId.get()
                );
            }
            return rowsAffected > 0;
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        }
        return false;
    }

}
