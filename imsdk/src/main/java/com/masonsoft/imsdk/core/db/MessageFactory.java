package com.masonsoft.imsdk.core.db;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMMessage;
import com.masonsoft.imsdk.core.proto.ProtoMessage;

/**
 * @since 1.0
 */
public class MessageFactory {

    private MessageFactory() {
    }

    @NonNull
    public static Message create(@NonNull ProtoMessage.ChatR input) {
        final Message target = new Message();

        // 如果服务器返回的消息中有明确的 sign 值, 才使用
        final long sign = input.getSign();
        if (sign != 0) {
            target.sign.set(sign);
        }

        target.localSeq.set(Sequence.create(input.getMsgTime()));
        target.fromUserId.set(input.getFromUid());
        target.toUserId.set(input.getToUid());
        target.remoteMessageId.set(input.getMsgId());
        target.remoteMessageTime.set(input.getMsgTime());
        target.localTimeMs.set(input.getMsgTime() / 1000);

        // 将服务器返回的秒转换为毫秒
        target.remoteFromUserProfileLastModifyMs.set(input.getSput() * 1000);

        target.messageType.set((int) input.getType());
        target.title.set(input.getTitle());
        target.body.set(input.getBody());
        target.thumb.set(input.getThumb());
        target.width.set(input.getWidth());
        target.height.set(input.getHeight());

        // 将消息中的时长 秒 转换为毫秒
        target.durationMs.set(input.getDuration() * 1000);

        target.lat.set(input.getLat());
        target.lng.set(input.getLng());
        target.zoom.set(input.getZoom());

        if (IMConstants.MessageType.isActionMessage((int) input.getType())) {
            // 消息类型不可见，是一种指令消息
            target.localActionMessage.set(IMConstants.TRUE);
        } else {
            // 非指令消息
            target.localActionMessage.set(IMConstants.FALSE);
        }

        return target;
    }

    @NonNull
    public static Message create(@NonNull IMMessage input) {
        final Message target = new Message();
        target._sessionUserId.apply(input._sessionUserId);
        target._conversationType.apply(input._conversationType);
        target._targetUserId.apply(input._targetUserId);
        target.localId.apply(input.id);
        target.sign.apply(input.sign);
        target.remoteMessageId.apply(input.serverMessageId);
        target.localLastModifyMs.apply(input.lastModifyMs);
        target.localSeq.apply(input.seq);
        target.fromUserId.apply(input.fromUserId);
        target.toUserId.apply(input.toUserId);
        target.localTimeMs.apply(input.timeMs);
        target.messageType.apply(input.type);
        target.title.apply(input.title);
        target.body.apply(input.body);
        target.localBodyOrigin.apply(input.localBodyOrigin);
        target.thumb.apply(input.thumb);
        target.localThumbOrigin.apply(input.localThumbOrigin);
        target.width.apply(input.width);
        target.height.apply(input.height);
        target.durationMs.apply(input.durationMs);
        target.lat.apply(input.lat);
        target.lng.apply(input.lng);
        target.zoom.apply(input.zoom);

        if (!input.type.isUnset() && input.type.get() != null) {
            // 当设置了有效的 type 值
            if (IMConstants.MessageType.isActionMessage(input.type.get())) {
                target.localActionMessage.set(IMConstants.TRUE);
            } else {
                target.localActionMessage.set(IMConstants.FALSE);
            }
        }

        return target;
    }

    @NonNull
    public static Message copy(@NonNull Message input) {
        final Message target = new Message();
        target.apply(input);
        return target;
    }

}
