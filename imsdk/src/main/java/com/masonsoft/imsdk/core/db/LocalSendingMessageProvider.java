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

    private LocalSendingMessageProvider() {
    }

    /**
     * 查询结果按照 localSendingMessageLocalId 从小到大排列
     *
     * @param localSendingMessageLocalId 不包括这一条，初始传 0
     */
    @NonNull
    public TinyPage<LocalSendingMessage> pageQueryLocalSendingMessage(
            final long sessionUserId,
            final long localSendingMessageLocalId,
            final int limit,
            @Nullable ColumnsSelector<LocalSendingMessage> columnsSelector) {
        if (columnsSelector == null) {
            columnsSelector = LocalSendingMessage.COLUMNS_SELECTOR_ALL;
        }

        final List<LocalSendingMessage> items = new ArrayList<>();
        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            final StringBuilder selection = new StringBuilder();
            final List<String> selectionArgs = new ArrayList<>();

            if (localSendingMessageLocalId > 0) {
                selection.append(" " + DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_ID + ">? ");
                selectionArgs.add(String.valueOf(localSendingMessageLocalId));
            }

            cursor = db.query(
                    DatabaseHelper.TABLE_NAME_LOCAL_SENDING_MESSAGE,
                    columnsSelector.queryColumns(),
                    selection.toString(),
                    selectionArgs.toArray(new String[]{}),
                    null,
                    null,
                    DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_ID + " asc",
                    String.valueOf(limit + 1) // 此处多查询一条用来计算 hasMore
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

        final TinyPage<LocalSendingMessage> result = new TinyPage<>();
        result.hasMore = items.size() > limit;
        if (result.hasMore) {
            result.items = new ArrayList<>(items.subList(0, limit));
        } else {
            result.items = items;
        }

        IMLog.v("found %s localSendingMessage[hasMore:%s] with sessionUserId:%s, localSendingMessageLocalId:%s, limit:%s",
                result.items.size(), result.hasMore, sessionUserId, localSendingMessageLocalId, limit);
        return result;
    }

    /**
     * @return 没有找到返回 null
     */
    @Nullable
    public LocalSendingMessage getLocalSendingMessage(
            final long sessionUserId,
            final long localSendingMessageLocalId,
            @Nullable ColumnsSelector<LocalSendingMessage> columnsSelector) {
        if (columnsSelector == null) {
            columnsSelector = LocalSendingMessage.COLUMNS_SELECTOR_ALL;
        }

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
            final long messageLocalId,
            @Nullable ColumnsSelector<LocalSendingMessage> columnsSelector) {
        IMConstants.ConversationType.check(conversationType);

        if (columnsSelector == null) {
            columnsSelector = LocalSendingMessage.COLUMNS_SELECTOR_ALL;
        }

        Cursor cursor = null;
        try {
            DatabaseHelper dbHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            final StringBuilder selection = new StringBuilder();
            final List<String> selectionArgs = new ArrayList<>();

            selection.append(" " + DatabaseHelper.ColumnsLocalSendingMessage.C_CONVERSATION_TYPE + "=? ");
            selectionArgs.add(String.valueOf(conversationType));

            selection.append(" " + DatabaseHelper.ColumnsLocalSendingMessage.C_TARGET_USER_ID + "=? ");
            selectionArgs.add(String.valueOf(targetUserId));

            selection.append(" " + DatabaseHelper.ColumnsLocalSendingMessage.C_MESSAGE_LOCAL_ID + "=? ");
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
            return true;
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
            return rowsAffected > 0;
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        }
        return false;
    }

}
