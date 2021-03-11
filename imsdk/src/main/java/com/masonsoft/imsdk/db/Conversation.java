package com.masonsoft.imsdk.db;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.lang.StateProp;

public class Conversation {

    /**
     * 自增主键
     */
    public StateProp<Long> id;

    /**
     * 会话的排序字段
     */
    public StateProp<Long> seq;

    /**
     * 会话目标用户 id
     */
    public StateProp<Long> targetUserId;

    /**
     * 会话中的第一条消息 id(服务器消息 id)
     */
    public StateProp<Long> messageStartId;

    /**
     * 会话中的最后一条消息 id(服务器消息 id)
     */
    public StateProp<Long> messageEndId;

    /**
     * 最后一条已读消息 id(服务器消息 id)
     */
    public StateProp<Long> messageLastReadId;

    /**
     * 在会话上需要展示的那条消息的类型
     */
    public StateProp<Integer> showMessageType;

    /**
     * 在会话上需要展示的那条消息的 id(服务器消息 id)
     */
    public StateProp<Long> showMessageId;

    /**
     * 会话中的最后一条消息 id，可能是发送的，也可能是收到的 (对应消息表的自增主键)(本地消息 id)
     */
    public StateProp<Long> lastMessageId;

    /**
     * 会话是否置顶
     */
    public StateProp<Integer> top;

    /**
     * 会话未读消息数
     */
    public StateProp<Integer> unreadCount;

    /**
     * 会话类型，用来区分是聊天消息，系统消息等等。
     */
    public StateProp<Integer> conversationType;

    /**
     * 会话的展示时间，通常是最后一条消息的时间(毫秒).
     */
    public StateProp<Long> timeMs;

    /**
     * 业务定制：是否 match
     */
    public StateProp<Integer> matched;

    /**
     * 业务定制：是否是 new message
     */
    public StateProp<Integer> newMessage;

    /**
     * 业务定制：是否 my move
     */
    public StateProp<Integer> myMove;

    /**
     * 业务定制：是否 ice break
     */
    public StateProp<Integer> iceBreak;

    /**
     * 业务定制：是否 tip free
     */
    public StateProp<Integer> tipFree;

    /**
     * 业务定制：是否 top album
     */
    public StateProp<Integer> topAlbum;

    @NonNull
    public ContentValues toContentValues() {
        final ContentValues target = new ContentValues();
        if (!this.id.isUnset()) {
            target.put(DatabaseHelper.ColumnsConversation.C_ID, this.id.get());
        }
        if (!this.seq.isUnset()) {
            target.put(DatabaseHelper.ColumnsConversation.C_SEQ, this.seq.get());
        }
        if (!this.targetUserId.isUnset()) {
            target.put(DatabaseHelper.ColumnsConversation.C_TARGET_USER_ID, this.targetUserId.get());
        }
        if (!this.messageStartId.isUnset()) {
            target.put(DatabaseHelper.ColumnsConversation.C_MSG_START_ID, this.messageStartId.get());
        }
        if (!this.messageEndId.isUnset()) {
            target.put(DatabaseHelper.ColumnsConversation.C_MSG_END_ID, this.messageEndId.get());
        }
        if (!this.messageLastReadId.isUnset()) {
            target.put(DatabaseHelper.ColumnsConversation.C_MSG_LAST_READ_ID, this.messageLastReadId.get());
        }
        if (!this.showMessageType.isUnset()) {
            target.put(DatabaseHelper.ColumnsConversation.C_SHOW_MSG_TYPE, this.showMessageType.get());
        }
        if (!this.showMessageId.isUnset()) {
            target.put(DatabaseHelper.ColumnsConversation.C_SHOW_MSG_ID, this.showMessageId.get());
        }
        if (!this.lastMessageId.isUnset()) {
            target.put(DatabaseHelper.ColumnsConversation.C_LAST_MSG_ID, this.lastMessageId.get());
        }
        if (!this.top.isUnset()) {
            target.put(DatabaseHelper.ColumnsConversation.C_TOP, this.top.get());
        }
        if (!this.unreadCount.isUnset()) {
            target.put(DatabaseHelper.ColumnsConversation.C_UNREAD_COUNT, this.unreadCount.get());
        }
        if (!this.conversationType.isUnset()) {
            target.put(DatabaseHelper.ColumnsConversation.C_CONVERSATION_TYPE, this.conversationType.get());
        }
        if (!this.timeMs.isUnset()) {
            target.put(DatabaseHelper.ColumnsConversation.C_TIME_MS, this.timeMs.get());
        }
        if (!this.matched.isUnset()) {
            target.put(DatabaseHelper.ColumnsConversation.C_MATCHED, this.matched.get());
        }
        if (!this.newMessage.isUnset()) {
            target.put(DatabaseHelper.ColumnsConversation.C_NEW_MSG, this.newMessage.get());
        }
        if (!this.myMove.isUnset()) {
            target.put(DatabaseHelper.ColumnsConversation.C_MY_MOVE, this.myMove.get());
        }
        if (!this.iceBreak.isUnset()) {
            target.put(DatabaseHelper.ColumnsConversation.C_ICE_BREAK, this.iceBreak.get());
        }
        if (!this.tipFree.isUnset()) {
            target.put(DatabaseHelper.ColumnsConversation.C_TIP_FREE, this.tipFree.get());
        }
        if (!this.topAlbum.isUnset()) {
            target.put(DatabaseHelper.ColumnsConversation.C_TOP_ALBUM, this.topAlbum.get());
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
                    DatabaseHelper.ColumnsConversation.C_ID,
                    DatabaseHelper.ColumnsConversation.C_SEQ,
                    DatabaseHelper.ColumnsConversation.C_TARGET_USER_ID,
                    DatabaseHelper.ColumnsConversation.C_MSG_START_ID,
                    DatabaseHelper.ColumnsConversation.C_MSG_END_ID,
                    DatabaseHelper.ColumnsConversation.C_MSG_LAST_READ_ID,
                    DatabaseHelper.ColumnsConversation.C_SHOW_MSG_TYPE,
                    DatabaseHelper.ColumnsConversation.C_SHOW_MSG_ID,
                    DatabaseHelper.ColumnsConversation.C_LAST_MSG_ID,
                    DatabaseHelper.ColumnsConversation.C_TOP,
                    DatabaseHelper.ColumnsConversation.C_UNREAD_COUNT,
                    DatabaseHelper.ColumnsConversation.C_CONVERSATION_TYPE,
                    DatabaseHelper.ColumnsConversation.C_TIME_MS,
                    DatabaseHelper.ColumnsConversation.C_MATCHED,
                    DatabaseHelper.ColumnsConversation.C_NEW_MSG,
                    DatabaseHelper.ColumnsConversation.C_MY_MOVE,
                    DatabaseHelper.ColumnsConversation.C_ICE_BREAK,
                    DatabaseHelper.ColumnsConversation.C_TIP_FREE,
                    DatabaseHelper.ColumnsConversation.C_TOP_ALBUM,
            };
        }

        @NonNull
        @Override
        public Conversation cursorToObjectWithQueryColumns(@NonNull Cursor cursor) {
            final Conversation target = new Conversation();
            int index = -1;
            target.id.set(cursor.getLong(++index));
            target.seq.set(cursor.getLong(++index));
            target.targetUserId.set(cursor.getLong(++index));
            target.messageStartId.set(cursor.getLong(++index));
            target.messageEndId.set(cursor.getLong(++index));
            target.messageLastReadId.set(cursor.getLong(++index));
            target.showMessageType.set(cursor.getInt(++index));
            target.showMessageId.set(cursor.getLong(++index));
            target.lastMessageId.set(cursor.getLong(++index));
            target.top.set(cursor.getInt(++index));
            target.unreadCount.set(cursor.getInt(++index));
            target.conversationType.set(cursor.getInt(++index));
            target.timeMs.set(cursor.getLong(++index));
            target.matched.set(cursor.getInt(++index));
            target.newMessage.set(cursor.getInt(++index));
            target.myMove.set(cursor.getInt(++index));
            target.iceBreak.set(cursor.getInt(++index));
            target.tipFree.set(cursor.getInt(++index));
            target.topAlbum.set(cursor.getInt(++index));
            return target;
        }
    };

}
