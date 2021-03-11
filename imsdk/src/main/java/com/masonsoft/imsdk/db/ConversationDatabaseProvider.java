package com.masonsoft.imsdk.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.core.Singleton;
import com.idonans.core.util.IOUtil;
import com.masonsoft.imsdk.IMLog;

import java.util.ArrayList;
import java.util.List;

/**
 * 会话表
 */
public class ConversationDatabaseProvider {

    private static final Singleton<ConversationDatabaseProvider> INSTANCE = new Singleton<ConversationDatabaseProvider>() {
        @Override
        protected ConversationDatabaseProvider create() {
            return new ConversationDatabaseProvider();
        }
    };

    public static ConversationDatabaseProvider getInstance() {
        return INSTANCE.get();
    }

    private ConversationDatabaseProvider() {
    }

    /**
     * 查询结果按照 seq 从大到小排列
     */
    @NonNull
    public TinyPage<Conversation> pageQueryConversation(
            final long sessionUserId,
            final int conversationType,
            final long seq  /*不包括这一条, 初始传 0*/,
            final int limit,
            @Nullable ColumnsSelector<Conversation> columnsSelector) {
        if (columnsSelector == null) {
            columnsSelector = Conversation.COLUMNS_SELECTOR_ALL;
        }
        final List<Conversation> items = new ArrayList<>();
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            if (seq > 0) {
                cursor = db.query(
                        DatabaseHelper.TABLE_NAME_CONVERSATION,
                        columnsSelector.queryColumns(),
                        DatabaseHelper.ColumnsConversation.C_CONVERSATION_TYPE + "=? and " +
                                DatabaseHelper.ColumnsConversation.C_SEQ + "<?",
                        new String[]{
                                String.valueOf(conversationType),
                                String.valueOf(seq),
                        },
                        null,
                        null,
                        DatabaseHelper.ColumnsConversation.C_SEQ + " desc",
                        String.valueOf(limit + 1) // 此处多查询一条用来计算 hasMore
                );
            } else {
                cursor = db.query(
                        DatabaseHelper.TABLE_NAME_CONVERSATION,
                        columnsSelector.queryColumns(),
                        DatabaseHelper.ColumnsConversation.C_CONVERSATION_TYPE + "=?",
                        new String[]{
                                String.valueOf(conversationType),
                        },
                        null,
                        null,
                        DatabaseHelper.ColumnsConversation.C_SEQ + " desc",
                        String.valueOf(limit + 1) // 此处多查询一条用来计算 hasMore
                );
            }

            while (cursor.moveToNext()) {
                Conversation item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                items.add(item);
            }
        } catch (Throwable e) {
            IMLog.e(e);
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
    public Conversation getConversation(final long sessionUserId, final long conversationId, @Nullable ColumnsSelector<Conversation> columnsSelector) {
        if (columnsSelector == null) {
            columnsSelector = Conversation.COLUMNS_SELECTOR_ALL;
        }
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_CONVERSATION,
                    columnsSelector.queryColumns(),
                    DatabaseHelper.ColumnsConversation.C_ID + "=?",
                    new String[]{
                            String.valueOf(conversationId)
                    },
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
    public Conversation getConversation(final long sessionUserId, final long targetUserId, final int conversationType, @Nullable ColumnsSelector<Conversation> columnsSelector) {
        if (columnsSelector == null) {
            columnsSelector = Conversation.COLUMNS_SELECTOR_ALL;
        }
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_CONVERSATION,
                    columnsSelector.queryColumns(),
                    DatabaseHelper.ColumnsConversation.C_TARGET_USER_ID + "=? and "
                            + DatabaseHelper.ColumnsConversation.C_CONVERSATION_TYPE + "=?",
                    new String[]{
                            String.valueOf(targetUserId),
                            String.valueOf(conversationType)
                    },
                    null,
                    null,
                    null,
                    "1"
            );

            if (cursor.moveToNext()) {
                Conversation result = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                IMLog.v("conversation found with sessionUserId:%s, targetUserId:%s, conversationType:%s", sessionUserId, targetUserId, conversationType);
                return result;
            }
        } catch (Throwable e) {
            IMLog.e(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        // conversation not found
        IMLog.v("conversation not found with sessionUserId:%s, targetUserId:%s, conversationType:%s",
                sessionUserId, targetUserId, conversationType);
        return null;
    }

    /**
     * @param conversation
     * @return 更新成功返回 true, 否则返回 false.
     */
    public boolean updateConversation(final long sessionUserId, final Conversation conversation) {
        if (conversation == null) {
            Throwable e = new IllegalArgumentException("conversation is null");
            Timber.e(e);
            return false;
        }

        if (conversation.id <= 0) {
            Throwable e = new IllegalArgumentException("invalid conversation id " + conversation.id);
            Timber.e(e);
            return false;
        }

        try {
            com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper dbHelper = com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            int rowsAffected = db.update(
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.TABLE_NAME_CONVERSATION,
                    conversation.toContentValues(),
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_ID + "=?",
                    new String[]{String.valueOf(conversation.id)}
            );
            if (rowsAffected != 1) {
                Throwable e = new IllegalAccessException("update conversation for session user id:" + sessionUserId + " with id:" + conversation.id + " affected " + rowsAffected + " rows");
                Timber.e(e);
            } else {
                Timber.v("update conversation for session user id:%s with id:%s affected %s rows", sessionUserId, conversation.id, rowsAffected);
            }
            return rowsAffected > 0;
        } catch (Throwable e) {
            Timber.e(e);
        }
        return false;
    }

    public boolean deleteConversation(final long sessionUserId, final Conversation conversation) {
        if (conversation == null) {
            Throwable e = new IllegalArgumentException("conversation is null");
            Timber.e(e);
            return false;
        }

        if (conversation.id <= 0) {
            Throwable e = new IllegalArgumentException("invalid conversation id " + conversation.id);
            Timber.e(e);
            return false;
        }

        try {
            com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper dbHelper = com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            int rowsAffected = db.delete(
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.TABLE_NAME_CONVERSATION,
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_ID + "=?",
                    new String[]{String.valueOf(conversation.id)}
            );
            if (rowsAffected != 1) {
                Throwable e = new IllegalAccessException("delete conversation for session user id:" + sessionUserId + " with id:" + conversation.id + " affected " + rowsAffected + " rows");
                Timber.e(e);
            } else {
                Timber.v("delete conversation for session user id:%s with id:%s affected %s rows", sessionUserId, conversation.id, rowsAffected);
            }
            return rowsAffected > 0;
        } catch (Throwable e) {
            Timber.e(e);
        }
        return false;
    }

    public boolean deleteAllConversation(final long sessionUserId, int systemType) {
        try {
            if (systemType != ImConstant.ConversationSystemType.SYSTEM_TYPE_CHAT) {
                Throwable e = new IllegalAccessException("WARN:: deleteAllConversation sessionUserId:" + sessionUserId + ", systemType:" + systemType + ", use update would be better.");
                Timber.e(e);
            }

            com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper dbHelper = com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            int rowsAffected = db.delete(
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.TABLE_NAME_CONVERSATION,
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_SYSTEM_TYPE + "=?",
                    new String[]{
                            String.valueOf(systemType)
                    }
            );
            Timber.v("delete all conversation  with session user id:%s, systemType:%s affected %s rows",
                    sessionUserId,
                    systemType,
                    rowsAffected);
            return true;
        } catch (Throwable e) {
            Timber.e(e);
        }
        return false;
    }

}
