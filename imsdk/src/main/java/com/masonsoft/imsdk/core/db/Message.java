package com.masonsoft.imsdk.core.db;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.annotation.LogicField;
import com.masonsoft.imsdk.core.db.DatabaseHelper.ColumnsMessage;
import com.masonsoft.imsdk.lang.StateProp;
import com.masonsoft.imsdk.util.CursorUtil;
import com.masonsoft.imsdk.util.Objects;

/**
 * 消息
 *
 * @since 1.0
 */
public class Message {

    /**
     * 消息所属的 sessionUserId
     */
    @NonNull
    @LogicField
    public final StateProp<Long> _sessionUserId = new StateProp<>();

    /**
     * 消息所属会话的 conversationType
     */
    @NonNull
    @LogicField
    public final StateProp<Integer> _conversationType = new StateProp<>();

    /**
     * 消息所属会话的 targetUserId
     */
    @NonNull
    @LogicField
    public final StateProp<Long> _targetUserId = new StateProp<>();

    /**
     * @see ColumnsMessage#C_LOCAL_ID
     */
    @NonNull
    public final StateProp<Long> localId = new StateProp<>();

    /**
     * @see ColumnsMessage#C_SIGN
     */
    @NonNull
    public final StateProp<Long> sign = new StateProp<>();

    /**
     * 本地记录的 lastModify, 毫秒
     */
    public final StateProp<Long> localLastModifyMs = new StateProp<>();

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
    public final StateProp<Integer> messageType = new StateProp<>();

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
     * @see ColumnsMessage#C_DURATION_MS
     */
    @NonNull
    public final StateProp<Long> durationMs = new StateProp<>();

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
     * @see ColumnsMessage#C_LOCAL_ACTION_MSG
     */
    @NonNull
    public final StateProp<Integer> localActionMessage = new StateProp<>();

    /**
     * @see ColumnsMessage#C_LOCAL_BLOCK_ID
     */
    @NonNull
    public final StateProp<Long> localBlockId = new StateProp<>();

    @NonNull
    public String toShortString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(Objects.defaultObjectTag(this));
        if (this._sessionUserId.isUnset()) {
            builder.append(" _sessionUserId:unset");
        } else {
            builder.append(" _sessionUserId:").append(this._sessionUserId.get());
        }
        if (this._conversationType.isUnset()) {
            builder.append(" _conversationType:unset");
        } else {
            builder.append(" _conversationType:").append(this._conversationType.get());
        }
        if (this._targetUserId.isUnset()) {
            builder.append(" _targetUserId:unset");
        } else {
            builder.append(" _targetUserId:").append(this._targetUserId.get());
        }
        if (this.localId.isUnset()) {
            builder.append(" localId:unset");
        } else {
            builder.append(" localId:").append(this.localId.get());
        }
        if (this.sign.isUnset()) {
            builder.append(" sign:unset");
        } else {
            builder.append(" sign:").append(this.sign.get());
        }
        if (this.localLastModifyMs.isUnset()) {
            builder.append(" localLastModifyMs:unset");
        } else {
            builder.append(" localLastModifyMs:").append(this.localLastModifyMs.get());
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

    public void applyLogicField(long _sessionUserId, int _conversationType, long _targetUserId) {
        this._sessionUserId.set(_sessionUserId);
        this._conversationType.set(_conversationType);
        this._targetUserId.set(_targetUserId);
    }

    /**
     * 使用 input 对象的内容替换当前内容
     */
    public void apply(@NonNull Message input) {
        this._sessionUserId.apply(input._sessionUserId);
        this._conversationType.apply(input._conversationType);
        this._targetUserId.apply(input._targetUserId);
        this.localId.apply(input.localId);
        this.sign.apply(input.sign);
        this.localLastModifyMs.apply(input.localLastModifyMs);
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
        this.durationMs.apply(input.durationMs);
        this.lat.apply(input.lat);
        this.lng.apply(input.lng);
        this.zoom.apply(input.zoom);
        this.localActionMessage.apply(input.localActionMessage);
        this.localBlockId.apply(input.localBlockId);
    }

    @NonNull
    public ContentValues toContentValues() {
        final ContentValues target = new ContentValues();
        if (!this.localId.isUnset()) {
            target.put(ColumnsMessage.C_LOCAL_ID, this.localId.get());
        }
        if (!this.sign.isUnset()) {
            target.put(ColumnsMessage.C_SIGN, this.sign.get());
        }
        if (!this.localLastModifyMs.isUnset()) {
            target.put(ColumnsMessage.C_LOCAL_LAST_MODIFY_MS, this.localLastModifyMs.get());
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
        if (!this.durationMs.isUnset()) {
            target.put(ColumnsMessage.C_DURATION_MS, this.durationMs.get());
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
                    ColumnsMessage.C_SIGN,
                    ColumnsMessage.C_LOCAL_LAST_MODIFY_MS,
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
                    ColumnsMessage.C_DURATION_MS,
                    ColumnsMessage.C_LAT,
                    ColumnsMessage.C_LNG,
                    ColumnsMessage.C_ZOOM,
                    ColumnsMessage.C_LOCAL_ACTION_MSG,
                    ColumnsMessage.C_LOCAL_BLOCK_ID,
            };
        }

        @NonNull
        @Override
        public Message cursorToObjectWithQueryColumns(@NonNull Cursor cursor) {
            final Message target = new Message();
            int index = -1;
            target.localId.set(CursorUtil.getLong(cursor, ++index));
            target.sign.set(CursorUtil.getLong(cursor, ++index));
            target.localLastModifyMs.set(CursorUtil.getLong(cursor, ++index));
            target.localSeq.set(CursorUtil.getLong(cursor, ++index));
            target.fromUserId.set(CursorUtil.getLong(cursor, ++index));
            target.toUserId.set(CursorUtil.getLong(cursor, ++index));
            target.remoteMessageId.set(CursorUtil.getLong(cursor, ++index));
            target.remoteMessageTime.set(CursorUtil.getLong(cursor, ++index));
            target.localTimeMs.set(CursorUtil.getLong(cursor, ++index));
            target.remoteFromUserProfileLastModifyMs.set(CursorUtil.getLong(cursor, ++index));
            target.messageType.set(CursorUtil.getInt(cursor, ++index));
            target.title.set(CursorUtil.getString(cursor, ++index));
            target.body.set(CursorUtil.getString(cursor, ++index));
            target.localBodyOrigin.set(CursorUtil.getString(cursor, ++index));
            target.thumb.set(CursorUtil.getString(cursor, ++index));
            target.localThumbOrigin.set(CursorUtil.getString(cursor, ++index));
            target.width.set(CursorUtil.getLong(cursor, ++index));
            target.height.set(CursorUtil.getLong(cursor, ++index));
            target.durationMs.set(CursorUtil.getLong(cursor, ++index));
            target.lat.set(CursorUtil.getDouble(cursor, ++index));
            target.lng.set(CursorUtil.getDouble(cursor, ++index));
            target.zoom.set(CursorUtil.getLong(cursor, ++index));
            target.localActionMessage.set(CursorUtil.getInt(cursor, ++index));
            target.localBlockId.set(CursorUtil.getLong(cursor, ++index));
            return target;
        }
    };

}
