package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.FetchMessageHistoryManager;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.core.db.Conversation;
import com.masonsoft.imsdk.core.db.ConversationDatabaseProvider;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.user.UserInfoSyncManager;

/**
 * 处理单条 ChatItemUpdate 消息
 *
 * @since 1.0
 */
public class TinyConversationUpdateProcessor extends ReceivedProtoMessageProtoTypeProcessor<ProtoMessage.ChatItemUpdate> {

    // msg_last_read 变动
    private static final int EVENT_MSG_LAST_READ = 0;
    // unread 变动
    private static final int EVENT_UNREAD = 1;
    // i_block_u 变动
    private static final int EVENT_I_BLOCK_U = 2;
    // deleted 变动
    private static final int EVENT_DELETED = 3;

    public TinyConversationUpdateProcessor() {
        super(ProtoMessage.ChatItemUpdate.class);
    }

    @Override
    protected boolean doNotNullProtoMessageObjectProcess(@NonNull SessionProtoByteMessageWrapper target, @NonNull ProtoMessage.ChatItemUpdate protoMessageObject) {
        final long sessionUserId = target.getSessionUserId();
        final long event = protoMessageObject.getEvent();
        final long targetUserId = protoMessageObject.getUid();
        final int conversationType = IMConstants.ConversationType.C2C;

        // 获取用户信息
        UserInfoSyncManager.getInstance().enqueueSyncUserInfo(targetUserId);

        final Conversation dbConversation = ConversationDatabaseProvider
                .getInstance()
                .getConversationByTargetUserId(sessionUserId, conversationType, targetUserId);
        if (dbConversation == null) {
            // 会话在本地不存在，忽略.
            IMLog.w("conversation is not exists. ignore chat item update. sessionUserId:%s, targetUserId:%s", sessionUserId, targetUserId);
            return true;
        }
        final Conversation conversationUpdate = new Conversation();
        conversationUpdate.localId.set(dbConversation.localId.get());
        boolean fetchMessageHistory = false;
        if (event == EVENT_MSG_LAST_READ) {
            conversationUpdate.messageLastRead.set(protoMessageObject.getMsgLastRead());
        } else if (event == EVENT_UNREAD) {
            conversationUpdate.remoteUnread.set(protoMessageObject.getUnread());
            conversationUpdate.localUnreadCount.set(protoMessageObject.getUnread());
            fetchMessageHistory = true;
        } else if (event == EVENT_I_BLOCK_U) {
            conversationUpdate.iBlockU.set(IMConstants.trueOfFalse(protoMessageObject.getIBlockU()));
        } else if (event == EVENT_DELETED) {
            conversationUpdate.delete.set(IMConstants.trueOfFalse(protoMessageObject.getDeleted()));
        } else {
            // 未知的 event 值
            IMLog.e("unknown event:%s. ignore chat item update.", event);
            return true;
        }

        if (!ConversationDatabaseProvider.getInstance().updateConversation(
                sessionUserId,
                conversationUpdate)) {
            final Throwable e = new IllegalAccessError("unexpected updateConversation return false " + conversationUpdate);
            IMLog.e(e);
        }

        if (fetchMessageHistory) {
            // 获取会话的最后一页消息
            FetchMessageHistoryManager.getInstance().enqueueFetchMessageHistory(
                    sessionUserId,
                    SignGenerator.next(),
                    conversationType,
                    targetUserId,
                    0,
                    true
            );
        }

        return true;
    }

}
