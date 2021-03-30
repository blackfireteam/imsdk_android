package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.annotation.LogicField;
import com.masonsoft.imsdk.core.IMConstants.MessageType;
import com.masonsoft.imsdk.lang.StateProp;
import com.masonsoft.imsdk.util.Objects;

/**
 * 会话中的一条消息
 *
 * @see IMMessageFactory
 * @see IMMessageFactory#copy(IMMessage)
 * @since 1.0
 */
public class IMMessage {

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
     * 消息 id, 在一个会话中消息 id 是唯一的.
     */
    @NonNull
    public final StateProp<Long> id = new StateProp<>();

    /**
     * 本地记录的 lastModify, 毫秒
     */
    public final StateProp<Long> lastModifyMs = new StateProp<>();

    /**
     * 消息的排序字段, 用于分页读取消息.
     */
    @NonNull
    public final StateProp<Long> seq = new StateProp<>();

    /**
     * 消息的发送方
     */
    @NonNull
    public final StateProp<Long> fromUserId = new StateProp<>();

    /**
     * 消息的接收方
     */
    @NonNull
    public final StateProp<Long> toUserId = new StateProp<>();

    /**
     * 消息产生的时间, 单位毫秒.<br>
     * 如果是本地发送的新消息，该时间是发送入库时的本地时间.<br>
     * 如果该消息是收到的服务器下发的消息, 并且该消息在本地不存在，则该时间是这一条
     * 服务器下发的消息上的服务器时间。
     */
    @NonNull
    public final StateProp<Long> timeMs = new StateProp<>();

    /**
     * 消息的类型
     *
     * @see MessageType
     */
    @NonNull
    public final StateProp<Integer> type = new StateProp<>();

    /**
     * 消息内容：title
     */
    @NonNull
    public final StateProp<String> title = new StateProp<>();

    /**
     * 消息内容：body
     */
    @NonNull
    public final StateProp<String> body = new StateProp<>();

    /**
     * 消息内容：thumb
     */
    @NonNull
    public final StateProp<String> thumb = new StateProp<>();

    /**
     * 消息内容：width
     */
    @NonNull
    public final StateProp<Long> width = new StateProp<>();

    /**
     * 消息内容：height
     */
    @NonNull
    public final StateProp<Long> height = new StateProp<>();

    /**
     * 消息内容：duration
     */
    @NonNull
    public final StateProp<Long> duration = new StateProp<>();

    /**
     * 消息内容：lat
     */
    @NonNull
    public final StateProp<Double> lat = new StateProp<>();

    /**
     * 消息内容：lng
     */
    @NonNull
    public final StateProp<Double> lng = new StateProp<>();

    /**
     * 消息内容：zoom
     */
    @NonNull
    public final StateProp<Long> zoom = new StateProp<>();

    /**
     * 消息发送失败时的错误码
     */
    @NonNull
    public final StateProp<Long> errorCode = new StateProp<>();

    /**
     * 消息发送失败时的错误描述信息
     */
    @NonNull
    public final StateProp<String> errorMessage = new StateProp<>();

    /**
     * 消息的发送状态
     *
     * @see com.masonsoft.imsdk.core.IMConstants.SendStatus
     */
    @NonNull
    public final StateProp<Integer> sendState = new StateProp<>();

    /**
     * 消息的发送进度. 范围 [0, 1]
     */
    @NonNull
    public final StateProp<Float> sendProgress = new StateProp<>();

    public void applyLogicField(long _sessionUserId, int _conversationType, long _targetUserId) {
        this._sessionUserId.set(_sessionUserId);
        this._conversationType.set(_conversationType);
        this._targetUserId.set(_targetUserId);
    }

    /**
     * 使用 input 对象的内容替换当前内容
     */
    public void apply(@NonNull IMMessage input) {
        this._sessionUserId.apply(input._sessionUserId);
        this._conversationType.apply(input._conversationType);
        this._targetUserId.apply(input._targetUserId);
        this.id.apply(input.id);
        this.lastModifyMs.apply(input.lastModifyMs);
        this.seq.apply(input.seq);
        this.fromUserId.apply(input.fromUserId);
        this.toUserId.apply(input.toUserId);
        this.timeMs.apply(input.timeMs);
        this.type.apply(input.type);
        this.title.apply(input.title);
        this.body.apply(input.body);
        this.thumb.apply(input.thumb);
        this.width.apply(input.width);
        this.height.apply(input.height);
        this.duration.apply(input.duration);
        this.lat.apply(input.lat);
        this.lng.apply(input.lng);
        this.zoom.apply(input.zoom);
        this.errorCode.apply(input.errorCode);
        this.errorMessage.apply(input.errorMessage);
        this.sendState.apply(input.sendState);
        this.sendProgress.apply(input.sendProgress);
    }

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
        if (this.id.isUnset()) {
            builder.append(" id:unset");
        } else {
            builder.append(" id:").append(this.id.get());
        }
        if (this.lastModifyMs.isUnset()) {
            builder.append(" lastModifyMs:unset");
        } else {
            builder.append(" lastModifyMs:").append(this.lastModifyMs.get());
        }
        if (this.type.isUnset()) {
            builder.append(" type:unset");
        } else {
            builder.append(" type:").append(this.type.get());
        }
        if (this.seq.isUnset()) {
            builder.append(" seq:unset");
        } else {
            builder.append(" seq:").append(this.seq.get());
        }
        return builder.toString();
    }

    @Override
    @NonNull
    public String toString() {
        return toShortString();
    }

}
