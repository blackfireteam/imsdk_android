package com.masonsoft.imsdk.core.db;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.core.proto.ProtoMessage;

public class ConversationFactory {

    private static final long NEW_SEQ_DIFF = 1577808000000000L;

    private ConversationFactory() {
    }

    @NonNull
    public static Conversation create(@NonNull ProtoMessage.ChatItem input) {
        final Conversation target = new Conversation();
        target.targetUserId.set(input.getUid());
        target.remoteMessageEnd.set(input.getMsgEnd());
        target.messageLastRead.set(input.getMsgLastRead());
        target.remoteShowMessageId.set(input.getShowMsgId());
        target.remoteUnread.set(input.getUnread());
        target.localUnreadCount.set(input.getUnread());
        target.iBlockU.set(IMConstants.trueOfFalse(input.getIBlockU()));
        target.delete.set(IMConstants.trueOfFalse(input.getDeleted()));

        final long showMessageTime = input.getShowMsgTime();
        if (showMessageTime > 0) {
            target.localSeq.set(Sequence.create(showMessageTime));
        } else {
            // 设置一个较小的 seq
            target.localSeq.set(Sequence.create(SignGenerator.nextMicroSeconds() - NEW_SEQ_DIFF));
        }

        return target;
    }

    @NonNull
    public static Conversation createEmptyConversation(
            final int conversationType,
            final long targetUserId) {
        final Conversation target = new Conversation();
        target.localConversationType.set(conversationType);
        target.targetUserId.set(targetUserId);
        target.localTimeMs.set(System.currentTimeMillis());
        target.localSeq.set(Sequence.create(SignGenerator.nextMicroSeconds()));
        return target;
    }

}
