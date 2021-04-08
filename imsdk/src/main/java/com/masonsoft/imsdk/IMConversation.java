package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.annotation.LogicField;
import com.masonsoft.imsdk.lang.StateProp;
import com.masonsoft.imsdk.util.Objects;

/**
 * 一个会话
 *
 * @since 1.0
 */
public class IMConversation {

    /**
     * 会话所属的 sessionUserId
     */
    @NonNull
    @LogicField
    public final StateProp<Long> _sessionUserId = new StateProp<>();

    /**
     * 会话 id, 在同一个登录信息下是唯一的.
     */
    @NonNull
    public final StateProp<Long> id = new StateProp<>();

    /**
     * 本地记录的 lastModify, 毫秒
     */
    public final StateProp<Long> lastModifyMs = new StateProp<>();

    /**
     * 排序字段, 用于分页读取会话.
     */
    @NonNull
    public final StateProp<Long> seq = new StateProp<>();

    /**
     * 会话的类型
     *
     * @see com.masonsoft.imsdk.core.IMConstants.ConversationType
     */
    @NonNull
    public final StateProp<Integer> type = new StateProp<>();

    /**
     * 会话的对话方
     */
    @NonNull
    public final StateProp<Long> targetUserId = new StateProp<>();

    /**
     * 在该会话上应该显示的 "最后一条" 消息
     */
    @NonNull
    public final StateProp<Long> showMessageId = new StateProp<>();

    /**
     * 未读消息数
     */
    @NonNull
    public final StateProp<Long> unreadCount = new StateProp<>();

    /**
     * 会话创建/更新的时间, 单位毫秒.<br>
     * 一般的，该时间与 {@linkplain #showMessageId} 对应的消息的时间相同
     */
    @NonNull
    public final StateProp<Long> timeMs = new StateProp<>();

    /**
     * 删除状态(软删除)
     */
    @NonNull
    public final StateProp<Integer> delete = new StateProp<>();

    /**
     * 自定义业务
     */
    @NonNull
    public final StateProp<Integer> matched = new StateProp<>();

    /**
     * 自定义业务
     */
    @NonNull
    public final StateProp<Integer> newMessage = new StateProp<>();

    /**
     * 自定义业务
     */
    @NonNull
    public final StateProp<Integer> myMove = new StateProp<>();

    /**
     * 自定义业务
     */
    @NonNull
    public final StateProp<Integer> iceBreak = new StateProp<>();

    /**
     * 自定义业务
     */
    @NonNull
    public final StateProp<Integer> tipFree = new StateProp<>();

    /**
     * 自定义业务
     */
    @NonNull
    public final StateProp<Integer> topAlbum = new StateProp<>();

    /**
     * 自定义业务
     */
    @NonNull
    public final StateProp<Integer> iBlockU = new StateProp<>();

    /**
     * 自定义业务
     */
    @NonNull
    public final StateProp<Integer> connected = new StateProp<>();

    public void applyLogicField(long _sessionUserId) {
        this._sessionUserId.set(_sessionUserId);
    }

    /**
     * 使用 input 对象的内容替换当前内容
     */
    public void apply(@NonNull IMConversation input) {
        this._sessionUserId.apply(input._sessionUserId);
        this.id.apply(input.id);
        this.lastModifyMs.apply(input.lastModifyMs);
        this.seq.apply(input.seq);
        this.type.apply(input.type);
        this.targetUserId.apply(input.targetUserId);
        this.showMessageId.apply(input.showMessageId);
        this.unreadCount.apply(input.unreadCount);
        this.timeMs.apply(input.timeMs);
        this.delete.apply(input.delete);
        this.matched.apply(input.matched);
        this.newMessage.apply(input.newMessage);
        this.myMove.apply(input.myMove);
        this.iceBreak.apply(input.iceBreak);
        this.tipFree.apply(input.tipFree);
        this.topAlbum.apply(input.topAlbum);
        this.iBlockU.apply(input.iBlockU);
        this.connected.apply(input.connected);
    }

    @NonNull
    public String toShortString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(Objects.defaultObjectTag(this));
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
