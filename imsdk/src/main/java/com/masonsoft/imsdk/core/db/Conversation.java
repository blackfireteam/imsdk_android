package com.masonsoft.imsdk.core.db;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.db.DatabaseHelper.ColumnsConversation;
import com.masonsoft.imsdk.lang.StateProp;
import com.masonsoft.imsdk.util.CursorUtil;

/**
 * 会话
 *
 * @since 1.0
 */
public class Conversation {

    /**
     * @see ColumnsConversation#C_LOCAL_ID
     */
    @NonNull
    public final StateProp<Long> localId = new StateProp<>();

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
     * @see ColumnsConversation#C_REMOTE_MSG_START
     */
    @NonNull
    public final StateProp<Long> remoteMessageStart = new StateProp<>();

    /**
     * @see ColumnsConversation#C_REMOTE_MSG_END
     */
    @NonNull
    public final StateProp<Long> remoteMessageEnd = new StateProp<>();

    /**
     * @see ColumnsConversation#C_REMOTE_MSG_LAST_READ
     */
    @NonNull
    public final StateProp<Long> remoteMessageLastRead = new StateProp<>();

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
     * @see ColumnsConversation#C_LOCAL_DELETE
     */
    @NonNull
    public final StateProp<Integer> localDelete = new StateProp<>();

    /**
     * @see ColumnsConversation#C_MATCHED
     */
    @NonNull
    public final StateProp<Integer> matched = new StateProp<>();

    /**
     * @see ColumnsConversation#C_NEW_MSG
     */
    @NonNull
    public final StateProp<Integer> newMessage = new StateProp<>();

    /**
     * @see ColumnsConversation#C_MY_MOVE
     */
    @NonNull
    public final StateProp<Integer> myMove = new StateProp<>();

    /**
     * @see ColumnsConversation#C_ICE_BREAK
     */
    @NonNull
    public final StateProp<Integer> iceBreak = new StateProp<>();

    /**
     * @see ColumnsConversation#C_TIP_FREE
     */
    @NonNull
    public final StateProp<Integer> tipFree = new StateProp<>();

    /**
     * @see ColumnsConversation#C_TOP_ALBUM
     */
    @NonNull
    public final StateProp<Integer> topAlbum = new StateProp<>();

    /**
     * @see ColumnsConversation#C_I_BLOCK_U
     */
    @NonNull
    public final StateProp<Integer> iBlockU = new StateProp<>();

    /**
     * @see ColumnsConversation#C_CONNECTED
     */
    @NonNull
    public final StateProp<Integer> connected = new StateProp<>();

    @NonNull
    public String toShortString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Conversation");
        if (this.localId.isUnset()) {
            builder.append(" localId:unset");
        } else {
            builder.append(" localId:").append(this.localId.get());
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

    @NonNull
    public ContentValues toContentValues() {
        final ContentValues target = new ContentValues();
        if (!this.localId.isUnset()) {
            target.put(ColumnsConversation.C_LOCAL_ID, this.localId.get());
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
        if (!this.remoteMessageStart.isUnset()) {
            target.put(ColumnsConversation.C_REMOTE_MSG_START, this.remoteMessageStart.get());
        }
        if (!this.remoteMessageEnd.isUnset()) {
            target.put(ColumnsConversation.C_REMOTE_MSG_END, this.remoteMessageEnd.get());
        }
        if (!this.remoteMessageLastRead.isUnset()) {
            target.put(ColumnsConversation.C_REMOTE_MSG_LAST_READ, this.remoteMessageLastRead.get());
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
        if (!this.localDelete.isUnset()) {
            target.put(ColumnsConversation.C_LOCAL_DELETE, this.localDelete.get());
        }
        if (!this.matched.isUnset()) {
            target.put(ColumnsConversation.C_MATCHED, this.matched.get());
        }
        if (!this.newMessage.isUnset()) {
            target.put(ColumnsConversation.C_NEW_MSG, this.newMessage.get());
        }
        if (!this.myMove.isUnset()) {
            target.put(ColumnsConversation.C_MY_MOVE, this.myMove.get());
        }
        if (!this.iceBreak.isUnset()) {
            target.put(ColumnsConversation.C_ICE_BREAK, this.iceBreak.get());
        }
        if (!this.tipFree.isUnset()) {
            target.put(ColumnsConversation.C_TIP_FREE, this.tipFree.get());
        }
        if (!this.topAlbum.isUnset()) {
            target.put(ColumnsConversation.C_TOP_ALBUM, this.topAlbum.get());
        }
        if (!this.iBlockU.isUnset()) {
            target.put(ColumnsConversation.C_I_BLOCK_U, this.iBlockU.get());
        }
        if (!this.connected.isUnset()) {
            target.put(ColumnsConversation.C_CONNECTED, this.connected.get());
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
                    ColumnsConversation.C_LOCAL_SEQ,
                    ColumnsConversation.C_LOCAL_CONVERSATION_TYPE,
                    ColumnsConversation.C_TARGET_USER_ID,
                    ColumnsConversation.C_REMOTE_MSG_START,
                    ColumnsConversation.C_REMOTE_MSG_END,
                    ColumnsConversation.C_REMOTE_MSG_LAST_READ,
                    ColumnsConversation.C_REMOTE_SHOW_MSG_ID,
                    ColumnsConversation.C_LOCAL_SHOW_MSG_ID,
                    ColumnsConversation.C_REMOTE_UNREAD,
                    ColumnsConversation.C_LOCAL_UNREAD_COUNT,
                    ColumnsConversation.C_LOCAL_TIME_MS,
                    ColumnsConversation.C_LOCAL_DELETE,
                    ColumnsConversation.C_MATCHED,
                    ColumnsConversation.C_NEW_MSG,
                    ColumnsConversation.C_MY_MOVE,
                    ColumnsConversation.C_ICE_BREAK,
                    ColumnsConversation.C_TIP_FREE,
                    ColumnsConversation.C_TOP_ALBUM,
                    ColumnsConversation.C_I_BLOCK_U,
                    ColumnsConversation.C_CONNECTED,
            };
        }

        @NonNull
        @Override
        public Conversation cursorToObjectWithQueryColumns(@NonNull Cursor cursor) {
            final Conversation target = new Conversation();
            int index = -1;
            target.localId.set(CursorUtil.getLong(cursor, ++index));
            target.localSeq.set(CursorUtil.getLong(cursor, ++index));
            target.localConversationType.set(CursorUtil.getInt(cursor, ++index));
            target.targetUserId.set(CursorUtil.getLong(cursor, ++index));
            target.remoteMessageStart.set(CursorUtil.getLong(cursor, ++index));
            target.remoteMessageEnd.set(CursorUtil.getLong(cursor, ++index));
            target.remoteMessageLastRead.set(CursorUtil.getLong(cursor, ++index));
            target.remoteShowMessageId.set(CursorUtil.getLong(cursor, ++index));
            target.localShowMessageId.set(CursorUtil.getLong(cursor, ++index));
            target.remoteUnread.set(CursorUtil.getLong(cursor, ++index));
            target.localUnreadCount.set(CursorUtil.getLong(cursor, ++index));
            target.localTimeMs.set(CursorUtil.getLong(cursor, ++index));
            target.localDelete.set(CursorUtil.getInt(cursor, ++index));
            target.matched.set(CursorUtil.getInt(cursor, ++index));
            target.newMessage.set(CursorUtil.getInt(cursor, ++index));
            target.myMove.set(CursorUtil.getInt(cursor, ++index));
            target.iceBreak.set(CursorUtil.getInt(cursor, ++index));
            target.tipFree.set(CursorUtil.getInt(cursor, ++index));
            target.topAlbum.set(CursorUtil.getInt(cursor, ++index));
            target.iBlockU.set(CursorUtil.getInt(cursor, ++index));
            target.connected.set(CursorUtil.getInt(cursor, ++index));
            return target;
        }
    };

}
