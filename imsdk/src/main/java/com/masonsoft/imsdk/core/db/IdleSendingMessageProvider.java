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
 * 将所有消息表(按照会话分表)中待发送与发送中的消息记录一个统一的副本。相当于一个持久化的发送队列。
 *
 * @since 1.0
 */
public class IdleSendingMessageProvider {

    private static final Singleton<IdleSendingMessageProvider> INSTANCE = new Singleton<IdleSendingMessageProvider>() {
        @Override
        protected IdleSendingMessageProvider create() {
            return new IdleSendingMessageProvider();
        }
    };

    public static IdleSendingMessageProvider getInstance() {
        IMProcessValidator.validateProcess();

        return INSTANCE.get();
    }

    private IdleSendingMessageProvider() {
    }

    /**
     * 查询结果按照 localId 从小到大排列
     *
     * @param idleSendingMessageLocalId 不包括这一条，初始传 0
     */
    @NonNull
    public TinyPage<IdleSendingMessage> pageQueryIdleSendingMessage(
            final long sessionUserId,
            final long idleSendingMessageLocalId,
            final int limit,
            @Nullable ColumnsSelector<IdleSendingMessage> columnsSelector) {
        if (columnsSelector == null) {
            columnsSelector = IdleSendingMessage.COLUMNS_SELECTOR_ALL;
        }

        final List<IdleSendingMessage> items = new ArrayList<>();
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            final StringBuilder selection = new StringBuilder();
            final List<String> selectionArgs = new ArrayList<>();

            if (idleSendingMessageLocalId > 0) {
                selection.append(" " + DatabaseHelper.ColumnsIdleSendingMessage.C_LOCAL_ID + ">? ");
                selectionArgs.add(String.valueOf(idleSendingMessageLocalId));
            }

            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_IDLE_SENDING_MESSAGE,
                    columnsSelector.queryColumns(),
                    selection.toString(),
                    selectionArgs.toArray(new String[]{}),
                    null,
                    null,
                    DatabaseHelper.ColumnsIdleSendingMessage.C_LOCAL_ID + " asc",
                    String.valueOf(limit + 1) // 此处多查询一条用来计算 hasMore
            );

            while (cursor.moveToNext()) {
                IdleSendingMessage item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                items.add(item);
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        final TinyPage<IdleSendingMessage> result = new TinyPage<>();
        result.hasMore = items.size() > limit;
        if (result.hasMore) {
            result.items = new ArrayList<>(items.subList(0, limit));
        } else {
            result.items = items;
        }

        IMLog.v("found %s idleSendingMessage[hasMore:%s] with sessionUserId:%s, idleSendingMessageLocalId:%s, limit:%s",
                result.items.size(), result.hasMore, sessionUserId, idleSendingMessageLocalId, limit);
        return result;
    }

    /**
     * @return 没有找到返回 null
     */
    @Nullable
    public IdleSendingMessage getIdleSendingMessage(
            final long sessionUserId,
            final long idleSendingMessageLocalId,
            @Nullable ColumnsSelector<IdleSendingMessage> columnsSelector) {
        if (columnsSelector == null) {
            columnsSelector = IdleSendingMessage.COLUMNS_SELECTOR_ALL;
        }

        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            //noinspection StringBufferReplaceableByString
            final StringBuilder selection = new StringBuilder();
            final List<String> selectionArgs = new ArrayList<>();

            selection.append(" " + DatabaseHelper.ColumnsIdleSendingMessage.C_LOCAL_ID + "=? ");
            selectionArgs.add(String.valueOf(idleSendingMessageLocalId));

            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_IDLE_SENDING_MESSAGE,
                    columnsSelector.queryColumns(),
                    selection.toString(),
                    selectionArgs.toArray(new String[]{}),
                    null,
                    null,
                    null,
                    "1"
            );

            if (cursor.moveToNext()) {
                final IdleSendingMessage result = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                IMLog.v("idleSendingMessage found with sessionUserId:%s, idleSendingMessageLocalId:%s",
                        sessionUserId, idleSendingMessageLocalId);
                return result;
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        // idleSendingMessage not found
        IMLog.v("idleSendingMessage not found with sessionUserId:%s, idleSendingMessageLocalId:%s",
                sessionUserId, idleSendingMessageLocalId);
        return null;
    }

    /**
     * @return 没有找到返回 null
     */
    @Nullable
    public IdleSendingMessage getIdleSendingMessageByTargetMessage(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId,
            final long messageLocalId,
            @Nullable ColumnsSelector<IdleSendingMessage> columnsSelector) {
        IMConstants.ConversationType.check(conversationType);

        if (columnsSelector == null) {
            columnsSelector = IdleSendingMessage.COLUMNS_SELECTOR_ALL;
        }

        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            final StringBuilder selection = new StringBuilder();
            final List<String> selectionArgs = new ArrayList<>();

            selection.append(" " + DatabaseHelper.ColumnsIdleSendingMessage.C_CONVERSATION_TYPE + "=? ");
            selectionArgs.add(String.valueOf(conversationType));

            selection.append(" " + DatabaseHelper.ColumnsIdleSendingMessage.C_TARGET_USER_ID + "=? ");
            selectionArgs.add(String.valueOf(targetUserId));

            selection.append(" " + DatabaseHelper.ColumnsIdleSendingMessage.C_MESSAGE_LOCAL_ID + "=? ");
            selectionArgs.add(String.valueOf(messageLocalId));

            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_IDLE_SENDING_MESSAGE,
                    columnsSelector.queryColumns(),
                    selection.toString(),
                    selectionArgs.toArray(new String[]{}),
                    null,
                    null,
                    null,
                    "1"
            );

            if (cursor.moveToNext()) {
                final IdleSendingMessage result = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                IMLog.v("idleSendingMessage found with sessionUserId:%s, conversationType:%s, targetUserId:%s, messageLocalId:%s",
                        sessionUserId,
                        conversationType,
                        targetUserId,
                        messageLocalId);
                return result;
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        // idleSendingMessage not found
        IMLog.v("idleSendingMessage not found with sessionUserId:%s, conversationType:%s, targetUserId:%s, messageLocalId:%s",
                sessionUserId,
                conversationType,
                targetUserId,
                messageLocalId);
        return null;
    }

    /**
     * 插入一条 IdleSendingMessage 。插入成功时，会自动设置 IdleSendingMessage 的 localId
     *
     * @return 插入成功返回 true, 否则返回 false.
     */
    public boolean insertIdleSendingMessage(
            final long sessionUserId,
            final IdleSendingMessage idleSendingMessage) {
        if (idleSendingMessage == null) {
            IMLog.e(new IllegalArgumentException("idleSendingMessage is null"));
            return false;
        }

        if (!idleSendingMessage.localId.isUnset()) {
            IMLog.e(
                    new IllegalArgumentException("invalid idleSendingMessage localId"),
                    "idleSendingMessage localId:%s, you cat not set localId by yourself for insert",
                    idleSendingMessage.localId.get()
            );
            return false;
        }

        if (idleSendingMessage.conversationType.isUnset()) {
            IMLog.e(
                    new IllegalArgumentException("invalid idleSendingMessage conversationType"),
                    "idleSendingMessage conversationType is unset"
            );
            return false;
        }
        IMConstants.ConversationType.check(idleSendingMessage.conversationType.get());

        if (idleSendingMessage.targetUserId.isUnset()) {
            IMLog.e(
                    new IllegalArgumentException("invalid idleSendingMessage targetUserId"),
                    "idleSendingMessage targetUserId is unset"
            );
            return false;
        }

        if (idleSendingMessage.messageLocalId.isUnset()) {
            IMLog.e(
                    new IllegalArgumentException("invalid idleSendingMessage messageLocalId"),
                    "idleSendingMessage messageLocalId is unset"
            );
            return false;
        }

        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            long rowId = db.insert(
                    DatabaseHelper.TABLE_NAME_IDLE_SENDING_MESSAGE,
                    null,
                    idleSendingMessage.toContentValues());
            if (rowId == -1) {
                IMLog.e(
                        new IllegalAccessError("insert idleSendingMessage fail"),
                        "fail to insert idleSendingMessage with sessionUserId:%s, conversationType:%s, targetUserId:%s, messageLocalId:%s",
                        sessionUserId,
                        idleSendingMessage.conversationType.get(),
                        idleSendingMessage.targetUserId.get(),
                        idleSendingMessage.messageLocalId.get()
                );
                return false;
            }

            // 自增主键
            idleSendingMessage.localId.set(rowId);
            return true;
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
    public boolean removeIdleSendingMessage(
            final long sessionUserId,
            final long localId) {

        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            //noinspection StringBufferReplaceableByString
            final StringBuilder where = new StringBuilder();
            final List<String> whereArgs = new ArrayList<>();

            where.append(" " + DatabaseHelper.ColumnsIdleSendingMessage.C_LOCAL_ID + "=? ");
            whereArgs.add(String.valueOf(localId));

            int rowsAffected = db.delete(
                    DatabaseHelper.TABLE_NAME_IDLE_SENDING_MESSAGE,
                    where.toString(),
                    whereArgs.toArray(new String[]{})
            );
            if (rowsAffected != 1) {
                IMLog.e(
                        new IllegalAccessException("remove idleSendingMessage fail"),
                        "unexpected. remove idleSendingMessage with sessionUserId:% idleSendingMessage localId:%s affected %s rows",
                        sessionUserId,
                        localId,
                        rowsAffected
                );
            } else {
                IMLog.v("remove idleSendingMessage with sessionUserId:%s idleSendingMessage localId:%s affected %s rows",
                        sessionUserId,
                        localId,
                        rowsAffected);
            }
            return rowsAffected > 0;
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        }
        return false;
    }

}
