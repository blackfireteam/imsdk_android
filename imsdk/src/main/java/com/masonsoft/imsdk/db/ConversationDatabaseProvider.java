package com.masonsoft.imsdk.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.core.Singleton;
import com.idonans.core.util.IOUtil;
import com.masonsoft.imsdk.IMConstants;
import com.masonsoft.imsdk.IMLog;
import com.masonsoft.imsdk.core.IMProcessValidator;
import com.masonsoft.imsdk.core.RuntimeMode;

import java.util.ArrayList;
import java.util.List;

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
                items.add(item);
            }
        } catch (Throwable e) {
            IMLog.e(e);
            if (RuntimeMode.isDebug()) {
                throw new RuntimeException(e);
            }
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
     * @return 没有找到返回 null
     */
    @Nullable
    public Conversation getConversation(
            final long sessionUserId,
            final long conversationId,
            @Nullable ColumnsSelector<Conversation> columnsSelector) {
        if (columnsSelector == null) {
            columnsSelector = Conversation.COLUMNS_SELECTOR_ALL;
        }

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
                IMLog.v("conversation found with sessionUserId:%s, conversationId:%s", sessionUserId, conversationId);
                return result;
            }
        } catch (Throwable e) {
            IMLog.e(e);
            if (RuntimeMode.isDebug()) {
                throw new RuntimeException(e);
            }
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
            final long targetUserId,
            final int conversationType,
            @Nullable ColumnsSelector<Conversation> columnsSelector) {
        IMConstants.ConversationType.check(conversationType);

        if (columnsSelector == null) {
            columnsSelector = Conversation.COLUMNS_SELECTOR_ALL;
        }

        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            final StringBuilder selection = new StringBuilder();
            final List<String> selectionArgs = new ArrayList<>();

            selection.append(" " + DatabaseHelper.ColumnsConversation.C_TARGET_USER_ID + "=? ");
            selectionArgs.add(String.valueOf(targetUserId));

            selection.append(" " + DatabaseHelper.ColumnsConversation.C_LOCAL_CONVERSATION_TYPE + "=? ");
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
                IMLog.v("conversation found with sessionUserId:%s, targetUserId:%s, conversationType:%s",
                        sessionUserId, targetUserId, conversationType);
                return result;
            }
        } catch (Throwable e) {
            IMLog.e(e);
            if (RuntimeMode.isDebug()) {
                throw new RuntimeException(e);
            }
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

            // 自增主键
            conversation.localId.set(rowId);
            return true;
        } catch (Throwable e) {
            IMLog.e(e);
            if (RuntimeMode.isDebug()) {
                throw new RuntimeException(e);
            }
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
            return rowsAffected > 0;
        } catch (Throwable e) {
            IMLog.e(e);
            if (RuntimeMode.isDebug()) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    /**
     * 软删除所有会话
     *
     * @see #updateConversation(long, Conversation)
     */
    public boolean deleteAllConversation(final long sessionUserId) {
        try {
            final ContentValues updateContentValues = new ContentValues();
            updateContentValues.put(DatabaseHelper.ColumnsConversation.C_LOCAL_DELETE, IMConstants.TRUE);

            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            int rowsAffected = db.update(
                    DatabaseHelper.TABLE_NAME_CONVERSATION,
                    updateContentValues,
                    null,
                    null
            );
            IMLog.v("delete all conversation with sessionUserId:%s affected %s rows",
                    sessionUserId,
                    rowsAffected);
            return true;
        } catch (Throwable e) {
            IMLog.e(e);
            if (RuntimeMode.isDebug()) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

}
