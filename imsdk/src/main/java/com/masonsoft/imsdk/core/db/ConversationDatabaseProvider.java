package com.masonsoft.imsdk.core.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMProcessValidator;
import com.masonsoft.imsdk.core.RuntimeMode;
import com.masonsoft.imsdk.core.observable.ConversationObservable;
import com.masonsoft.imsdk.util.CursorUtil;

import java.util.ArrayList;
import java.util.List;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.util.IOUtil;

/**
 * 会话表
 *
 * @since 1.0
 */
public class ConversationDatabaseProvider {

    private static final Singleton<ConversationDatabaseProvider> INSTANCE = new Singleton<ConversationDatabaseProvider>() {
        @Override
        protected ConversationDatabaseProvider create() {
            return new ConversationDatabaseProvider();
        }
    };

    public static ConversationDatabaseProvider getInstance() {
        IMProcessValidator.validateProcess();

        return INSTANCE.get();
    }

    private static class MemoryFullCache {

        private static final MemoryFullCache DEFAULT = new MemoryFullCache();

        private static final int MEMORY_CACHE_SIZE = 500;
        @NonNull
        private final LruCache<String, Conversation> mFullCaches = new LruCache<>(MEMORY_CACHE_SIZE);

        private String buildKey(long sessionUserId, long conversationId) {
            return sessionUserId + "_" + conversationId;
        }

        private String buildKeyWithTargetUserId(long sessionUserId, int conversationType, long targetUserId) {
            return sessionUserId + "_" + conversationType + "_" + targetUserId;
        }

        private void addFullCache(long sessionUserId, @NonNull Conversation conversation) {
            try {
                {
                    final String key = buildKey(sessionUserId, conversation.localId.get());
                    mFullCaches.put(key, conversation);
                }
                {
                    // 同时缓存 by targetUserId
                    final String key = buildKeyWithTargetUserId(sessionUserId, conversation.localConversationType.get(), conversation.targetUserId.get());
                    mFullCaches.put(key, conversation);
                }
            } catch (Throwable e) {
                IMLog.e(e);
                RuntimeMode.fixme(e);
            }
        }

        private void removeFullCache(long sessionUserId, long conversationId) {
            final String key = buildKey(sessionUserId, conversationId);
            final Conversation cache = mFullCaches.get(key);
            if (cache != null) {
                removeFullCacheInternal(sessionUserId, cache);
            }
        }

        private void removeFullCacheInternal(long sessionUserId, @NonNull Conversation conversation) {
            try {
                {
                    final String key = buildKey(sessionUserId, conversation.localId.get());
                    mFullCaches.remove(key);
                }
                {
                    // 同时删除 by targetUserId
                    final String key = buildKeyWithTargetUserId(sessionUserId, conversation.localConversationType.get(), conversation.targetUserId.get());
                    mFullCaches.remove(key);
                }
            } catch (Throwable e) {
                IMLog.e(e);
                RuntimeMode.fixme(e);
            }
        }

        @Nullable
        private Conversation getFullCache(long sessionUserId, long conversationId) {
            final String key = buildKey(sessionUserId, conversationId);
            return mFullCaches.get(key);
        }

        @Nullable
        private Conversation getFullCacheWithTargetUserId(long sessionUserId, int conversationType, long targetUserId) {
            final String key = buildKeyWithTargetUserId(sessionUserId, conversationType, targetUserId);
            return mFullCaches.get(key);
        }

    }

    private static class MemoryAllUnreadCountCache {

        private static final MemoryAllUnreadCountCache DEFAULT = new MemoryAllUnreadCountCache();

        private static final int MEMORY_CACHE_SIZE = 2000;

        @NonNull
        private final LruCache<Long, Integer> mAllUnreadCountCaches = new LruCache<>(MEMORY_CACHE_SIZE);

        private void addAllUnreadCountCache(long sessionUserId, int allUnreadCount) {
            mAllUnreadCountCaches.put(sessionUserId, allUnreadCount);
        }

        private void removeAllUnreadCountCache(long sessionUserId) {
            mAllUnreadCountCaches.remove(sessionUserId);
        }

        @Nullable
        private Integer getAllUnreadCountCache(long sessionUserId) {
            return mAllUnreadCountCaches.get(sessionUserId);
        }

    }

    private ConversationDatabaseProvider() {
    }

    /**
     * 查询结果按照 seq 从大到小排列
     *
     * @param seq           不包括这一条, 初始传 0
     * @param includeDelete 是否查询已删除的会话
     */
    @NonNull
    public TinyPage<Conversation> pageQueryConversation(
            final long sessionUserId,
            final long seq,
            final int limit,
            final int conversationType,
            final boolean includeDelete,
            @Nullable ColumnsSelector<Conversation> columnsSelector) {
        IMConstants.ConversationType.check(conversationType);

        if (columnsSelector == null) {
            columnsSelector = Conversation.COLUMNS_SELECTOR_ALL;
        }
        final List<Conversation> items = new ArrayList<>();
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            final StringBuilder selection = new StringBuilder();
            final List<String> selectionArgs = new ArrayList<>();

            selection.append(" " + DatabaseHelper.ColumnsConversation.C_LOCAL_CONVERSATION_TYPE + "=? ");
            selectionArgs.add(String.valueOf(conversationType));

            if (seq > 0) {
                selection.append(" and " + DatabaseHelper.ColumnsConversation.C_LOCAL_SEQ + "<? ");
                selectionArgs.add(String.valueOf(seq));
            }

            if (!includeDelete) {
                selection.append(" and " + DatabaseHelper.ColumnsConversation.C_LOCAL_DELETE + "=0 ");
            }

            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_CONVERSATION,
                    columnsSelector.queryColumns(),
                    selection.toString(),
                    selectionArgs.toArray(new String[]{}),
                    null,
                    null,
                    DatabaseHelper.ColumnsConversation.C_LOCAL_SEQ + " desc",
                    String.valueOf(limit + 1) // 此处多查询一条用来计算 hasMore
            );

            while (cursor.moveToNext()) {
                Conversation item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                item.applyLogicField(sessionUserId);
                items.add(item);
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.fixme(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        final TinyPage<Conversation> result = new TinyPage<>();
        result.hasMore = items.size() > limit;
        if (result.hasMore) {
            result.items = new ArrayList<>(items.subList(0, limit));
        } else {
            result.items = items;
        }

        IMLog.v("found %s conversations[hasMore:%s] with sessionUserId:%s, conversationType:%s, seq:%s, limit:%s",
                result.items.size(), result.hasMore, sessionUserId, conversationType, seq, limit);
        return result;
    }

    /**
     * 获取所有会话的未读消息数
     */
    public int getAllUnreadCount(final long sessionUserId) {
        final Integer cache = MemoryAllUnreadCountCache.DEFAULT.getAllUnreadCountCache(sessionUserId);
        if (cache != null) {
            if (cache != null) {
                IMLog.v("getAllUnreadCount cache hit sessionUserId:%s", sessionUserId);
                return cache;
            }
            return cache;
        }

        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            final String sql = "select sum(" + DatabaseHelper.ColumnsConversation.C_LOCAL_UNREAD_COUNT + ") from "
                    + DatabaseHelper.TABLE_NAME_CONVERSATION;
            cursor = db.rawQuery(sql, null);
            final int count = CursorUtil.getInt(cursor, 0);
            IMLog.v("getAllUnreadCount sessionUserId:%s, count:%s", sessionUserId, count);
            MemoryAllUnreadCountCache.DEFAULT.addAllUnreadCountCache(sessionUserId, count);
            return count;
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.fixme(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        // fallback
        return 0;
    }

    /**
     * @return 没有找到返回 null
     */
    @Nullable
    public Conversation getConversation(
            final long sessionUserId,
            final long conversationId) {

        final ColumnsSelector<Conversation> columnsSelector = Conversation.COLUMNS_SELECTOR_ALL;

        final Conversation cache = MemoryFullCache.DEFAULT.getFullCache(sessionUserId, conversationId);
        if (cache != null) {
            IMLog.v("getConversation cache hit sessionUserId:%s, conversationId:%s",
                    sessionUserId,
                    conversationId);
            return cache;
        }

        IMLog.v("getConversation cache miss, try read from db, sessionUserId:%s, conversationId:%s",
                sessionUserId,
                conversationId);

        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            //noinspection StringBufferReplaceableByString
            final StringBuilder selection = new StringBuilder();
            final List<String> selectionArgs = new ArrayList<>();

            selection.append(" " + DatabaseHelper.ColumnsConversation.C_LOCAL_ID + "=? ");
            selectionArgs.add(String.valueOf(conversationId));

            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_CONVERSATION,
                    columnsSelector.queryColumns(),
                    selection.toString(),
                    selectionArgs.toArray(new String[]{}),
                    null,
                    null,
                    null,
                    "1"
            );

            if (cursor.moveToNext()) {
                final Conversation result = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                result.applyLogicField(sessionUserId);

                IMLog.v("conversation found with sessionUserId:%s, conversationId:%s", sessionUserId, conversationId);

                MemoryFullCache.DEFAULT.addFullCache(sessionUserId, result);
                return result;
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.fixme(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        // conversation not found
        IMLog.v("conversation not found with sessionUserId:%s, conversationId:%s",
                sessionUserId, conversationId);
        return null;
    }

    /**
     * @return 没有找到返回 null
     */
    @Nullable
    public Conversation getConversationByTargetUserId(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId) {
        IMConstants.ConversationType.check(conversationType);

        final ColumnsSelector<Conversation> columnsSelector = Conversation.COLUMNS_SELECTOR_ALL;

        final Conversation cache = MemoryFullCache.DEFAULT.getFullCacheWithTargetUserId(sessionUserId, conversationType, targetUserId);
        if (cache != null) {
            IMLog.v("getConversationByTargetUserId cache hit sessionUserId:%s, conversationType:%s, targetUserId:%s",
                    sessionUserId,
                    conversationType,
                    targetUserId);
            return cache;
        }

        IMLog.v("getConversationByTargetUserId cache miss, try read from db, sessionUserId:%s, conversationType:%s, targetUserId:%s",
                sessionUserId,
                conversationType,
                targetUserId);

        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            final StringBuilder selection = new StringBuilder();
            final List<String> selectionArgs = new ArrayList<>();

            selection.append(" " + DatabaseHelper.ColumnsConversation.C_TARGET_USER_ID + "=? ");
            selectionArgs.add(String.valueOf(targetUserId));

            selection.append(" and " + DatabaseHelper.ColumnsConversation.C_LOCAL_CONVERSATION_TYPE + "=? ");
            selectionArgs.add(String.valueOf(conversationType));

            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_CONVERSATION,
                    columnsSelector.queryColumns(),
                    selection.toString(),
                    selectionArgs.toArray(new String[]{}),
                    null,
                    null,
                    null,
                    "1"
            );

            if (cursor.moveToNext()) {
                Conversation result = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                result.applyLogicField(sessionUserId);

                IMLog.v("conversation found with sessionUserId:%s, targetUserId:%s, conversationType:%s",
                        sessionUserId, targetUserId, conversationType);

                MemoryFullCache.DEFAULT.addFullCache(sessionUserId, result);
                return result;
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.fixme(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        // conversation not found
        IMLog.v("conversation not found with sessionUserId:%s, targetUserId:%s, conversationType:%s",
                sessionUserId, targetUserId, conversationType);
        return null;
    }

    /**
     * 插入一条会话。新会话插入成功时，会自动设置会话的 localId
     *
     * @return
     */
    public boolean insertConversation(
            final long sessionUserId,
            final Conversation conversation) {
        if (conversation == null) {
            IMLog.e(new IllegalArgumentException("conversation is null"));
            return false;
        }

        if (!conversation.localId.isUnset()) {
            IMLog.e(
                    new IllegalArgumentException("invalid conversation localId"),
                    "conversation localId:%s, you may need use update instead of insert",
                    conversation.localId.get()
            );
            return false;
        }

        if (conversation.localConversationType.isUnset()) {
            IMLog.e(
                    new IllegalArgumentException("invalid conversation localConversationType"),
                    "conversation localConversationType is unset"
            );
            return false;
        }

        if (conversation.targetUserId.isUnset()) {
            IMLog.e(
                    new IllegalArgumentException("invalid conversation targetUserId"),
                    "conversation targetUserId is unset"
            );
            return false;
        }

        // 设置 last modify
        conversation.localLastModifyMs.set(System.currentTimeMillis());

        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            long rowId = db.insert(
                    DatabaseHelper.TABLE_NAME_CONVERSATION,
                    null,
                    conversation.toContentValues());
            if (rowId == -1) {
                IMLog.e(
                        new IllegalAccessError("insert conversation fail"),
                        "fail to insert conversation with sessionUserId:%s, localConversationType:%s, targetUserId:%s",
                        sessionUserId, conversation.localConversationType.get(), conversation.targetUserId.get()
                );
                return false;
            }

            // 当新加入的会话有未读消息数时，需要移除未读消息总数的缓存
            if (!conversation.localUnreadCount.isUnset()) {
                if (conversation.localUnreadCount.get() != 0) {
                    MemoryAllUnreadCountCache.DEFAULT.removeAllUnreadCountCache(sessionUserId);
                }
            }

            // 自增主键
            conversation.localId.set(rowId);
            ConversationObservable.DEFAULT.notifyConversationCreated(
                    sessionUserId,
                    rowId,
                    conversation.localConversationType.get(),
                    conversation.targetUserId.get());
            return true;
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.fixme(e);
        }
        return false;
    }

    /**
     * @param conversation
     * @return 更新成功返回 true, 否则返回 false.
     */
    public boolean updateConversation(
            final long sessionUserId,
            final Conversation conversation) {
        if (conversation == null) {
            IMLog.e(new IllegalArgumentException("conversation is null"));
            return false;
        }

        if (conversation.localId.isUnset()) {
            IMLog.e(new IllegalArgumentException("conversation localId is unset"));
            return false;
        }

        // 设置 last modify
        conversation.localLastModifyMs.set(System.currentTimeMillis());

        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            //noinspection StringBufferReplaceableByString
            final StringBuilder where = new StringBuilder();
            final List<String> whereArgs = new ArrayList<>();

            where.append(" " + DatabaseHelper.ColumnsConversation.C_LOCAL_ID + "=? ");
            whereArgs.add(String.valueOf(conversation.localId.get()));

            int rowsAffected = db.update(
                    DatabaseHelper.TABLE_NAME_CONVERSATION,
                    conversation.toContentValues(),
                    where.toString(),
                    whereArgs.toArray(new String[]{})
            );
            if (rowsAffected != 1) {
                IMLog.e(
                        new IllegalAccessException("update conversation fail"),
                        "unexpected. update conversation with sessionUserId:% conversation localId:%s affected %s rows",
                        sessionUserId,
                        conversation.localId.get(),
                        rowsAffected
                );
            } else {
                IMLog.v("update conversation with sessionUserId:%s conversation localId:%s affected %s rows",
                        sessionUserId,
                        conversation.localId.get(),
                        rowsAffected);
            }
            MemoryFullCache.DEFAULT.removeFullCache(sessionUserId, conversation.localId.get());

            // 当会话的未读消息数发生更新时，需要移除未读消息总数的缓存
            if (!conversation.localUnreadCount.isUnset()) {
                MemoryAllUnreadCountCache.DEFAULT.removeAllUnreadCountCache(sessionUserId);
            }

            final Conversation readConversation = getConversation(sessionUserId, conversation.localId.get());
            if (readConversation != null) {
                ConversationObservable.DEFAULT.notifyConversationChanged(
                        sessionUserId,
                        readConversation.localId.get(),
                        readConversation.localConversationType.get(),
                        readConversation.targetUserId.get());
            } else {
                final Throwable e = new IllegalAccessError("conversation not found with sessionUserId:" + sessionUserId + ", conversation localId:" + conversation.localId.get());
                IMLog.e(e);
            }
            return rowsAffected > 0;
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.fixme(e);
        }
        return false;
    }

}
