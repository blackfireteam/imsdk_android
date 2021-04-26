package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.IMConversation;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMConversationManager;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.db.Conversation;
import com.masonsoft.imsdk.core.db.ConversationDatabaseProvider;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.lang.NotNullProcessor;

/**
 * 处理单条 LastReadMsg 消息
 *
 * @since 1.0
 */
public class TinyLastReadMsgProcessor extends NotNullProcessor<ProtoMessage.LastReadMsg> {

    private final long mSessionUserId;

    public TinyLastReadMsgProcessor(long sessionUserId) {
        mSessionUserId = sessionUserId;
    }

    @Override
    protected boolean doNotNullProcess(@NonNull ProtoMessage.LastReadMsg target) {
        final long targetUserId = target.getFromUid();
        final long unread = target.getUnread();
        // 纳秒
        final long updateTime = target.getUpdateTime();
        final long msgId = target.getMsgId();
        final int conversationType = IMConstants.ConversationType.C2C;

        if (targetUserId == mSessionUserId) {
            IMLog.e("unexpected. targetUserId:%s is same as sessionUserId:%s", targetUserId, mSessionUserId);
            return false;
        }

        final IMConversation conversation = IMConversationManager.getInstance().getOrCreateConversationByTargetUserId(
                mSessionUserId,
                conversationType,
                targetUserId
        );
        if (conversation.id.isUnset()) {
            IMLog.e("unexpected. conversation id is unset");
            return false;
        }

        final Conversation conversationUpdate = new Conversation();
        conversationUpdate.localId.set(conversation.id.get());
        if (msgId > 0) {
            // 被动方
            conversationUpdate.remoteMessageLastRead.set(msgId);
        } else {
            // 主动方
            conversationUpdate.remoteUnread.set(unread);
            conversationUpdate.localUnreadCount.set(unread);
        }

        ConversationDatabaseProvider.getInstance().updateConversation(mSessionUserId, conversationUpdate);
        return true;
    }

}
