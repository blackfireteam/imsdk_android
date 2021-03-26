package com.masonsoft.imsdk.core.db;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.db.DatabaseHelper.ColumnsMessage;
import com.masonsoft.imsdk.lang.StateProp;

/**
 * 消息
 *
 * @since 1.0
 */
public class Message {

    /**
     * @see ColumnsMessage#C_LOCAL_ID
     */
    @NonNull
    public final StateProp<Long> localId = new StateProp<>();

    /**
     * @see ColumnsMessage#C_LOCAL_SEQ
     */
    @NonNull
    public final StateProp<Long> localSeq = new StateProp<>();

    /**
     * @see ColumnsMessage#C_FROM_USER_ID
     */
    @NonNull
    public final StateProp<Long> fromUserId = new StateProp<>();

    /**
     * @see ColumnsMessage#C_TO_USER_ID
     */
    @NonNull
    public final StateProp<Long> toUserId = new StateProp<>();

    /**
     * @see ColumnsMessage#C_REMOTE_MSG_ID
     */
    @NonNull
    public final StateProp<Long> remoteMessageId = new StateProp<>();

    /**
     * @see ColumnsMessage#C_REMOTE_MSG_TIME
     */
    @NonNull
    public final StateProp<Long> remoteMessageTime = new StateProp<>();

    /**
     * @see ColumnsMessage#C_LOCAL_TIME_MS
     */
    @NonNull
    public final StateProp<Long> localTimeMs = new StateProp<>();

    /**
     * @see ColumnsMessage#C_REMOTE_FROM_USER_PROFILE_LAST_MODIFY_MS
     */
    @NonNull
    public final StateProp<Long> remoteFromUserProfileLastModifyMs = new StateProp<>();

    /**
     * @see ColumnsMessage#C_MSG_TYPE
     */
    @NonNull
    public final StateProp<Long> messageType = new StateProp<>();

    /**
     * @see ColumnsMessage#C_TITLE
     */
    @NonNull
    public final StateProp<String> title = new StateProp<>();

    /**
     * @see ColumnsMessage#C_BODY
     */
    @NonNull
    public final StateProp<String> body = new StateProp<>();

    /**
     * @see ColumnsMessage#C_LOCAL_BODY_ORIGIN
     */
    @NonNull
    public final StateProp<String> localBodyOrigin = new StateProp<>();

    /**
     * @see ColumnsMessage#C_THUMB
     */
    @NonNull
    public final StateProp<String> thumb = new StateProp<>();

    /**
     * @see ColumnsMessage#C_LOCAL_THUMB_ORIGIN
     */
    @NonNull
    public final StateProp<String> localThumbOrigin = new StateProp<>();

    /**
     * @see ColumnsMessage#C_WIDTH
     */
    @NonNull
    public final StateProp<Long> width = new StateProp<>();

    /**
     * @see ColumnsMessage#C_HEIGHT
     */
    @NonNull
    public final StateProp<Long> height = new StateProp<>();

    /**
     * @see ColumnsMessage#C_DURATION
     */
    @NonNull
    public final StateProp<Long> duration = new StateProp<>();

    /**
     * @see ColumnsMessage#C_LAT
     */
    @NonNull
    public final StateProp<Double> lat = new StateProp<>();

    /**
     * @see ColumnsMessage#C_LNG
     */
    @NonNull
    public final StateProp<Double> lng = new StateProp<>();

    /**
     * @see ColumnsMessage#C_ZOOM
     */
    @NonNull
    public final StateProp<Long> zoom = new StateProp<>();

    /**
     * @see ColumnsMessage#C_ERROR_CODE
     */
    @NonNull
    public final StateProp<Long> errorCode = new StateProp<>();

    /**
     * @see ColumnsMessage#C_ERROR_MESSAGE
     */
    @NonNull
    public final StateProp<String> errorMessage = new StateProp<>();

    /**
     * @see ColumnsMessage#C_LOCAL_SEND_STATUS
     */
    @NonNull
    public final StateProp<Integer> localSendStatus = new StateProp<>();

    /**
     * @see ColumnsMessage#C_LOCAL_ACTION_MSG
     */
    @NonNull
    public final StateProp<Long> localActionMessage = new StateProp<>();

    /**
     * @see ColumnsMessage#C_LOCAL_BLOCK_ID
     */
    @NonNull
    public final StateProp<Long> localBlockId = new StateProp<>();

    @NonNull
    public String toShortString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Message");
        if (this.localId.isUnset()) {
            builder.append(" localId:unset");
        } else {
            builder.append(" localId:").append(this.localId.get());
        }
        if (this.fromUserId.isUnset()) {
            builder.append(" fromUserId:unset");
        } else {
            builder.append(" fromUserId:").append(this.fromUserId.get());
        }
        if (this.toUserId.isUnset()) {
            builder.append(" toUserId:unset");
        } else {
            builder.append(" toUserId:").append(this.toUserId.get());
        }
        if (this.localBlockId.isUnset()) {
            builder.append(" localBlockId:unset");
        } else {
            builder.append(" localBlockId:").append(this.localBlockId.get());
        }
        return builder.toString();
    }

    @Override
    @NonNull
    public String toString() {
        return this.toShortString();
    }

    /**
     * 使用 input 对象的内容替换当前内容
     */
    public void apply(@NonNull Message input) {
        this.localId.apply(input.localId);
        this.localSeq.apply(input.localSeq);
        this.fromUserId.apply(input.fromUserId);
        this.toUserId.apply(input.toUserId);
        this.remoteMessageId.apply(input.remoteMessageId);
        this.remoteMessageTime.apply(input.remoteMessageTime);
        this.localTimeMs.apply(input.localTimeMs);
        this.remoteFromUserProfileLastModifyMs.apply(input.remoteFromUserProfileLastModifyMs);
        this.messageType.apply(input.messageType);
        this.title.apply(input.title);
        this.body.apply(input.body);
        this.localBodyOrigin.apply(input.localBodyOrigin);
        this.thumb.apply(input.thumb);
        this.localThumbOrigin.apply(input.localThumbOrigin);
        this.width.apply(input.width);
        this.height.apply(input.height);
        this.duration.apply(input.duration);
        this.lat.apply(input.lat);
        this.lng.apply(input.lng);
        this.zoom.apply(input.zoom);
        this.errorCode.apply(input.errorCode);
        this.errorMessage.apply(input.errorMessage);
        this.localSendStatus.apply(input.localSendStatus);
        this.localActionMessage.apply(input.localActionMessage);
        this.localBlockId.apply(input.localBlockId);
    }

    @NonNull
    public ContentValues toContentValues() {
        final ContentValues target = new ContentValues();
        if (!this.localId.isUnset()) {
            target.put(ColumnsMessage.C_LOCAL_ID, this.localId.get());
        }
        if (!this.localSeq.isUnset()) {
            target.put(ColumnsMessage.C_LOCAL_SEQ, this.localSeq.get());
        }
        if (!this.fromUserId.isUnset()) {
            target.put(ColumnsMessage.C_FROM_USER_ID, this.fromUserId.get());
        }
        if (!this.toUserId.isUnset()) {
            target.put(ColumnsMessage.C_TO_USER_ID, this.toUserId.get());
        }
        if (!this.remoteMessageId.isUnset()) {
            target.put(ColumnsMessage.C_REMOTE_MSG_ID, this.remoteMessageId.get());
        }
        if (!this.remoteMessageTime.isUnset()) {
            target.put(ColumnsMessage.C_REMOTE_MSG_TIME, this.remoteMessageTime.get());
        }
        if (!this.localTimeMs.isUnset()) {
            target.put(ColumnsMessage.C_LOCAL_TIME_MS, this.localTimeMs.get());
        }
        if (!this.remoteFromUserProfileLastModifyMs.isUnset()) {
            target.put(ColumnsMessage.C_REMOTE_FROM_USER_PROFILE_LAST_MODIFY_MS, this.remoteFromUserProfileLastModifyMs.get());
        }
        if (!this.messageType.isUnset()) {
            target.put(ColumnsMessage.C_MSG_TYPE, this.messageType.get());
        }
        if (!this.title.isUnset()) {
            target.put(ColumnsMessage.C_TITLE, this.title.get());
        }
        if (!this.body.isUnset()) {
            target.put(ColumnsMessage.C_BODY, this.body.get());
        }
        if (!this.localBodyOrigin.isUnset()) {
            target.put(ColumnsMessage.C_LOCAL_BODY_ORIGIN, this.localBodyOrigin.get());
        }
        if (!this.thumb.isUnset()) {
            target.put(ColumnsMessage.C_THUMB, this.thumb.get());
        }
        if (!this.localThumbOrigin.isUnset()) {
            target.put(ColumnsMessage.C_LOCAL_THUMB_ORIGIN, this.localThumbOrigin.get());
        }
        if (!this.width.isUnset()) {
            target.put(ColumnsMessage.C_WIDTH, this.width.get());
        }
        if (!this.height.isUnset()) {
            target.put(ColumnsMessage.C_HEIGHT, this.height.get());
        }
        if (!this.duration.isUnset()) {
            target.put(ColumnsMessage.C_DURATION, this.duration.get());
        }
        if (!this.lat.isUnset()) {
            target.put(ColumnsMessage.C_LAT, this.lat.get());
        }
        if (!this.lng.isUnset()) {
            target.put(ColumnsMessage.C_LNG, this.lng.get());
        }
        if (!this.zoom.isUnset()) {
            target.put(ColumnsMessage.C_ZOOM, this.zoom.get());
        }
        if (!this.errorCode.isUnset()) {
            target.put(ColumnsMessage.C_ERROR_CODE, this.errorCode.get());
        }
        if (!this.errorMessage.isUnset()) {
            target.put(ColumnsMessage.C_ERROR_MESSAGE, this.errorMessage.get());
        }
        if (!this.localSendStatus.isUnset()) {
            target.put(ColumnsMessage.C_LOCAL_SEND_STATUS, this.localSendStatus.get());
        }
        if (!this.localActionMessage.isUnset()) {
            target.put(ColumnsMessage.C_LOCAL_ACTION_MSG, this.localActionMessage.get());
        }
        if (!this.localBlockId.isUnset()) {
            target.put(ColumnsMessage.C_LOCAL_BLOCK_ID, this.localBlockId.get());
        }
        return target;
    }

    /**
     * 查询所有字段
     */
    public static final ColumnsSelector<Message> COLUMNS_SELECTOR_ALL = new ColumnsSelector<Message>() {

        @NonNull
        @Override
        public String[] queryColumns() {
            return new String[]{
                    ColumnsMessage.C_LOCAL_ID,
                    ColumnsMessage.C_LOCAL_SEQ,
                    ColumnsMessage.C_FROM_USER_ID,
                    ColumnsMessage.C_TO_USER_ID,
                    ColumnsMessage.C_REMOTE_MSG_ID,
                    ColumnsMessage.C_REMOTE_MSG_TIME,
                    ColumnsMessage.C_LOCAL_TIME_MS,
                    ColumnsMessage.C_REMOTE_FROM_USER_PROFILE_LAST_MODIFY_MS,
                    ColumnsMessage.C_MSG_TYPE,
                    ColumnsMessage.C_TITLE,
                    ColumnsMessage.C_BODY,
                    ColumnsMessage.C_LOCAL_BODY_ORIGIN,
                    ColumnsMessage.C_THUMB,
                    ColumnsMessage.C_LOCAL_THUMB_ORIGIN,
                    ColumnsMessage.C_WIDTH,
                    ColumnsMessage.C_HEIGHT,
                    ColumnsMessage.C_DURATION,
                    ColumnsMessage.C_LAT,
                    ColumnsMessage.C_LNG,
                    ColumnsMessage.C_ZOOM,
                    ColumnsMessage.C_ERROR_CODE,
                    ColumnsMessage.C_ERROR_MESSAGE,
                    ColumnsMessage.C_LOCAL_SEND_STATUS,
                    ColumnsMessage.C_LOCAL_ACTION_MSG,
                    ColumnsMessage.C_LOCAL_BLOCK_ID,
            };
        }

        @NonNull
        @Override
        public Message cursorToObjectWithQueryColumns(@NonNull Cursor cursor) {
            final Message target = new Message();
            int index = -1;
            target.localId.set(cursor.getLong(++index));
            target.localSeq.set(cursor.getLong(++index));
            target.fromUserId.set(cursor.getLong(++index));
            target.toUserId.set(cursor.getLong(++index));
            target.remoteMessageId.set(cursor.getLong(++index));
            target.remoteMessageTime.set(cursor.getLong(++index));
            target.localTimeMs.set(cursor.getLong(++index));
            target.remoteFromUserProfileLastModifyMs.set(cursor.getLong(++index));
            target.messageType.set(cursor.getLong(++index));
            target.title.set(cursor.getString(++index));
            target.body.set(cursor.getString(++index));
            target.localBodyOrigin.set(cursor.getString(++index));
            target.thumb.set(cursor.getString(++index));
            target.localThumbOrigin.set(cursor.getString(++index));
            target.width.set(cursor.getLong(++index));
            target.height.set(cursor.getLong(++index));
            target.duration.set(cursor.getLong(++index));
            target.lat.set(cursor.getDouble(++index));
            target.lng.set(cursor.getDouble(++index));
            target.zoom.set(cursor.getLong(++index));
            target.errorCode.set(cursor.getLong(++index));
            target.errorMessage.set(cursor.getString(++index));
            target.localSendStatus.set(cursor.getInt(++index));
            target.localActionMessage.set(cursor.getLong(++index));
            target.localBlockId.set(cursor.getLong(++index));
            return target;
        }
    };

}
