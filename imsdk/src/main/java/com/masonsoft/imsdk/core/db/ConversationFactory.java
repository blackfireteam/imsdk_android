package com.masonsoft.imsdk.core.db;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.core.proto.ProtoMessage;

public class ConversationFactory {

    private ConversationFactory() {
    }

    @NonNull
    public static Conversation create(@NonNull ProtoMessage.ChatItem input) {
        final Conversation target = new Conversation();
        target.targetUserId.set(input.getUid());
        target.remoteMessageEnd.set(input.getMsgEnd());
        target.remoteMessageLastRead.set(input.getMsgLastRead());
        target.remoteShowMessageId.set(input.getShowMsgId());
        target.remoteUnread.set(input.getUnread());
        target.localUnreadCount.set(input.getUnread());
        target.matched.set(input.getMatched() ? IMConstants.TRUE : IMConstants.FALSE);
        target.newMessage.set(input.getNewMsg() ? IMConstants.TRUE : IMConstants.FALSE);
        target.myMove.set(input.getMyMove() ? IMConstants.TRUE : IMConstants.FALSE);
        target.iceBreak.set(input.getIceBreak() ? IMConstants.TRUE : IMConstants.FALSE);
        target.tipFree.set(input.getTipFree() ? IMConstants.TRUE : IMConstants.FALSE);
        target.topAlbum.set(input.getTopAlbum() ? IMConstants.TRUE : IMConstants.FALSE);
        target.iBlockU.set(input.getIBlockU() ? IMConstants.TRUE : IMConstants.FALSE);
        target.connected.set(input.getConnected() ? IMConstants.TRUE : IMConstants.FALSE);
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
        target.localSeq.set(Sequence.create(SignGenerator.next()));
        return target;
    }

}
