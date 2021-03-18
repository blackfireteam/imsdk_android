package com.masonsoft.imsdk.core.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.core.Singleton;
import com.idonans.core.util.IOUtil;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMProcessValidator;
import com.masonsoft.imsdk.core.RuntimeMode;

import java.util.ArrayList;
import java.util.List;

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

    private MessageDatabaseProvider() {
    }

    /**
     * 查询结果按照 seq 从大到小排列
     *
     * @param seq 不包括这一条, 初始传 0
     */
    @NonNull
    public TinyPage<Message> pageQueryMessage(
            final long sessionUserId,
            final long seq,
            final int limit,
            final int conversationType,
            final long targetUserId,
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
                selection.append(" and " + DatabaseHelper.ColumnsConversation.C_LOCAL_SEQ + "<? ");
                selectionArgs.add(String.valueOf(seq));
            }

            cursor = db.query(
                    tableName,
                    columnsSelector.queryColumns(),
                    selection.toString(),
                    selectionArgs.toArray(new String[]{}),
                    null,
                    null,
                    DatabaseHelper.ColumnsMessage.C_LOCAL_SEQ + " desc",
                    String.valueOf(limit + 1) // 此处多查询一条用来计算 hasMore
            );

            while (cursor.moveToNext()) {
                Message item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
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
            final long localMessageId,
            @Nullable ColumnsSelector<Message> columnsSelector) {
        IMConstants.ConversationType.check(conversationType);

        if (columnsSelector == null) {
            columnsSelector = Message.COLUMNS_SELECTOR_ALL;
        }

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
                Message item = columnsSelector.cursorToObjectWithQueryColumns(cursor);

                IMLog.v(
                        "found message with localMessageId:%s, sessionUserId:%s, conversationType:%s, targetUserId:%s",
                        localMessageId, sessionUserId, conversationType, targetUserId
                );
                return item;
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        IMLog.v(
                "message not found with localMessageId:%s, sessionUserId:%s, conversationType:%s, targetUserId:%s",
                localMessageId, sessionUserId, conversationType, targetUserId
        );
        return null;
    }

    @Nullable
    public Message getMessageByServerMessageId(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId,
            final long serverMessageId,
            @Nullable ColumnsSelector<Message> columnsSelector) {
        IMConstants.ConversationType.check(conversationType);

        if (columnsSelector == null) {
            columnsSelector = Message.COLUMNS_SELECTOR_ALL;
        }

        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            final String tableName = dbHelper.createTableMessageIfNeed(conversationType, targetUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            //noinspection StringBufferReplaceableByString
            final StringBuilder selection = new StringBuilder();
            final List<String> selectionArgs = new ArrayList<>();

            selection.append(" " + DatabaseHelper.ColumnsMessage.C_REMOTE_MSG_ID + "=? ");
            selectionArgs.add(String.valueOf(serverMessageId));

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
                Message item = columnsSelector.cursorToObjectWithQueryColumns(cursor);

                IMLog.v(
                        "found message with serverMessageId:%s, sessionUserId:%s, conversationType:%s, targetUserId:%s",
                        serverMessageId, sessionUserId, conversationType, targetUserId
                );
                return item;
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        IMLog.v(
                "message not found with serverMessageId:%s, sessionUserId:%s, conversationType:%s, targetUserId:%s",
                serverMessageId, sessionUserId, conversationType, targetUserId
        );
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
            return rowsAffected > 0;
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        }
        return false;
    }

}
