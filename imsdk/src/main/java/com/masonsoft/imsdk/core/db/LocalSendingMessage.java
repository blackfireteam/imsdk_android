package com.masonsoft.imsdk.core.db;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.lang.StateProp;
import com.masonsoft.imsdk.util.CursorUtil;
import com.masonsoft.imsdk.util.Objects;

/**
 * 本地消息发送队列表。
 * 记录所有本地未发送成功的消息。包括待发送，发送中与发送失败的消息。当消息发送成功之后会从该表中移除记录。
 *
 * @since 1.0
 */
public class LocalSendingMessage {

    /**
     * @see DatabaseHelper.ColumnsLocalSendingMessage#C_LOCAL_ID
     */
    @NonNull
    public final StateProp<Long> localId = new StateProp<>();

    /**
     * @see DatabaseHelper.ColumnsLocalSendingMessage#C_CONVERSATION_TYPE
     */
    @NonNull
    public final StateProp<Integer> conversationType = new StateProp<>();

    /**
     * @see DatabaseHelper.ColumnsLocalSendingMessage#C_TARGET_USER_ID
     */
    @NonNull
    public final StateProp<Long> targetUserId = new StateProp<>();

    /**
     * @see DatabaseHelper.ColumnsLocalSendingMessage#C_MESSAGE_LOCAL_ID
     */
    @NonNull
    public final StateProp<Long> messageLocalId = new StateProp<>();

    /**
     * @see DatabaseHelper.ColumnsLocalSendingMessage#C_LOCAL_SEND_STATUS
     */
    @NonNull
    public final StateProp<Integer> localSendStatus = new StateProp<>();

    /**
     * @see DatabaseHelper.ColumnsLocalSendingMessage#C_LOCAL_ABORT_ID
     */
    public final StateProp<Long> localAbortId = new StateProp<>();

    @NonNull
    public String toShortString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(Objects.defaultObjectTag(this));
        if (this.localId.isUnset()) {
            builder.append(" localId:unset");
        } else {
            builder.append(" localId:").append(this.localId.get());
        }
        if (this.conversationType.isUnset()) {
            builder.append(" conversationType:unset");
        } else {
            builder.append(" conversationType:").append(this.conversationType.get());
        }
        if (this.targetUserId.isUnset()) {
            builder.append(" targetUserId:unset");
        } else {
            builder.append(" targetUserId:").append(this.targetUserId.get());
        }
        if (this.messageLocalId.isUnset()) {
            builder.append(" messageLocalId:unset");
        } else {
            builder.append(" messageLocalId:").append(this.messageLocalId.get());
        }
        if (this.localSendStatus.isUnset()) {
            builder.append(" localSendStatus:unset");
        } else {
            builder.append(" localSendStatus:").append(this.localSendStatus.get());
        }
        if (this.localAbortId.isUnset()) {
            builder.append(" localAbortId:unset");
        } else {
            builder.append(" localAbortId:").append(this.localAbortId.get());
        }
        return builder.toString();
    }

    @Override
    @NonNull
    public String toString() {
        return this.toShortString();
    }

    @NonNull
    public ContentValues toContentValues() {
        final ContentValues target = new ContentValues();
        if (!this.localId.isUnset()) {
            target.put(DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_ID, this.localId.get());
        }
        if (!this.conversationType.isUnset()) {
            target.put(DatabaseHelper.ColumnsLocalSendingMessage.C_CONVERSATION_TYPE, this.conversationType.get());
        }
        if (!this.targetUserId.isUnset()) {
            target.put(DatabaseHelper.ColumnsLocalSendingMessage.C_TARGET_USER_ID, this.targetUserId.get());
        }
        if (!this.messageLocalId.isUnset()) {
            target.put(DatabaseHelper.ColumnsLocalSendingMessage.C_MESSAGE_LOCAL_ID, this.messageLocalId.get());
        }
        if (!this.localSendStatus.isUnset()) {
            target.put(DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_SEND_STATUS, this.localSendStatus.get());
        }
        if (!this.localAbortId.isUnset()) {
            target.put(DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_ABORT_ID, this.localAbortId.get());
        }
        return target;
    }

    /**
     * 查询所有字段
     */
    public static final ColumnsSelector<LocalSendingMessage> COLUMNS_SELECTOR_ALL = new ColumnsSelector<LocalSendingMessage>() {

        @NonNull
        @Override
        public String[] queryColumns() {
            return new String[]{
                    DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_ID,
                    DatabaseHelper.ColumnsLocalSendingMessage.C_CONVERSATION_TYPE,
                    DatabaseHelper.ColumnsLocalSendingMessage.C_TARGET_USER_ID,
                    DatabaseHelper.ColumnsLocalSendingMessage.C_MESSAGE_LOCAL_ID,
                    DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_SEND_STATUS,
                    DatabaseHelper.ColumnsLocalSendingMessage.C_LOCAL_ABORT_ID,
            };
        }

        @NonNull
        @Override
        public LocalSendingMessage cursorToObjectWithQueryColumns(@NonNull Cursor cursor) {
            final LocalSendingMessage target = new LocalSendingMessage();
            int index = -1;
            target.localId.set(CursorUtil.getLong(cursor, ++index));
            target.conversationType.set(CursorUtil.getInt(cursor, ++index));
            target.targetUserId.set(CursorUtil.getLong(cursor, ++index));
            target.messageLocalId.set(CursorUtil.getLong(cursor, ++index));
            target.localSendStatus.set(CursorUtil.getInt(cursor, ++index));
            target.localAbortId.set(CursorUtil.getLong(cursor, ++index));
            return target;
        }
    };

}
