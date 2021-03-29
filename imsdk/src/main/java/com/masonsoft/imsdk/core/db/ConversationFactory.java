package com.masonsoft.imsdk.core.db;

import com.masonsoft.imsdk.core.SignGenerator;

public class ConversationFactory {

    private ConversationFactory() {
    }

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
