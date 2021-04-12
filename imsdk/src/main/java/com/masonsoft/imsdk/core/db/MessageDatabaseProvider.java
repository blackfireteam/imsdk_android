package com.masonsoft.imsdk.core.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMProcessValidator;
import com.masonsoft.imsdk.core.RuntimeMode;
import com.masonsoft.imsdk.core.observable.MessageObservable;

import java.util.ArrayList;
import java.util.List;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.util.IOUtil;

/**
 * 消息表(每一个会话是一个单独的消息表)
 *
 * @since 1.0
 */
public class MessageDatabaseProvider {

    private static final Singleton<MessageDatabaseProvider> INSTANCE = new Singleton<MessageDatabaseProvider>() {
        @Override
        protected MessageDatabaseProvider create() {
            return new MessageDatabaseProvider();
        }
    };

    public static MessageDatabaseProvider getInstance() {
        IMProcessValidator.validateProcess();

        return INSTANCE.get();
    }

    private static class MemoryFullCache {

        private static final MemoryFullCache DEFAULT = new MemoryFullCache();

        private static final int MEMORY_CACHE_SIZE = 500;
        @NonNull
        private final LruCache<String, Message> mFullCaches = new LruCache<>(MEMORY_CACHE_SIZE);

        private String buildKey(long sessionUserId, int conversationType, long targetUserId, long localId) {
            return sessionUserId + "_" + conversationType + "_" + targetUserId + "_" + localId;
        }

        private String buildKeyWithRemoteMessageId(long sessionUserId, int conversationType, long targetUserId, long remoteMessageId) {
            return sessionUserId + "_" + conversationType + "_" + targetUserId + "_remoteMessageId_" + remoteMessageId;
        }

        private void addFullCache(long sessionUserId, int conversationType, long targetUserId, @NonNull Message message) {
            try {
                {
                    final String key = buildKey(sessionUserId, conversationType, targetUserId, message.localId.get());
                    mFullCaches.put(key, message);
                }
                {
                    // 同时缓存 by remoteMessageId
                    final String key = buildKeyWithRemoteMessageId(sessionUserId, conversationType, targetUserId, message.remoteMessageId.get());
                    mFullCaches.put(key, message);
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

        private void removeFullCache(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
            final String key = buildKey(sessionUserId, conversationType, targetUserId, localMessageId);
            final Message cache = mFullCaches.get(key);
            if (cache != null) {
                removeFullCacheInternal(sessionUserId, conversationType, targetUserId, cache);
            }
        }

        private void removeFullCacheInternal(long sessionUserId, int conversationType, long targetUserId, @NonNull Message message) {
            try {
                {
                    final String key = buildKey(sessionUserId, conversationType, targetUserId, message.localId.get());
                    mFullCaches.remove(key);
                }
                {
                    // 同时删除 by remoteMessageId
                    final String key = buildKeyWithRemoteMessageId(sessionUserId, conversationType, targetUserId, message.remoteMessageId.get());
                    mFullCaches.remove(key);
                }
            } catch (Throwable e) {
                IMLog.e(e);
                RuntimeMode.throwIfDebug(e);
            }
        }

        @Nullable
        private Message getFullCache(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
            final String key = buildKey(sessionUserId, conversationType, targetUserId, localMessageId);
            return mFullCaches.get(key);
        }

        @Nullable
        private Message getFullCacheWithRemoteMessageId(long sessionUserId, int conversationType, long targetUserId, long remoteMessageId) {
            final String key = buildKeyWithRemoteMessageId(sessionUserId, conversationType, targetUserId, remoteMessageId);
            return mFullCaches.get(key);
        }

    }

    private MessageDatabaseProvider() {
    }

    /**
     * 获取 remote message id 的最大值
     */
    @NonNull
    public Message getMaxRemoteMessageId(final long sessionUserId,
                                         final int conversationType,
                                         final long targetUserId) {
        return getMinMaxRemoteMessageIdWithBlockId(
                sessionUserId,
                conversationType,
                targetUserId,
                0,
                true,
                false
        );
    }

    /**
     * 获取 remote message id 的最小值
     */
    @NonNull
    public Message getMinRemoteMessageId(final long sessionUserId,
                                         final int conversationType,
                                         final long targetUserId) {
        return getMinMaxRemoteMessageIdWithBlockId(
                sessionUserId,
                conversationType,
                targetUserId,
                0,
                false,
                false
        );
    }

    /**
     * 获取该 block 中 remote message id 的最大值对应的消息.
     */
    @Nullable
    public Message getMaxRemoteMessageIdWithBlockId(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId,
            final long blockId) {
        return getMinMaxRemoteMessageIdWithBlockId(
                sessionUserId,
                conversationType,
                targetUserId,
                blockId,
                true,
                true);
    }

    /**
     * 获取该 block 中 remote message id 的最小值对应的消息.
     */
    @Nullable
    public Message getMinRemoteMessageIdWithBlockId(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId,
            final long blockId) {
        return getMinMaxRemoteMessageIdWithBlockId(
                sessionUserId,
                conversationType,
                targetUserId,
                blockId,
                false,
                true);
    }

    /**
     * 获取该 block 中 remote message id 的最大值或最小值对应的消息.
     */
    @Nullable
    private Message getMinMaxRemoteMessageIdWithBlockId(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId,
            final long blockId,
            final boolean max,
            final boolean withSameBlockId) {
        IMConstants.ConversationType.check(conversationType);

        final ColumnsSelector<Message> columnsSelector = Message.COLUMNS_SELECTOR_ALL;
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            final String tableName = dbHelper.createTableMessageIfNeed(conversationType, targetUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            final StringBuilder selection = new StringBuilder();
            final List<String> selectionArgs = new ArrayList<>();

            selection.append(" " + DatabaseHelper.ColumnsMessage.C_REMOTE_MSG_ID + ">0 ");

            if (withSameBlockId) {
                selection.append(" and " + DatabaseHelper.ColumnsMessage.C_LOCAL_BLOCK_ID + "=? ");
                selectionArgs.add(String.valueOf(blockId));
            }

            cursor = db.query(
                    tableName,
                    columnsSelector.queryColumns(),
                    selection.toString(),
                    selectionArgs.toArray(new String[]{}),
                    null,
                    null,
                    DatabaseHelper.ColumnsMessage.C_REMOTE_MSG_ID + (max ? " desc" : " asc"),
                    "0,1"
            );

            if (cursor.moveToNext()) {
                final Message item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                item.applyLogicField(sessionUserId, conversationType, targetUserId);

                IMLog.v(
                        "found remoteMessageId:%s with sessionUserId:%s, conversationType:%s, targetUserId:%s," +
                                " blockId:%s, max:%s, withSameBlockId:%s",
                        item.remoteMessageId.get(),
                        sessionUserId,
                        conversationType,
                        targetUserId,
                        blockId,
                        max,
                        withSameBlockId);

                return item;
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        IMLog.v(
                "remoteMessageId not found with sessionUserId:%s, conversationType:%s, targetUserId:%s," +
                        " blockId:%s, max:%s, withSameBlockId:%s",
                sessionUserId,
                conversationType,
                targetUserId,
                blockId,
                max,
                withSameBlockId);
        return null;
    }

    /**
     * 获取紧挨着 remote message id 的更小的 remote message id
     */
    @Nullable
    public Message getClosestLessThanRemoteMessageIdWithRemoteMessageId(final long sessionUserId,
                                                                        final int conversationType,
                                                                        final long targetUserId,
                                                                        final long remoteMessageId) {
        return getClosestRemoteMessageIdWithRemoteMessageId(
                sessionUserId,
                conversationType,
                targetUserId,
                remoteMessageId,
                true
        );
    }

    /**
     * 获取紧挨着 remote message id 的更大的 remote message id
     */
    @Nullable
    public Message getClosestGreaterThanRemoteMessageIdWithRemoteMessageId(final long sessionUserId,
                                                                           final int conversationType,
                                                                           final long targetUserId,
                                                                           final long remoteMessageId) {
        return getClosestRemoteMessageIdWithRemoteMessageId(
                sessionUserId,
                conversationType,
                targetUserId,
                remoteMessageId,
                false
        );
    }

    /**
     * 获取与 remote message id 的最接近的 remote message id
     */
    @Nullable
    private Message getClosestRemoteMessageIdWithRemoteMessageId(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId,
            final long remoteMessageId,
            final boolean history) {
        IMConstants.ConversationType.check(conversationType);

        final ColumnsSelector<Message> columnsSelector = Message.COLUMNS_SELECTOR_ALL;
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            final String tableName = dbHelper.createTableMessageIfNeed(conversationType, targetUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            final StringBuilder selection = new StringBuilder();
            final List<String> selectionArgs = new ArrayList<>();

            selection.append(" " + DatabaseHelper.ColumnsMessage.C_REMOTE_MSG_ID + ">0 ");

            if (history) {
                selection.append(" and " + DatabaseHelper.ColumnsMessage.C_REMOTE_MSG_ID + "<? ");
            } else {
                selection.append(" and " + DatabaseHelper.ColumnsMessage.C_REMOTE_MSG_ID + ">? ");
            }
            selectionArgs.add(String.valueOf(remoteMessageId));

            cursor = db.query(
                    tableName,
                    columnsSelector.queryColumns(),
                    selection.toString(),
                    selectionArgs.toArray(new String[]{}),
                    null,
                    null,
                    DatabaseHelper.ColumnsMessage.C_REMOTE_MSG_ID + (history ? " desc" : " asc"),
                    "0,1"
            );

            if (cursor.moveToNext()) {
                final Message item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                item.applyLogicField(sessionUserId, conversationType, targetUserId);

                IMLog.v(
                        "found remoteMessageId:%s with sessionUserId:%s, conversationType:%s, targetUserId:%s, history:%s",
                        item.remoteMessageId.get(),
                        sessionUserId,
                        conversationType,
                        targetUserId,
                        history);

                return item;
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        IMLog.v(
                "remoteMessageId not found with sessionUserId:%s, conversationType:%s, targetUserId:%s, history:%s",
                sessionUserId,
                conversationType,
                targetUserId,
                history);
        return null;
    }

    /**
     * 获取与 seq 关联最紧密的 block id 值
     *
     * @param sessionUserId
     * @param conversationType
     * @param targetUserId
     * @param seq
     * @param queryHistory
     * @return
     * @see #getMessage(long, int, long, long)
     * @see #pageQueryMessage(long, long, int, int, long, boolean, ColumnsSelector)
     */
    public long getBlockIdWithSeq(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId,
            final long seq,
            final boolean queryHistory) {
        IMConstants.ConversationType.check(conversationType);

        final ColumnsSelector<Message> columnsSelector = Message.COLUMNS_SELECTOR_ALL;
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            final String tableName = dbHelper.createTableMessageIfNeed(conversationType, targetUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            final StringBuilder selection = new StringBuilder();
            final List<String> selectionArgs = new ArrayList<>();

            if (queryHistory) {
                selection.append(" " + DatabaseHelper.ColumnsMessage.C_LOCAL_SEQ + "<=? ");
            } else {
                selection.append(" " + DatabaseHelper.ColumnsMessage.C_LOCAL_SEQ + ">=? ");
            }
            selectionArgs.add(String.valueOf(seq));

            // 查询有效的 block id
            selection.append(" and " + DatabaseHelper.ColumnsMessage.C_LOCAL_BLOCK_ID + ">0 ");

            cursor = db.query(
                    tableName,
                    columnsSelector.queryColumns(),
                    selection.toString(),
                    selectionArgs.toArray(new String[]{}),
                    null,
                    null,
                    DatabaseHelper.ColumnsMessage.C_LOCAL_SEQ + (queryHistory ? " desc" : " asc"),
                    "0,1"
            );

            if (cursor.moveToNext()) {
                final Message item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                item.applyLogicField(sessionUserId, conversationType, targetUserId);

                IMLog.v(
                        "found blockId:%s with sessionUserId:%s, conversationType:%s, targetUserId:%s, seq:%s, queryHistory:%s",
                        item.localBlockId.get(),
                        sessionUserId,
                        conversationType,
                        targetUserId,
                        seq,
                        queryHistory);

                return item.localBlockId.get();
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        IMLog.v(
                "blockId not found with sessionUserId:%s, conversationType:%s, targetUserId:%s, seq:%s, queryHistory:%s",
                sessionUserId,
                conversationType,
                targetUserId,
                seq,
                queryHistory);
        return 0L;
    }

    /**
     * 查询结果按照 seq 排列
     *
     * @param seq          不包括这一条, 初始传 0
     * @param queryHistory 是否是查询历史记录。当查询历史记录（true）时，查询结果总是比 seq 小的(查询结果按照 seq
     *                     从大到小排列)，否则表示查询比 seq 更新的消息，查询结果总是比 seq 大的(查询结果总是按照
     *                     seq 从小到大排列).
     */
    @NonNull
    public TinyPage<Message> pageQueryMessage(
            final long sessionUserId,
            final long seq,
            final int limit,
            final int conversationType,
            final long targetUserId,
            final boolean queryHistory,
            @Nullable ColumnsSelector<Message> columnsSelector) {
        IMConstants.ConversationType.check(conversationType);

        if (columnsSelector == null) {
            columnsSelector = Message.COLUMNS_SELECTOR_ALL;
        }

        final List<Message> items = new ArrayList<>();
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            final String tableName = dbHelper.createTableMessageIfNeed(conversationType, targetUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            final StringBuilder selection = new StringBuilder();
            final List<String> selectionArgs = new ArrayList<>();

            selection.append(" " + DatabaseHelper.ColumnsMessage.C_LOCAL_ID + ">0 ");

            if (seq > 0) {
                if (queryHistory) {
                    selection.append(" and " + DatabaseHelper.ColumnsConversation.C_LOCAL_SEQ + "<? ");
                } else {
                    selection.append(" and " + DatabaseHelper.ColumnsConversation.C_LOCAL_SEQ + ">? ");
                }
                selectionArgs.add(String.valueOf(seq));
            }

            cursor = db.query(
                    tableName,
                    columnsSelector.queryColumns(),
                    selection.toString(),
                    selectionArgs.toArray(new String[]{}),
                    null,
                    null,
                    DatabaseHelper.ColumnsMessage.C_LOCAL_SEQ + (queryHistory ? " desc" : " asc"),
                    String.valueOf(limit + 1) // 此处多查询一条用来计算 hasMore
            );

            while (cursor.moveToNext()) {
                final Message item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                item.applyLogicField(sessionUserId, conversationType, targetUserId);
                items.add(item);
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        final TinyPage<Message> result = new TinyPage<>();
        result.hasMore = items.size() > limit;
        if (result.hasMore) {
            result.items = new ArrayList<>(items.subList(0, limit));
        } else {
            result.items = items;
        }

        IMLog.v(
                "found %s messages[hasMore:%s] with sessionUserId:%s, conversationType:%s, targetUserId:%s, seq:%s, limit:%s",
                result.items.size(), result.hasMore, sessionUserId, conversationType, targetUserId, seq, limit
        );
        return result;
    }

    @Nullable
    public Message getMessage(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId,
            final long localMessageId) {
        IMConstants.ConversationType.check(conversationType);

        final ColumnsSelector<Message> columnsSelector = Message.COLUMNS_SELECTOR_ALL;

        final Message cache = MemoryFullCache.DEFAULT.getFullCache(sessionUserId, conversationType, targetUserId, localMessageId);
        if (cache != null) {
            IMLog.v("getMessage cache hit sessionUserId:%s, conversationType:%s, targetUserId:%s, localMessageId:%s",
                    sessionUserId,
                    conversationType,
                    targetUserId,
                    localMessageId);
            return cache;
        }

        IMLog.v("getMessage cache miss, try read from db, sessionUserId:%s, conversationType:%s, targetUserId:%s, localMessageId:%s",
                sessionUserId,
                conversationType,
                targetUserId,
                localMessageId);

        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            final String tableName = dbHelper.createTableMessageIfNeed(conversationType, targetUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            //noinspection StringBufferReplaceableByString
            final StringBuilder selection = new StringBuilder();
            final List<String> selectionArgs = new ArrayList<>();

            selection.append(" " + DatabaseHelper.ColumnsMessage.C_LOCAL_ID + "=? ");
            selectionArgs.add(String.valueOf(localMessageId));

            cursor = db.query(
                    tableName,
                    columnsSelector.queryColumns(),
                    selection.toString(),
                    selectionArgs.toArray(new String[]{}),
                    null,
                    null,
                    null,
                    "0,1"
            );

            if (cursor.moveToNext()) {
                final Message item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                item.applyLogicField(sessionUserId, conversationType, targetUserId);

                IMLog.v(
                        "found message with sessionUserId:%s, conversationType:%s, targetUserId:%s, localMessageId:%s",
                        sessionUserId,
                        conversationType,
                        targetUserId,
                        localMessageId);

                MemoryFullCache.DEFAULT.addFullCache(sessionUserId, conversationType, targetUserId, item);
                return item;
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        IMLog.v(
                "message not found with sessionUserId:%s, conversationType:%s, targetUserId:%s, localMessageId:%s",
                sessionUserId,
                conversationType,
                targetUserId,
                localMessageId);
        return null;
    }

    @Nullable
    public Message getMessageWithRemoteMessageId(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId,
            final long remoteMessageId) {
        IMConstants.ConversationType.check(conversationType);

        final ColumnsSelector<Message> columnsSelector = Message.COLUMNS_SELECTOR_ALL;

        final Message cache = MemoryFullCache.DEFAULT.getFullCacheWithRemoteMessageId(sessionUserId, conversationType, targetUserId, remoteMessageId);
        if (cache != null) {
            IMLog.v("getMessageWithRemoteMessageId cache hit sessionUserId:%s, conversationType:%s, targetUserId:%s, remoteMessageId:%s",
                    sessionUserId,
                    conversationType,
                    targetUserId,
                    remoteMessageId);
            return cache;
        }

        IMLog.v("getMessageWithRemoteMessageId cache miss, try read from db, sessionUserId:%s, conversationType:%s, targetUserId:%s, remoteMessageId:%s",
                sessionUserId,
                conversationType,
                targetUserId,
                remoteMessageId);

        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            final String tableName = dbHelper.createTableMessageIfNeed(conversationType, targetUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            //noinspection StringBufferReplaceableByString
            final StringBuilder selection = new StringBuilder();
            final List<String> selectionArgs = new ArrayList<>();

            selection.append(" " + DatabaseHelper.ColumnsMessage.C_REMOTE_MSG_ID + "=? ");
            selectionArgs.add(String.valueOf(remoteMessageId));

            cursor = db.query(
                    tableName,
                    columnsSelector.queryColumns(),
                    selection.toString(),
                    selectionArgs.toArray(new String[]{}),
                    null,
                    null,
                    null,
                    "0,1"
            );

            if (cursor.moveToNext()) {
                final Message item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                item.applyLogicField(sessionUserId, conversationType, targetUserId);

                IMLog.v(
                        "found message with sessionUserId:%s, conversationType:%s, targetUserId:%s, remoteMessageId:%s",
                        sessionUserId,
                        conversationType,
                        targetUserId,
                        remoteMessageId);

                MemoryFullCache.DEFAULT.addFullCache(sessionUserId, conversationType, targetUserId, item);
                return item;
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        IMLog.v(
                "message not found with sessionUserId:%s, conversationType:%s, targetUserId:%s, remoteMessageId:%s",
                sessionUserId,
                conversationType,
                targetUserId,
                remoteMessageId);
        return null;
    }

    /**
     * 插入一条新消息数据。新消息插入成功时，会自动设置消息的 localId
     *
     * @param sessionUserId
     * @param message
     * @return 插入成功返回 true, 否则返回 false.
     * @see Message#localId
     */
    public boolean insertMessage(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId,
            final Message message) {
        IMConstants.ConversationType.check(conversationType);

        if (message == null) {
            Throwable e = new IllegalArgumentException("message is null");
            IMLog.e(e);
            return false;
        }

        if (!message.localId.isUnset()) {
            IMLog.e(
                    new IllegalArgumentException("invalid message localId"),
                    "message localId:%s, you may need use update instead of insert",
                    message.localId.get()
            );
            return false;
        }

        // 设置 last modify
        message.localLastModifyMs.set(System.currentTimeMillis());

        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            final String tableName = dbHelper.createTableMessageIfNeed(conversationType, targetUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            long rowId = db.insert(
                    tableName,
                    null,
                    message.toContentValues());
            if (rowId == -1) {
                IMLog.e(
                        new IllegalAccessError("insert message fail"),
                        "fail to insert message with sessionUserId:%s, conversationType:%s, targetUserId:%s",
                        sessionUserId, conversationType, targetUserId
                );
                return false;
            }

            // 自增主键
            message.localId.set(rowId);
            MessageObservable.DEFAULT.notifyMessageCreated(sessionUserId, conversationType, targetUserId, rowId);
            return true;
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        }
        return false;
    }

    /**
     * @param message
     * @return 更新成功返回 true, 否则返回 false.
     */
    public boolean updateMessage(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId,
            final Message message) {
        IMConstants.ConversationType.check(conversationType);

        if (message == null) {
            Throwable e = new IllegalArgumentException("message is null");
            IMLog.e(e);
            return false;
        }

        if (message.localId.isUnset()) {
            IMLog.e(
                    new IllegalArgumentException("invalid message localId"),
                    "message localId is unset, you may need use insert instead of update"
            );
            return false;
        }

        // 设置 last modify
        message.localLastModifyMs.set(System.currentTimeMillis());

        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            final String tableName = dbHelper.createTableMessageIfNeed(conversationType, targetUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            //noinspection StringBufferReplaceableByString
            final StringBuilder where = new StringBuilder();
            final List<String> whereArgs = new ArrayList<>();

            where.append(" " + DatabaseHelper.ColumnsMessage.C_LOCAL_ID + "=? ");
            whereArgs.add(String.valueOf(message.localId.get()));

            int rowsAffected = db.update(
                    tableName,
                    message.toContentValues(),
                    where.toString(),
                    whereArgs.toArray(new String[]{})
            );
            if (rowsAffected != 1) {
                IMLog.e(
                        new IllegalAccessError("unexpected update message"),
                        "update message with sessionUserId:%s, message localId:%s, conversationType:%s, targetUserId:%s affected %s rows",
                        sessionUserId,
                        message.localId.get(),
                        conversationType,
                        targetUserId,
                        rowsAffected
                );
            }
            MemoryFullCache.DEFAULT.removeFullCache(sessionUserId, conversationType, targetUserId, message.localId.get());
            MessageObservable.DEFAULT.notifyMessageChanged(sessionUserId, conversationType, targetUserId, message.localId.get());
            return rowsAffected > 0;
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        }
        return false;
    }

    /**
     * 更新 block id
     */
    public boolean updateBlockId(final long sessionUserId,
                                 final int conversationType,
                                 final long targetUserId,
                                 final long fromBlockId,
                                 final long toBlockId) {
        IMConstants.ConversationType.check(conversationType);

        if (fromBlockId <= 0) {
            Throwable e = new IllegalArgumentException("invalid fromBlockId " + fromBlockId);
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
            return false;
        }

        if (toBlockId <= 0) {
            Throwable e = new IllegalArgumentException("invalid toBlockId " + toBlockId);
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
            return false;
        }

        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            final String tableName = dbHelper.createTableMessageIfNeed(conversationType, targetUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            final ContentValues contentValuesUpdate = new ContentValues();
            contentValuesUpdate.put(DatabaseHelper.ColumnsMessage.C_LOCAL_BLOCK_ID, toBlockId);
            // 设置 last modify
            contentValuesUpdate.put(DatabaseHelper.ColumnsMessage.C_LOCAL_LAST_MODIFY_MS, System.currentTimeMillis());

            //noinspection StringBufferReplaceableByString
            final StringBuilder where = new StringBuilder();
            final List<String> whereArgs = new ArrayList<>();

            where.append(" " + DatabaseHelper.ColumnsMessage.C_LOCAL_BLOCK_ID + "=? ");
            whereArgs.add(String.valueOf(fromBlockId));

            int rowsAffected = db.update(
                    tableName,
                    contentValuesUpdate,
                    where.toString(),
                    whereArgs.toArray(new String[]{})
            );

            IMLog.v(
                    "updateMessageBlockId with sessionUserId:%s, conversationType:%s, targetUserId:%s, fromBlockId:%s, toBlockId:%s affected %s rows",
                    sessionUserId,
                    conversationType,
                    targetUserId,
                    fromBlockId,
                    toBlockId,
                    rowsAffected
            );

            if (rowsAffected > 0) {
                MemoryFullCache.DEFAULT.clear();
                MessageObservable.DEFAULT.notifyMessageBlockChanged(sessionUserId, conversationType, targetUserId, fromBlockId, toBlockId);
                return true;
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        }
        return false;
    }

}
