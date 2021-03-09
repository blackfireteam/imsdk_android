package com.masonsoft.imsdk.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.lang.Singleton;
import com.idonans.lang.util.IOUtil;
import com.xmqvip.xiaomaiquan.common.Debug;
import com.xmqvip.xiaomaiquan.common.im.ImConstant;
import com.xmqvip.xiaomaiquan.common.im.core.DataFormat;
import com.xmqvip.xiaomaiquan.common.im.core.db.entity.Message;
import com.xmqvip.xiaomaiquan.common.im.core.db.entity.Page;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * 消息表
 */
public class ImMessageDatabaseProvider {

    private static final boolean DEBUG = Debug.isDebug();

    private static final Singleton<com.xmqvip.xiaomaiquan.common.im.core.db.ImMessageDatabaseProvider> sInstance = new Singleton<com.xmqvip.xiaomaiquan.common.im.core.db.ImMessageDatabaseProvider>() {
        @Override
        protected com.xmqvip.xiaomaiquan.common.im.core.db.ImMessageDatabaseProvider create() {
            return new com.xmqvip.xiaomaiquan.common.im.core.db.ImMessageDatabaseProvider();
        }
    };

    public static com.xmqvip.xiaomaiquan.common.im.core.db.ImMessageDatabaseProvider getInstance() {
        return sInstance.get();
    }

    private ImMessageDatabaseProvider() {
    }

    public long count(final long sessionUserId, final long conversationId) {
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            cursor = db.rawQuery("select count(*) from " + DatabaseHelper.TABLE_NAME_MESSAGE + " where " + DatabaseHelper.ColumnsMessage.C_CONVERSATION_ID + "=?",
                    new String[]{String.valueOf(conversationId)});
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            IOUtil.closeQuietly(cursor);
        }
        return -1;
    }

    /**
     * 获取最新的消息。查询结果按照消息 id 从小到大排列.
     *
     * @param sessionUserId
     * @param conversationId 会话表自增 id
     * @param offset         本次查询的最大数量
     * @return
     */
    @NonNull
    public Page<Message> getLatestMessages(final long sessionUserId, final long conversationId, final int offset, @Nullable ColumnsSelector<Message> columnsSelector) {
        if (columnsSelector == null) {
            columnsSelector = COLUMNS_SELECTOR_FULL;
        }
        final Page<Message> page = new Page<>();
        page.total = 0;
        page.hasMore = false;
        page.data = null;

        final List<Message> items = new ArrayList<>();
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_MESSAGE,
                    columnsSelector.queryColumns(),
                    DatabaseHelper.ColumnsMessage.C_CONVERSATION_ID + "=?",
                    new String[]{
                            String.valueOf(conversationId)
                    },
                    null,
                    null,
                    DatabaseHelper.ColumnsMessage.C_ID + " desc",
                    "0," + (offset + 1) // 此处多查询一条用来计算 hasMore
            );

            for (cursor.moveToLast(); !cursor.isBeforeFirst(); cursor.moveToPrevious()) {
                Message item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                items.add(item);
            }
        } catch (Throwable e) {
            Timber.e(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        page.total = count(sessionUserId, conversationId);
        page.hasMore = items.size() > offset;
        if (page.hasMore) {
            page.data = items.subList(1, offset + 1);
        } else {
            page.data = items;
        }

        if (DEBUG) {
            Timber.v("found latest messages %s for session user id:%s, conversation id:%s, offset:%s",
                    page, sessionUserId, conversationId, offset);
        }
        return page;
    }

    /**
     * 获取更旧的消息，查询比目标消息 id 更小的消息。查询结果按照消息 id 从小到大排列.
     *
     * @param sessionUserId
     * @param conversationId 会话表自增 id
     * @param localMessageId 消息表自增 id, 查询结果不包含此条消息
     * @param offset         本次查询的最大数量
     * @return
     */
    @NonNull
    public Page<Message> getOlderMessages(final long sessionUserId, final long conversationId, final long localMessageId, final int offset, @Nullable ColumnsSelector<Message> columnsSelector) {
        if (columnsSelector == null) {
            columnsSelector = COLUMNS_SELECTOR_FULL;
        }
        final Page<Message> page = new Page<>();
        page.total = 0;
        page.hasMore = false;
        page.data = null;

        final List<Message> items = new ArrayList<>();
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_MESSAGE,
                    columnsSelector.queryColumns(),
                    DatabaseHelper.ColumnsMessage.C_CONVERSATION_ID + "=? and " +
                            DatabaseHelper.ColumnsMessage.C_ID + "<?",
                    new String[]{
                            String.valueOf(conversationId),
                            String.valueOf(localMessageId)
                    },
                    null,
                    null,
                    DatabaseHelper.ColumnsMessage.C_ID + " desc",
                    "0," + (offset + 1) // 此处多查询一条用来计算 hasMore
            );

            for (cursor.moveToLast(); !cursor.isBeforeFirst(); cursor.moveToPrevious()) {
                Message item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                items.add(item);
            }
        } catch (Throwable e) {
            Timber.e(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        page.total = count(sessionUserId, conversationId);
        page.hasMore = items.size() > offset;
        if (page.hasMore) {
            page.data = items.subList(1, offset + 1);
        } else {
            page.data = items;
        }

        if (DEBUG) {
            Timber.v("found old messages %s for session user id:%s, conversation id:%s, local message id:%s, offset:%s",
                    page, sessionUserId, conversationId, localMessageId, offset);
        }
        return page;
    }

    /**
     * 获取更新的消息，查询比目标消息 id 更大的消息。查询结果按照消息 id 从小到大排列.
     *
     * @param sessionUserId
     * @param conversationId 会话表自增 id
     * @param localMessageId 消息表自增 id, 查询结果不包含此条消息
     * @param offset         本次查询的最大数量
     * @return
     */
    @NonNull
    public Page<Message> getNewerMessages(final long sessionUserId, final long conversationId, final long localMessageId, final int offset, @Nullable ColumnsSelector<Message> columnsSelector) {
        if (columnsSelector == null) {
            columnsSelector = COLUMNS_SELECTOR_FULL;
        }
        final Page<Message> page = new Page<>();
        page.total = 0;
        page.hasMore = false;
        page.data = null;

        final List<Message> items = new ArrayList<>();
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_MESSAGE,
                    columnsSelector.queryColumns(),
                    DatabaseHelper.ColumnsMessage.C_CONVERSATION_ID + "=? and " +
                            DatabaseHelper.ColumnsMessage.C_ID + ">?",
                    new String[]{
                            String.valueOf(conversationId),
                            String.valueOf(localMessageId)
                    },
                    null,
                    null,
                    DatabaseHelper.ColumnsMessage.C_ID + " asc",
                    "0," + (offset + 1) // 此处多查询一条用来计算 hasMore
            );

            for (; cursor.moveToNext(); ) {
                Message item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                items.add(item);
            }
        } catch (Throwable e) {
            Timber.e(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        page.total = count(sessionUserId, conversationId);
        page.hasMore = items.size() > offset;
        if (page.hasMore) {
            page.data = items.subList(0, offset);
        } else {
            page.data = items;
        }

        if (DEBUG) {
            Timber.v("found old messages %s for session user id:%s, conversation id:%s, local message id:%s, offset:%s",
                    page, sessionUserId, conversationId, localMessageId, offset);
        }
        return page;
    }

    /**
     * 获取更旧的图片或者视频消息，查询比目标消息 id 更小的消息。查询结果按照消息 id 从小到大排列.
     *
     * @param sessionUserId
     * @param conversationId 会话表自增 id
     * @param localMessageId 消息表自增 id, 查询结果不包含此条消息
     * @param offset         本次查询的最大数量
     * @return
     */
    @NonNull
    public Page<Message> getOlderMessagesWithTypeImageOrVideo(final long sessionUserId, final long conversationId, final long localMessageId, final int offset, @Nullable ColumnsSelector<Message> columnsSelector) {
        if (columnsSelector == null) {
            columnsSelector = COLUMNS_SELECTOR_FULL;
        }
        final Page<Message> page = new Page<>();
        page.total = 0;
        page.hasMore = false;
        page.data = null;

        final List<Message> items = new ArrayList<>();
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_MESSAGE,
                    columnsSelector.queryColumns(),
                    DatabaseHelper.ColumnsMessage.C_CONVERSATION_ID + "=? and " +
                            DatabaseHelper.ColumnsMessage.C_ID + "<? and (" +
                            DatabaseHelper.ColumnsMessage.C_MSG_TYPE + "=? or " +
                            DatabaseHelper.ColumnsMessage.C_MSG_TYPE + "=?)",
                    new String[]{
                            String.valueOf(conversationId),
                            String.valueOf(localMessageId),
                            String.valueOf(ImConstant.MessageType.MESSAGE_TYPE_IMAGE),
                            String.valueOf(ImConstant.MessageType.MESSAGE_TYPE_VIDEO)
                    },
                    null,
                    null,
                    DatabaseHelper.ColumnsMessage.C_ID + " desc",
                    "0," + (offset + 1) // 此处多查询一条用来计算 hasMore
            );

            for (cursor.moveToLast(); !cursor.isBeforeFirst(); cursor.moveToPrevious()) {
                Message item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                items.add(item);
            }
        } catch (Throwable e) {
            Timber.e(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        page.total = count(sessionUserId, conversationId);
        page.hasMore = items.size() > offset;
        if (page.hasMore) {
            page.data = items.subList(1, offset + 1);
        } else {
            page.data = items;
        }

        if (DEBUG) {
            Timber.v("found old messages %s for session user id:%s, conversation id:%s, local message id:%s, offset:%s",
                    page, sessionUserId, conversationId, localMessageId, offset);
        }
        return page;
    }

    /**
     * 获取更新的图片或者视频消息，查询比目标消息 id 更大的消息。查询结果按照消息 id 从小到大排列.
     *
     * @param sessionUserId
     * @param conversationId 会话表自增 id
     * @param localMessageId 消息表自增 id, 查询结果不包含此条消息
     * @param offset         本次查询的最大数量
     * @return
     */
    @NonNull
    public Page<Message> getNewerMessagesWithTypeImageOrVideo(final long sessionUserId, final long conversationId, final long localMessageId, final int offset, @Nullable ColumnsSelector<Message> columnsSelector) {
        if (columnsSelector == null) {
            columnsSelector = COLUMNS_SELECTOR_FULL;
        }
        final Page<Message> page = new Page<>();
        page.total = 0;
        page.hasMore = false;
        page.data = null;

        final List<Message> items = new ArrayList<>();
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_MESSAGE,
                    columnsSelector.queryColumns(),
                    DatabaseHelper.ColumnsMessage.C_CONVERSATION_ID + "=? and " +
                            DatabaseHelper.ColumnsMessage.C_ID + ">? and (" +
                            DatabaseHelper.ColumnsMessage.C_MSG_TYPE + "=? or " +
                            DatabaseHelper.ColumnsMessage.C_MSG_TYPE + "=?)",
                    new String[]{
                            String.valueOf(conversationId),
                            String.valueOf(localMessageId),
                            String.valueOf(ImConstant.MessageType.MESSAGE_TYPE_IMAGE),
                            String.valueOf(ImConstant.MessageType.MESSAGE_TYPE_VIDEO)
                    },
                    null,
                    null,
                    DatabaseHelper.ColumnsMessage.C_ID + " asc",
                    "0," + (offset + 1) // 此处多查询一条用来计算 hasMore
            );

            for (; cursor.moveToNext(); ) {
                Message item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                items.add(item);
            }
        } catch (Throwable e) {
            Timber.e(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        page.total = count(sessionUserId, conversationId);
        page.hasMore = items.size() > offset;
        if (page.hasMore) {
            page.data = items.subList(0, offset);
        } else {
            page.data = items;
        }

        if (DEBUG) {
            Timber.v("getNewerMessagesWithTypeImageOrVideo %s for session user id:%s, conversation id:%s, local message id:%s, offset:%s",
                    page, sessionUserId, conversationId, localMessageId, offset);
        }
        return page;
    }

    /**
     * 获取更旧的指定 messageType, fromUserId 的消息，查询比目标消息 id 更小的消息。查询结果按照消息 id 从小到大排列.
     *
     * @param sessionUserId
     * @param conversationId 会话表自增 id
     * @param localMessageId 消息表自增 id, 查询结果不包含此条消息. -1 表示从最新一条消息查询.
     * @param messageType    查询指定的 messageType
     * @param fromUserId     查询指定的 fromUserId
     * @param offset         本次查询的最大数量
     * @return
     */
    @NonNull
    public Page<Message> getOlderMessagesWithTypeAndFromUserId(final long sessionUserId,
                                                               final long conversationId,
                                                               final long localMessageId,
                                                               final int messageType,
                                                               final long fromUserId,
                                                               final int offset,
                                                               @Nullable ColumnsSelector<Message> columnsSelector) {
        if (columnsSelector == null) {
            columnsSelector = COLUMNS_SELECTOR_FULL;
        }
        final Page<Message> page = new Page<>();
        page.total = 0;
        page.hasMore = false;
        page.data = null;

        final List<Message> items = new ArrayList<>();
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            if (localMessageId <= 0) {
                cursor = db.query(
                        DatabaseHelper.TABLE_NAME_MESSAGE,
                        columnsSelector.queryColumns(),
                        DatabaseHelper.ColumnsMessage.C_CONVERSATION_ID + "=? and " +
                                DatabaseHelper.ColumnsMessage.C_MSG_TYPE + "=? and " +
                                DatabaseHelper.ColumnsMessage.C_FROM_USER_ID + "=?",
                        new String[]{
                                String.valueOf(conversationId),
                                String.valueOf(messageType),
                                String.valueOf(fromUserId)
                        },
                        null,
                        null,
                        DatabaseHelper.ColumnsMessage.C_ID + " desc",
                        "0," + (offset + 1) // 此处多查询一条用来计算 hasMore
                );
            } else {
                cursor = db.query(
                        DatabaseHelper.TABLE_NAME_MESSAGE,
                        columnsSelector.queryColumns(),
                        DatabaseHelper.ColumnsMessage.C_CONVERSATION_ID + "=? and " +
                                DatabaseHelper.ColumnsMessage.C_ID + "<? and " +
                                DatabaseHelper.ColumnsMessage.C_MSG_TYPE + "=? and " +
                                DatabaseHelper.ColumnsMessage.C_FROM_USER_ID + "=?",
                        new String[]{
                                String.valueOf(conversationId),
                                String.valueOf(localMessageId),
                                String.valueOf(messageType),
                                String.valueOf(fromUserId)
                        },
                        null,
                        null,
                        DatabaseHelper.ColumnsMessage.C_ID + " desc",
                        "0," + (offset + 1) // 此处多查询一条用来计算 hasMore
                );
            }

            for (cursor.moveToLast(); !cursor.isBeforeFirst(); cursor.moveToPrevious()) {
                Message item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                items.add(item);
            }
        } catch (Throwable e) {
            Timber.e(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        page.total = count(sessionUserId, conversationId);
        page.hasMore = items.size() > offset;
        if (page.hasMore) {
            page.data = items.subList(1, offset + 1);
        } else {
            page.data = items;
        }

        if (DEBUG) {
            Timber.v("found old messages %s for session user id:%s, conversation id:%s, local message id:%s, offset:%s, message type:%s, from user id:%s",
                    page, sessionUserId, conversationId, localMessageId, offset, messageType, fromUserId);
        }
        return page;
    }

    /**
     * 将数据库中所有没有发送成功的数据设置为发送失败
     *
     * @param sessionUserId
     * @return 操作成功返回 true, 否则返回 false.
     */
    public boolean forceSetMessageToFailIfNotSuccess(final long sessionUserId) {
        try {
            DatabaseHelper dbHelper = ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            ContentValues cv = new ContentValues();
            cv.put(DatabaseHelper.ColumnsMessage.C_SEND_STATUS, ImConstant.MessageSendStatus.FAIL);
            int rowsAffected = db.update(
                    DatabaseHelper.TABLE_NAME_MESSAGE,
                    cv,
                    DatabaseHelper.ColumnsMessage.C_FROM_USER_ID + "=? and "
                            + DatabaseHelper.ColumnsMessage.C_SEND_STATUS + "!=?",
                    new String[]{
                            String.valueOf(sessionUserId),
                            String.valueOf(ImConstant.MessageSendStatus.SUCCESS)
                    }
            );
            Timber.v("update message for session user id:%s force set to fail affected %s rows", sessionUserId, rowsAffected);
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
        }
        return false;
    }

    /**
     * 查询指定数量的待发送的消息，按照数据库 id 顺序排序。仅查询少量字段。
     *
     * @param sessionUserId
     * @param limit
     * @return
     */
    @NonNull
    public List<Message> getIdleMessages(final long sessionUserId, final int limit) {
        final List<Message> items = new ArrayList<>();
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_MESSAGE,
                    new String[]{
                            DatabaseHelper.ColumnsMessage.C_ID,
                            DatabaseHelper.ColumnsMessage.C_MSG_TYPE
                    },
                    DatabaseHelper.ColumnsMessage.C_FROM_USER_ID + "=? and " +
                            DatabaseHelper.ColumnsMessage.C_SEND_STATUS + "=?",
                    new String[]{
                            String.valueOf(sessionUserId),
                            String.valueOf(ImConstant.MessageSendStatus.IDLE)
                    },
                    null,
                    null,
                    DatabaseHelper.ColumnsMessage.C_ID + " asc",
                    "0," + limit
            );

            for (; cursor.moveToNext(); ) {
                Message item = new Message();
                item.id = cursor.getLong(0);
                item.msgType = cursor.getInt(1);
                items.add(item);
            }
        } catch (Throwable e) {
            Timber.e(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }
        return items;
    }

    @Nullable
    public Message getTargetMessage(final long sessionUserId, final long localMessageId, @Nullable ColumnsSelector<Message> columnsSelector) {
        if (columnsSelector == null) {
            columnsSelector = COLUMNS_SELECTOR_FULL;
        }
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_MESSAGE,
                    columnsSelector.queryColumns(),
                    DatabaseHelper.ColumnsMessage.C_ID + "=?",
                    new String[]{String.valueOf(localMessageId)},
                    null,
                    null,
                    null,
                    "0,1"
            );

            if (cursor.moveToNext()) {
                Message item = columnsSelector.cursorToObjectWithQueryColumns(cursor);

                Timber.v("found message with local message id:%s for session user id:%s", localMessageId, sessionUserId);
                return item;
            }
        } catch (Throwable e) {
            Timber.e(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        Timber.v("message not found with local message id:%s for session user id:%s", localMessageId, sessionUserId);
        return null;
    }

    @Nullable
    public Message getTargetMessageByServerMessageId(final long sessionUserId, final long serverMessageId, @Nullable ColumnsSelector<Message> columnsSelector) {
        if (columnsSelector == null) {
            columnsSelector = COLUMNS_SELECTOR_FULL;
        }
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_MESSAGE,
                    columnsSelector.queryColumns(),
                    DatabaseHelper.ColumnsMessage.C_MSG_ID + "=?",
                    new String[]{String.valueOf(serverMessageId)},
                    null,
                    null,
                    null,
                    "0,1"
            );

            if (cursor.moveToNext()) {
                Message item = columnsSelector.cursorToObjectWithQueryColumns(cursor);

                Timber.v("found message with server message id:%s for session user id:%s", serverMessageId, sessionUserId);
                return item;
            }
        } catch (Throwable e) {
            Timber.e(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        Timber.v("message not found with server message id:%s for session user id:%s", serverMessageId, sessionUserId);
        return null;
    }

    /**
     * 插入一条新消息数据
     *
     * @param sessionUserId
     * @param message
     * @return 插入成功返回 true, 否则返回 false.
     */
    public boolean insertMessage(final long sessionUserId, final Message message) {
        if (message == null) {
            Throwable e = new IllegalArgumentException("message is null");
            Timber.e(e);
            return false;
        }

        if (message.id > 0) {
            Throwable e = new IllegalArgumentException("invalid message id " + message.id + ", you may need use update instead of insert");
            Timber.e(e);
            return false;
        }

        if (message.conversationId <= 0) {
            Throwable e = new IllegalArgumentException("invalid message conversation id " + message.conversationId);
            Timber.e(e);
            return false;
        }

        try {
            DatabaseHelper dbHelper = ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            long rowId = db.insert(
                    DatabaseHelper.TABLE_NAME_MESSAGE,
                    null,
                    message.toContentValues());
            if (rowId == -1) {
                Throwable e = new IllegalAccessException("insert message for session user id:" + sessionUserId + " with conversation id:" + message.conversationId + " fail");
                Timber.e(e);
                return false;
            }

            // 自增主键
            message.id = rowId;

            return true;
        } catch (Throwable e) {
            Timber.e(e);
        }
        return false;
    }

    /**
     * @param message
     * @return 更新成功返回 true, 否则返回 false.
     */
    public boolean updateMessage(final long sessionUserId, final Message message) {
        if (message == null) {
            Throwable e = new IllegalArgumentException("message is null");
            Timber.e(e);
            return false;
        }

        if (message.id <= 0) {
            Throwable e = new IllegalArgumentException("invalid message id " + message.id);
            Timber.e(e);
            return false;
        }

        if (message.conversationId <= 0) {
            Throwable e = new IllegalArgumentException("invalid message conversation id " + message.conversationId);
            Timber.e(e);
            return false;
        }

        try {
            DatabaseHelper dbHelper = ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            int rowsAffected = db.update(
                    DatabaseHelper.TABLE_NAME_MESSAGE,
                    message.toContentValues(),
                    DatabaseHelper.ColumnsMessage.C_ID + "=?",
                    new String[]{String.valueOf(message.id)}
            );
            if (rowsAffected != 1) {
                Throwable e = new IllegalAccessException("update message for session user id:" + sessionUserId + " with id:" + message.id + " affected " + rowsAffected + " rows");
                Timber.e(e);
            } else {
                Timber.v("update message for session user id:%s with id:%s affected %s rows", sessionUserId, message.id, rowsAffected);
            }
            return rowsAffected > 0;
        } catch (Throwable e) {
            Timber.e(e);
        }
        return false;
    }

    public boolean deleteMessage(final long sessionUserId, final Message message) {
        if (message == null) {
            Throwable e = new IllegalArgumentException("message is null");
            Timber.e(e);
            return false;
        }

        if (message.id <= 0) {
            Throwable e = new IllegalArgumentException("invalid message id " + message.id);
            Timber.e(e);
            return false;
        }

        try {
            DatabaseHelper dbHelper = ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            int rowsAffected = db.delete(
                    DatabaseHelper.TABLE_NAME_MESSAGE,
                    DatabaseHelper.ColumnsMessage.C_ID + "=?",
                    new String[]{String.valueOf(message.id)}
            );
            if (rowsAffected != 1) {
                Throwable e = new IllegalAccessException("delete message for session user id:" + sessionUserId + " with id:" + message.id + " affected " + rowsAffected + " rows");
                Timber.e(e);
            } else {
                Timber.v("delete message for session user id:%s with id:%s affected %s rows", sessionUserId, message.id, rowsAffected);
            }
            return rowsAffected > 0;
        } catch (Throwable e) {
            Timber.e(e);
        }
        return false;
    }

    @NonNull
    public List<Long> queryLatestServerMessageIds(final long sessionUserId, final long conversationId) {
        List<Long> serverMessageIds = new ArrayList<>();
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            cursor = db.query(DatabaseHelper.TABLE_NAME_MESSAGE,
                    new String[]{DatabaseHelper.ColumnsMessage.C_MSG_ID},
                    DatabaseHelper.ColumnsMessage.C_MSG_ID + ">0 and "
                            + DatabaseHelper.ColumnsMessage.C_CONVERSATION_ID + "=?",
                    new String[]{String.valueOf(conversationId)},
                    null,
                    null,
                    DatabaseHelper.ColumnsMessage.C_ID + " desc",
                    "0," + ImConstant.MAX_DELETE_MESSAGE_COUNT_SYNC_WITH_SERVER_PER_CONVERSATION);
            while (cursor.moveToNext()) {
                serverMessageIds.add(cursor.getLong(0));
            }
        } catch (Throwable e) {
            Timber.e(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }
        return serverMessageIds;
    }

    @Nullable
    public Long getServerMessageId(final long sessionUserId, final long localMessageId) {
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            cursor = db.query(DatabaseHelper.TABLE_NAME_MESSAGE,
                    new String[]{DatabaseHelper.ColumnsMessage.C_MSG_ID},
                    DatabaseHelper.ColumnsMessage.C_ID + "=?",
                    new String[]{String.valueOf(localMessageId)},
                    null,
                    null,
                    null,
                    "0,1");
            if (cursor.moveToNext()) {
                return cursor.getLong(0);
            }
        } catch (Throwable e) {
            Timber.e(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }
        return null;
    }

    public boolean deleteAllMessage(final long sessionUserId, final long conversationId) {
        try {
            DatabaseHelper dbHelper = ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            int rowsAffected = db.delete(
                    DatabaseHelper.TABLE_NAME_MESSAGE,
                    DatabaseHelper.ColumnsMessage.C_CONVERSATION_ID + "=?",
                    new String[]{String.valueOf(conversationId)}
            );
            Timber.v("delete all message for session user id:%s, conversation id:%s, affected %s rows", sessionUserId, conversationId, rowsAffected);
            return true;
        } catch (Throwable e) {
            Timber.e(e);
        }
        return false;
    }

    public static final ColumnsSelector<Message> COLUMNS_SELECTOR_FULL = new ColumnsSelector<Message>() {
        @Override
        public String[] queryColumns() {
            return new String[]{
                    DatabaseHelper.ColumnsMessage.C_ID,
                    DatabaseHelper.ColumnsMessage.C_CONVERSATION_ID,
                    DatabaseHelper.ColumnsMessage.C_MSG_ID,
                    DatabaseHelper.ColumnsMessage.C_MSG_SERVER_TIME,
                    DatabaseHelper.ColumnsMessage.C_FROM_USER_ID,
                    DatabaseHelper.ColumnsMessage.C_TO_USER_ID,
                    DatabaseHelper.ColumnsMessage.C_MSG_LOCAL_TIME,
                    DatabaseHelper.ColumnsMessage.C_MSG_TYPE,
                    DatabaseHelper.ColumnsMessage.C_MSG_TEXT,
                    DatabaseHelper.ColumnsMessage.C_MSG_TITLE,
                    DatabaseHelper.ColumnsMessage.C_MSG_IMAGE_SERVER_THUMB,
                    DatabaseHelper.ColumnsMessage.C_MSG_IMAGE_SERVER_URL,
                    DatabaseHelper.ColumnsMessage.C_MSG_IMAGE_LOCAL_URL,
                    DatabaseHelper.ColumnsMessage.C_MSG_IMAGE_WIDTH,
                    DatabaseHelper.ColumnsMessage.C_MSG_IMAGE_HEIGHT,
                    DatabaseHelper.ColumnsMessage.C_MSG_IMAGE_FILE_SIZE,
                    DatabaseHelper.ColumnsMessage.C_MSG_VOICE_SERVER_URL,
                    DatabaseHelper.ColumnsMessage.C_MSG_VOICE_LOCAL_URL,
                    DatabaseHelper.ColumnsMessage.C_MSG_VOICE_DURATION,
                    DatabaseHelper.ColumnsMessage.C_MSG_VOICE_FILE_SIZE,
                    DatabaseHelper.ColumnsMessage.C_MSG_VIDEO_SERVER_THUMB,
                    DatabaseHelper.ColumnsMessage.C_MSG_VIDEO_SERVER_URL,
                    DatabaseHelper.ColumnsMessage.C_MSG_VIDEO_LOCAL_URL,
                    DatabaseHelper.ColumnsMessage.C_MSG_VIDEO_WIDTH,
                    DatabaseHelper.ColumnsMessage.C_MSG_VIDEO_HEIGHT,
                    DatabaseHelper.ColumnsMessage.C_MSG_VIDEO_DURATION,
                    DatabaseHelper.ColumnsMessage.C_MSG_VIDEO_FILE_SIZE,
                    DatabaseHelper.ColumnsMessage.C_MSG_LOCATION_TITLE,
                    DatabaseHelper.ColumnsMessage.C_MSG_LOCATION_LAT,
                    DatabaseHelper.ColumnsMessage.C_MSG_LOCATION_LNG,
                    DatabaseHelper.ColumnsMessage.C_MSG_LOCATION_ZOOM,
                    DatabaseHelper.ColumnsMessage.C_MSG_LOCATION_ADDRESS,
                    DatabaseHelper.ColumnsMessage.C_MSG_UGC_ID,
                    DatabaseHelper.ColumnsMessage.C_MSG_UGC_USER_ID,
                    DatabaseHelper.ColumnsMessage.C_MSG_UGC_SERVER_THUMB,
                    DatabaseHelper.ColumnsMessage.C_MSG_UGC_NICE_NUM,
                    DatabaseHelper.ColumnsMessage.C_MSG_FROM_USER_ID,
                    DatabaseHelper.ColumnsMessage.C_MSG_NUMBER,
                    DatabaseHelper.ColumnsMessage.C_MSG_URL,
                    DatabaseHelper.ColumnsMessage.C_MSG_SUBJECT,
                    DatabaseHelper.ColumnsMessage.C_MSG_MSGS,
                    DatabaseHelper.ColumnsMessage.C_MSG_GIFT_ID,
                    DatabaseHelper.ColumnsMessage.C_MSG_GIFT_NAME,
                    DatabaseHelper.ColumnsMessage.C_MSG_GIFT_DESC,
                    DatabaseHelper.ColumnsMessage.C_MSG_GIFT_K_PRICE,
                    DatabaseHelper.ColumnsMessage.C_MSG_GIFT_COVER,
                    DatabaseHelper.ColumnsMessage.C_MSG_GIFT_ANIM,
                    DatabaseHelper.ColumnsMessage.C_SEND_STATUS,
                    DatabaseHelper.ColumnsMessage.C_READ_STATUS,
                    DatabaseHelper.ColumnsMessage.C_REVERT_STATUS
            };
        }

        @Override
        public Message cursorToObjectWithQueryColumns(Cursor cursor) {
            Message object = new Message();
            int index = -1;
            object.id = cursor.getLong(++index);
            object.conversationId = cursor.getLong(++index);
            object.msgId = cursor.getLong(++index);
            object.msgServerTime = cursor.getLong(++index);
            object.fromUserId = cursor.getLong(++index);
            object.toUserId = cursor.getLong(++index);
            object.msgLocalTime = cursor.getLong(++index);
            object.msgType = cursor.getInt(++index);
            object.msgText = cursor.getString(++index);
            object.msgTitle = cursor.getString(++index);
            object.msgImageServerThumb = cursor.getString(++index);
            object.msgImageServerUrl = cursor.getString(++index);
            object.msgImageLocalUrl = cursor.getString(++index);
            object.msgImageWidth = cursor.getInt(++index);
            object.msgImageHeight = cursor.getInt(++index);
            object.msgImageFileSize = cursor.getLong(++index);
            object.msgVoiceServerUrl = cursor.getString(++index);
            object.msgVoiceLocalUrl = cursor.getString(++index);
            object.msgVoiceDuration = cursor.getLong(++index);
            object.msgVoiceFileSize = cursor.getLong(++index);
            object.msgVideoServerThumb = cursor.getString(++index);
            object.msgVideoServerUrl = cursor.getString(++index);
            object.msgVideoLocalUrl = cursor.getString(++index);
            object.msgVideoWidth = cursor.getInt(++index);
            object.msgVideoHeight = cursor.getInt(++index);
            object.msgVideoDuration = cursor.getLong(++index);
            object.msgVideoFileSize = cursor.getLong(++index);
            object.msgLocationTitle = cursor.getString(++index);
            object.msgLocationLat = cursor.getString(++index);
            object.msgLocationLng = cursor.getString(++index);
            object.msgLocationZoom = cursor.getInt(++index);
            object.msgLocationAddress = cursor.getString(++index);
            object.msgUgcId = cursor.getLong(++index);
            object.msgUgcUserId = cursor.getLong(++index);
            object.msgUgcServerThumb = cursor.getString(++index);
            object.msgUgcNiceNum = cursor.getLong(++index);
            object.msgFromUserId = cursor.getLong(++index);
            object.msgNumber = cursor.getLong(++index);
            object.msgUrl = cursor.getString(++index);
            object.msgSubject = cursor.getString(++index);
            object.msgMsgs = DataFormat.jsonToArrayListString(cursor.getString(++index));
            object.msgGiftId = cursor.getLong(++index);
            object.msgGiftName = cursor.getString(++index);
            object.msgGiftDesc = cursor.getString(++index);
            object.msgGiftKPrice = cursor.getLong(++index);
            object.msgGiftCover = cursor.getString(++index);
            object.msgGiftAnim = cursor.getString(++index);
            object.sendStatus = cursor.getInt(++index);
            object.readStatus = cursor.getInt(++index);
            object.revertStatus = cursor.getInt(++index);
            return object;
        }

    };


}
