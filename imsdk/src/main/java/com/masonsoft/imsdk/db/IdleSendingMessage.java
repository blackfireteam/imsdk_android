package com.masonsoft.imsdk.db;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.lang.StateProp;

/**
 * 将所有消息表(按照会话分表)中待发送与发送中的消息记录一个统一的副本。相当于一个持久化的发送队列。
 *
 * @since 1.0
 */
public class IdleSendingMessage {

    /**
     * @see DatabaseHelper.ColumnsIdleSendingMessage#C_LOCAL_ID
     */
    @NonNull
    public final StateProp<Long> localId = new StateProp<>();

    /**
     * @see DatabaseHelper.ColumnsIdleSendingMessage#C_CONVERSATION_TYPE
     */
    @NonNull
    public final StateProp<Integer> conversationType = new StateProp<>();

    /**
     * @see DatabaseHelper.ColumnsIdleSendingMessage#C_TARGET_USER_ID
     */
    @NonNull
    public final StateProp<Long> targetUserId = new StateProp<>();

    /**
     * @see DatabaseHelper.ColumnsIdleSendingMessage#C_MESSAGE_LOCAL_ID
     */
    @NonNull
    public final StateProp<Long> messageLocalId = new StateProp<>();

    @NonNull
    public ContentValues toContentValues() {
        final ContentValues target = new ContentValues();
        if (!this.localId.isUnset()) {
            target.put(DatabaseHelper.ColumnsIdleSendingMessage.C_LOCAL_ID, this.localId.get());
        }
        if (!this.conversationType.isUnset()) {
            target.put(DatabaseHelper.ColumnsIdleSendingMessage.C_CONVERSATION_TYPE, this.conversationType.get());
        }
        if (!this.targetUserId.isUnset()) {
            target.put(DatabaseHelper.ColumnsIdleSendingMessage.C_TARGET_USER_ID, this.targetUserId.get());
        }
        if (!this.messageLocalId.isUnset()) {
            target.put(DatabaseHelper.ColumnsIdleSendingMessage.C_MESSAGE_LOCAL_ID, this.messageLocalId.get());
        }
        return target;
    }

    /**
     * 查询所有字段
     */
    public static final ColumnsSelector<IdleSendingMessage> COLUMNS_SELECTOR_ALL = new ColumnsSelector<IdleSendingMessage>() {

        @NonNull
        @Override
        public String[] queryColumns() {
            return new String[]{
                    DatabaseHelper.ColumnsIdleSendingMessage.C_LOCAL_ID,
                    DatabaseHelper.ColumnsIdleSendingMessage.C_CONVERSATION_TYPE,
                    DatabaseHelper.ColumnsIdleSendingMessage.C_TARGET_USER_ID,
                    DatabaseHelper.ColumnsIdleSendingMessage.C_MESSAGE_LOCAL_ID,
            };
        }

        @NonNull
        @Override
        public IdleSendingMessage cursorToObjectWithQueryColumns(@NonNull Cursor cursor) {
            final IdleSendingMessage target = new IdleSendingMessage();
            int index = -1;
            target.localId.set(cursor.getLong(++index));
            target.conversationType.set(cursor.getInt(++index));
            target.targetUserId.set(cursor.getLong(++index));
            target.messageLocalId.set(cursor.getLong(++index));
            return target;
        }
    };

}
