package com.masonsoft.imsdk.core.db;

import androidx.annotation.NonNull;

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
        target.width.set((int) input.getWidth());
        target.height.set((int) input.getHeight());
        target.duration.set(input.getDuration());
        target.lat.set(input.getLat());
        target.lng.set(input.getLng());
        target.zoom.set((int) input.getZoom());

        if (IMConstants.MessageType.isVisible((int) input.getType())) {
            // 消息类型不可见，是一种指令消息
            target.localActionMessage.set(IMConstants.TRUE);
        } else {
            // 非指令消息
            target.localActionMessage.set(IMConstants.FALSE);
        }

        return target;
    }

}
