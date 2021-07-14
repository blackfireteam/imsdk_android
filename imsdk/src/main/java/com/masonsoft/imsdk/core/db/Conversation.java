package com.masonsoft.imsdk.core.db;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.annotation.LogicField;
import com.masonsoft.imsdk.core.db.DatabaseHelper.ColumnsConversation;
import com.masonsoft.imsdk.lang.StateProp;
import com.masonsoft.imsdk.util.CursorUtil;
import com.masonsoft.imsdk.util.Objects;

/**
 * 会话
 *
 * @since 1.0
 */
public class Conversation {

    /**
     * 会话所属的 sessionUserId
     */
    @NonNull
    @LogicField
    public final StateProp<Long> _sessionUserId = new StateProp<>();

    /**
     * @see ColumnsConversation#C_LOCAL_ID
     */
    @NonNull
    public final StateProp<Long> localId = new StateProp<>();

    /**
     * 本地记录的 lastModify, 毫秒
     */
    public final StateProp<Long> localLastModifyMs = new StateProp<>();

    /**
     * @see ColumnsConversation#C_LOCAL_SEQ
     */
    @NonNull
    public final StateProp<Long> localSeq = new StateProp<>();

    /**
     * @see ColumnsConversation#C_LOCAL_CONVERSATION_TYPE
     */
    @NonNull
    public final StateProp<Integer> localConversationType = new StateProp<>();

    /**
     * @see ColumnsConversation#C_TARGET_USER_ID
     */
    @NonNull
    public final StateProp<Long> targetUserId = new StateProp<>();

    /**
     * @see ColumnsConversation#C_REMOTE_MSG_END
     */
    @NonNull
    public final StateProp<Long> remoteMessageEnd = new StateProp<>();

    /**
     * @see ColumnsConversation#C_MSG_LAST_READ
     */
    @NonNull
    public final StateProp<Long> messageLastRead = new StateProp<>();

    /**
     * @see ColumnsConversation#C_REMOTE_SHOW_MSG_ID
     */
    @NonNull
    public final StateProp<Long> remoteShowMessageId = new StateProp<>();

    /**
     * @see ColumnsConversation#C_LOCAL_SHOW_MSG_ID
     */
    @NonNull
    public final StateProp<Long> localShowMessageId = new StateProp<>();

    /**
     * @see ColumnsConversation#C_REMOTE_UNREAD
     */
    @NonNull
    public final StateProp<Long> remoteUnread = new StateProp<>();

    /**
     * @see ColumnsConversation#C_LOCAL_UNREAD_COUNT
     */
    @NonNull
    public final StateProp<Long> localUnreadCount = new StateProp<>();

    /**
     * @see ColumnsConversation#C_LOCAL_TIME_MS
     */
    @NonNull
    public final StateProp<Long> localTimeMs = new StateProp<>();

    /**
     * @see ColumnsConversation#C_DELETE
     */
    @NonNull
    public final StateProp<Integer> delete = new StateProp<>();

    /**
     * @see ColumnsConversation#C_I_BLOCK_U
     */
    @NonNull
    public final StateProp<Integer> iBlockU = new StateProp<>();

    @NonNull
    public String toShortString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(Objects.defaultObjectTag(this));
        if (this.localId.isUnset()) {
            builder.append(" localId:unset");
        } else {
            builder.append(" localId:").append(this.localId.get());
        }
        if (this.localLastModifyMs.isUnset()) {
            builder.append(" localLastModifyMs:unset");
        } else {
            builder.append(" localLastModifyMs:").append(this.localLastModifyMs.get());
        }
        if (this.localSeq.isUnset()) {
            builder.append(" localSeq:unset");
        } else {
            builder.append(" localSeq:").append(this.localSeq.get());
        }
        if (this.localConversationType.isUnset()) {
            builder.append(" localConversationType:unset");
        } else {
            builder.append(" localConversationType:").append(this.localConversationType.get());
        }
        if (this.targetUserId.isUnset()) {
            builder.append(" targetUserId:unset");
        } else {
            builder.append(" targetUserId:").append(this.targetUserId.get());
        }
        return builder.toString();
    }

    @Override
    @NonNull
    public String toString() {
        return this.toShortString();
    }

    public void applyLogicField(long _sessionUserId) {
        this._sessionUserId.set(_sessionUserId);
    }

    @NonNull
    public ContentValues toContentValues() {
        final ContentValues target = new ContentValues();
        if (!this.localId.isUnset()) {
            target.put(ColumnsConversation.C_LOCAL_ID, this.localId.get());
        }
        if (!this.localLastModifyMs.isUnset()) {
            target.put(ColumnsConversation.C_LOCAL_LAST_MODIFY_MS, this.localLastModifyMs.get());
        }
        if (!this.localSeq.isUnset()) {
            target.put(ColumnsConversation.C_LOCAL_SEQ, this.localSeq.get());
        }
        if (!this.localConversationType.isUnset()) {
            target.put(ColumnsConversation.C_LOCAL_CONVERSATION_TYPE, this.localConversationType.get());
        }
        if (!this.targetUserId.isUnset()) {
            target.put(ColumnsConversation.C_TARGET_USER_ID, this.targetUserId.get());
        }
        if (!this.remoteMessageEnd.isUnset()) {
            target.put(ColumnsConversation.C_REMOTE_MSG_END, this.remoteMessageEnd.get());
        }
        if (!this.messageLastRead.isUnset()) {
            target.put(ColumnsConversation.C_MSG_LAST_READ, this.messageLastRead.get());
        }
        if (!this.remoteShowMessageId.isUnset()) {
            target.put(ColumnsConversation.C_REMOTE_SHOW_MSG_ID, this.remoteShowMessageId.get());
        }
        if (!this.localShowMessageId.isUnset()) {
            target.put(ColumnsConversation.C_LOCAL_SHOW_MSG_ID, this.localShowMessageId.get());
        }
        if (!this.remoteUnread.isUnset()) {
            target.put(ColumnsConversation.C_REMOTE_UNREAD, this.remoteUnread.get());
        }
        if (!this.localUnreadCount.isUnset()) {
            target.put(ColumnsConversation.C_LOCAL_UNREAD_COUNT, this.localUnreadCount.get());
        }
        if (!this.localTimeMs.isUnset()) {
            target.put(ColumnsConversation.C_LOCAL_TIME_MS, this.localTimeMs.get());
        }
        if (!this.delete.isUnset()) {
            target.put(ColumnsConversation.C_DELETE, this.delete.get());
        }
        if (!this.iBlockU.isUnset()) {
            target.put(ColumnsConversation.C_I_BLOCK_U, this.iBlockU.get());
        }
        return target;
    }

    /**
     * 查询所有字段
     */
    public static final ColumnsSelector<Conversation> COLUMNS_SELECTOR_ALL = new ColumnsSelector<Conversation>() {

        @NonNull
        @Override
        public String[] queryColumns() {
            return new String[]{
                    ColumnsConversation.C_LOCAL_ID,
                    ColumnsConversation.C_LOCAL_LAST_MODIFY_MS,
                    ColumnsConversation.C_LOCAL_SEQ,
                    ColumnsConversation.C_LOCAL_CONVERSATION_TYPE,
                    ColumnsConversation.C_TARGET_USER_ID,
                    ColumnsConversation.C_REMOTE_MSG_END,
                    ColumnsConversation.C_MSG_LAST_READ,
                    ColumnsConversation.C_REMOTE_SHOW_MSG_ID,
                    ColumnsConversation.C_LOCAL_SHOW_MSG_ID,
                    ColumnsConversation.C_REMOTE_UNREAD,
                    ColumnsConversation.C_LOCAL_UNREAD_COUNT,
                    ColumnsConversation.C_LOCAL_TIME_MS,
                    ColumnsConversation.C_DELETE,
                    ColumnsConversation.C_I_BLOCK_U,
            };
        }

        @NonNull
        @Override
        public Conversation cursorToObjectWithQueryColumns(@NonNull Cursor cursor) {
            final Conversation target = new Conversation();
            int index = -1;
            target.localId.set(CursorUtil.getLong(cursor, ++index));
            target.localLastModifyMs.set(CursorUtil.getLong(cursor, ++index));
            target.localSeq.set(CursorUtil.getLong(cursor, ++index));
            target.localConversationType.set(CursorUtil.getInt(cursor, ++index));
            target.targetUserId.set(CursorUtil.getLong(cursor, ++index));
            target.remoteMessageEnd.set(CursorUtil.getLong(cursor, ++index));
            target.messageLastRead.set(CursorUtil.getLong(cursor, ++index));
            target.remoteShowMessageId.set(CursorUtil.getLong(cursor, ++index));
            target.localShowMessageId.set(CursorUtil.getLong(cursor, ++index));
            target.remoteUnread.set(CursorUtil.getLong(cursor, ++index));
            target.localUnreadCount.set(CursorUtil.getLong(cursor, ++index));
            target.localTimeMs.set(CursorUtil.getLong(cursor, ++index));
            target.delete.set(CursorUtil.getInt(cursor, ++index));
            target.iBlockU.set(CursorUtil.getInt(cursor, ++index));
            return target;
        }
    };

}
