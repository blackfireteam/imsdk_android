package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.FetchMessageHistoryManager;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.core.db.Conversation;
import com.masonsoft.imsdk.core.db.ConversationDatabaseProvider;
import com.masonsoft.imsdk.core.db.ConversationFactory;
import com.masonsoft.imsdk.core.db.DatabaseHelper;
import com.masonsoft.imsdk.core.db.DatabaseProvider;
import com.masonsoft.imsdk.core.db.DatabaseSessionWriteLock;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.observable.FetchConversationListObservable;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.user.UserInfoSyncManager;
import com.masonsoft.imsdk.util.Objects;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析收到的批量会话列表
 *
 * @since 1.0
 */
public class ReceivedProtoMessageConversationListProcessor extends ReceivedProtoMessageProtoTypeProcessor<ProtoMessage.ChatList> {

    public ReceivedProtoMessageConversationListProcessor() {
        super(ProtoMessage.ChatList.class);
    }

    @Override
    protected boolean doNotNullProtoMessageObjectProcess(@NonNull SessionProtoByteMessageWrapper target, @NonNull ProtoMessage.ChatList protoMessageObject) {
        final long sessionUserId = target.getSessionUserId();
        final long updateTime = protoMessageObject.getUpdateTime();
        final List<ProtoMessage.ChatItem> chatItemList = protoMessageObject.getChatItemsList();
        final List<Conversation> conversationList = new ArrayList<>();
        if (chatItemList != null) {
            for (ProtoMessage.ChatItem item : chatItemList) {
                conversationList.add(ConversationFactory.create(item));
            }
        }
        IMLog.v(Objects.defaultObjectTag(this) + " received conversation list size:%s, sessionUserId:%s, updateTime:%s",
                conversationList.size(), sessionUserId, updateTime);

        if (!conversationList.isEmpty()) {
            syncConversationUserInfo(conversationList);
            updateConversationList(sessionUserId, conversationList);
            syncLastMessages(sessionUserId, conversationList);
        }

        if (updateTime > 0) {
            // 会话获取结束
            target.getSessionTcpClient().setFetchConversationListFinish(updateTime);
            FetchConversationListObservable.DEFAULT.notifyConversationListFetchedSuccess();
        }
        return true;
    }

    private void syncConversationUserInfo(@NonNull final List<Conversation> conversationList) {
        final List<Long> userIdList = new ArrayList<>();
        for (Conversation conversation : conversationList) {
            userIdList.add(conversation.targetUserId.get());
        }
        UserInfoSyncManager.getInstance().enqueueSyncUserInfoList(userIdList);
    }

    private void updateConversationList(final long sessionUserId, @NonNull final List<Conversation> conversationList) {
        final DatabaseHelper databaseHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
        synchronized (DatabaseSessionWriteLock.getInstance().getSessionWriteLock(databaseHelper)) {
            for (Conversation conversation : conversationList) {
                conversation.applyLogicField(sessionUserId);
                final long targetUserId = conversation.targetUserId.get();
                final int conversationType = IMConstants.ConversationType.C2C;

                // 设置会话的类型
                conversation.localConversationType.set(conversationType);

                final Conversation dbConversation = ConversationDatabaseProvider
                        .getInstance()
                        .getConversationByTargetUserId(sessionUserId, conversationType, targetUserId);

                if (dbConversation == null) {
                    // 会话在本地不存在
                    if (!ConversationDatabaseProvider.getInstance().insertConversation(sessionUserId, conversation)) {
                        final Throwable e = new IllegalStateException("unexpected insertConversation return false " + conversation);
                        IMLog.e(e);
                    }
                } else {
                    // 会话已经存在
                    // 回写 localId
                    conversation.localId.set(dbConversation.localId.get());
                    if (!ConversationDatabaseProvider.getInstance().updateConversation(
                            sessionUserId,
                            conversation)) {
                        final Throwable e = new IllegalStateException("unexpected updateConversation return false " + conversation);
                        IMLog.e(e);
                    }
                }
            }
        }
    }

    /**
     * 同步会话的最后一页消息数据
     */
    private void syncLastMessages(final long sessionUserId, @NonNull final List<Conversation> conversationList) {
        for (Conversation conversation : conversationList) {
            if (!conversation.localId.isUnset()) {
                FetchMessageHistoryManager.getInstance().enqueueFetchMessageHistory(
                        sessionUserId,
                        SignGenerator.nextSign(),
                        conversation.localConversationType.get(),
                        conversation.targetUserId.get(),
                        0,
                        true
                );
            }
        }
    }

}
