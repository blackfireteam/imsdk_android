package com.masonsoft.imsdk.core.db;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.core.IMConstants;
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
        target.localSeq.set(Sequence.create(input.getMsgTime()));
        target.fromUserId.set(input.getFromUid());
        target.toUserId.set(input.getToUid());
        target.remoteMessageId.set(input.getMsgId());
        target.remoteMessageTime.set(input.getMsgTime());
        target.localTimeMs.set(System.currentTimeMillis());

        // 将服务器返回的秒转换为毫秒
        target.remoteFromUserProfileLastModifyMs.set(input.getSput() * 1000);

        target.messageType.set(input.getType());
        target.title.set(input.getTitle());
        target.body.set(input.getBody());
        target.thumb.set(input.getThumb());
        target.width.set(input.getWidth());
        target.height.set(input.getHeight());
        target.duration.set(input.getDuration());
        target.lat.set(input.getLat());
        target.lng.set(input.getLng());
        target.zoom.set(input.getZoom());

        if (IMConstants.MessageType.isActionMessage(input.getType())) {
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
        target.localId.apply(input.id);
        target.localSeq.apply(input.seq);
        target.fromUserId.apply(input.fromUserId);
        target.toUserId.apply(input.toUserId);
        target.localTimeMs.apply(input.timeMs);
        target.messageType.apply(input.type);
        target.title.apply(input.title);
        target.body.apply(input.body);
        target.thumb.apply(input.thumb);
        target.width.apply(input.width);
        target.height.apply(input.height);
        target.duration.apply(input.duration);
        target.lat.apply(input.lat);
        target.lng.apply(input.lng);
        target.zoom.apply(input.zoom);
        target.errorCode.apply(input.errorCode);
        target.errorMessage.apply(input.errorMessage);
        target.localSendStatus.apply(input.sendState);

        return target;
    }

    @NonNull
    public static Message copy(@NonNull Message input) {
        final Message target = new Message();
        target.apply(input);
        return target;
    }

}
