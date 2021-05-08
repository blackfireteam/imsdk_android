package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.IMConversation;
import com.masonsoft.imsdk.IMConversationFactory;
import com.masonsoft.imsdk.core.db.Conversation;
import com.masonsoft.imsdk.core.db.ConversationDatabaseProvider;
import com.masonsoft.imsdk.core.db.ConversationFactory;
import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageDatabaseProvider;
import com.masonsoft.imsdk.core.db.TinyPage;
import com.masonsoft.imsdk.user.UserInfoSyncManager;
import com.masonsoft.imsdk.util.Objects;

import java.util.ArrayList;
import java.util.List;

import io.github.idonans.core.Singleton;

/**
 * 处理会话相关内容
 *
 * @since 1.0
 */
public class IMConversationManager {

    private static final Singleton<IMConversationManager> INSTANCE = new Singleton<IMConversationManager>() {
        @Override
        protected IMConversationManager create() {
            return new IMConversationManager();
        }
    };

    @NonNull
    public static IMConversationManager getInstance() {
        IMProcessValidator.validateProcess();

        return INSTANCE.get();
    }

    private IMConversationManager() {
    }

    @Nullable
    public IMConversation getConversation(
            final long sessionUserId,
            final long conversationId) {
        if (conversationId == IMConstants.ID_ANY) {
            IMLog.v("getConversation fast return null, sessionUserId:%s, conversationId is id any.", sessionUserId);
            return null;
        }

        final Conversation conversation = ConversationDatabaseProvider.getInstance()
                .getConversation(sessionUserId, conversationId);
        if (conversation != null) {
            return IMConversationFactory.create(conversation);
        }
        return null;
    }

    @Nullable
    public IMConversation getConversationByTargetUserId(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId) {
        if (conversationType == IMConstants.ID_ANY) {
            IMLog.v("getConversationByTargetUserId fast return null, sessionUserId:%s, conversationType is id any, targetUserId:%s.", sessionUserId, targetUserId);
            return null;
        }
        if (targetUserId == IMConstants.ID_ANY) {
            IMLog.v("getConversationByTargetUserId fast return null, sessionUserId:%s, conversationType:%s, targetUserId is id any.", sessionUserId, conversationType);
            return null;
        }

        final Conversation conversation = ConversationDatabaseProvider.getInstance()
                .getConversationByTargetUserId(
                        sessionUserId,
                        conversationType,
                        targetUserId);
        if (conversation != null) {
            return IMConversationFactory.create(conversation);
        }
        return null;
    }

    @NonNull
    public IMConversation getOrCreateConversationByTargetUserId(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId) {
        final Conversation conversation = ConversationDatabaseProvider.getInstance()
                .getConversationByTargetUserId(
                        sessionUserId,
                        conversationType,
                        targetUserId);
        if (conversation != null) {
            return IMConversationFactory.create(conversation);
        } else {
            // try create conversation
            Conversation targetConversation = null;

            final Conversation insertConversation = ConversationFactory.createEmptyConversation(
                    conversationType,
                    targetUserId
            );
            if (ConversationDatabaseProvider.getInstance().insertConversation(
                    sessionUserId,
                    insertConversation)) {
                final long conversationId = insertConversation.localId.get();
                targetConversation = ConversationDatabaseProvider.getInstance().getConversation(
                        sessionUserId,
                        conversationId
                );
            } else {
                // insert conversation fail, try read again.
                targetConversation = ConversationDatabaseProvider.getInstance()
                        .getConversationByTargetUserId(
                                sessionUserId,
                                conversationType,
                                targetUserId
                        );
                IMLog.v("fail to insert conversation, try read again result %s", targetConversation);
            }

            if (targetConversation != null) {
                return IMConversationFactory.create(targetConversation);
            }

            final Throwable e = new IllegalAccessError("unexpected. targetConversation is null.");
            IMLog.e(e, "sessionUserId:%s, conversationType:%s, targetUserId:%s",
                    sessionUserId, conversationType, targetUserId);
            RuntimeMode.fixme(e);
            // fallback
            return IMConversationFactory.create(insertConversation);
        }
    }

    /**
     * 获取所有会话的未读消息数
     */
    public int getAllUnreadCount(final long sessionUserId) {
        return ConversationDatabaseProvider.getInstance().getAllUnreadCount(sessionUserId);
    }

    /**
     * 如果是对方发送的新消息，则累加未读消息数
     */
    public boolean increaseConversationUnreadCount(final long sessionUserId,
                                                   final int conversationType,
                                                   final long targetUserId,
                                                   final Message message) {
        if (message == null) {
            final Throwable e = new IllegalAccessError("unexpected. message is null");
            IMLog.e(e);
            RuntimeMode.fixme(e);
            return false;
        }

        if (message.localId.isUnset()) {
            final Throwable e = new IllegalAccessError("unexpected. message's localId is unset");
            IMLog.e(e);
            RuntimeMode.fixme(e);
            return false;
        }

        if (message.messageType.isUnset()) {
            final Throwable e = new IllegalAccessError("unexpected. message's messageType is unset");
            IMLog.e(e);
            RuntimeMode.fixme(e);
            return false;
        }

        // 指令消息不影响未读消息数
        if (IMConstants.MessageType.isActionMessage(message.messageType.get())) {
            IMLog.v("ignore. message's messageType:%s is action message", message.messageType.get());
            return false;
        }

        if (message.remoteMessageId.isUnset()) {
            IMLog.v("ignore. message's remoteMessageId is unset");
            return false;
        }

        if (message.fromUserId.isUnset()) {
            IMLog.v("ignore. message's fromUserId is unset");
            return false;
        }

        if (message.fromUserId.get() == sessionUserId) {
            IMLog.v("ignore. message's fromUserId is sessionUserId");
            return false;
        }

        final Conversation conversation = ConversationDatabaseProvider.getInstance()
                .getConversationByTargetUserId(
                        sessionUserId,
                        conversationType,
                        targetUserId);
        if (conversation == null) {
            final Throwable e = new IllegalAccessError("unexpected. conversation is null");
            IMLog.e(e);
            RuntimeMode.fixme(e);
            return false;
        }

        // 累加消息未读数
        final Conversation conversationUpdate = new Conversation();
        conversationUpdate.localId.set(conversation.localId.get());
        conversationUpdate.localUnreadCount.set(conversation.localUnreadCount.get() + 1);
        return ConversationDatabaseProvider.getInstance().updateConversation(sessionUserId, conversationUpdate);
    }

    /**
     * 如果 localMessageId 比当前会话上已经记录的 showMessageId 的 seq 更大，则将 showMessageId 替换为 localMessageId
     *
     * @param sessionUserId
     * @param conversationType
     * @param targetUserId
     * @param localMessageId
     */
    public void updateConversationLastMessage(final long sessionUserId,
                                              final int conversationType,
                                              final long targetUserId,
                                              final long localMessageId) {
        final IMConversation imConversation = getOrCreateConversationByTargetUserId(
                sessionUserId, conversationType, targetUserId);
        if (imConversation.id.isUnset()) {
            final Throwable e = new IllegalAccessError("unexpected. conversation's id is unset");
            IMLog.e(e);
            RuntimeMode.fixme(e);
            return;
        }
        if (imConversation.id.get() <= 0) {
            final Throwable e = new IllegalAccessError("unexpected. conversation's id is invalid " + imConversation.id.get());
            IMLog.e(e);
            RuntimeMode.fixme(e);
            return;
        }

        // 判断是否需要将 conversation 的 showMessageId 更改为 localMessageId
        Message oldShowMessage = null;
        Message newShowMessage = null;

        if (!imConversation.showMessageId.isUnset()) {
            oldShowMessage = MessageDatabaseProvider.getInstance().getMessage(
                    sessionUserId, conversationType, targetUserId, imConversation.showMessageId.get());
        }
        newShowMessage = MessageDatabaseProvider.getInstance().getMessage(
                sessionUserId, conversationType, targetUserId, localMessageId);

        if (newShowMessage != null) {
            boolean useNewShowMessageId = true;
            if (oldShowMessage != null) {
                // showMessageId 对应的 seq 不小于 localMessageId 的 seq
                if (oldShowMessage.localSeq.get() >= newShowMessage.localSeq.get()) {
                    useNewShowMessageId = false;
                }
            }

            if (newShowMessage.localActionMessage.get() > 0) {
                // 指令消息不能作为会话的 showMessageId
                useNewShowMessageId = false;
            }

            if (useNewShowMessageId) {
                // 更新 conversation 的 showMessageId 为 localMessageId
                final Conversation conversationUpdate = new Conversation();
                conversationUpdate.localId.set(imConversation.id.get());
                conversationUpdate.localShowMessageId.set(localMessageId);
                conversationUpdate.localTimeMs.set(newShowMessage.localTimeMs.get());
                conversationUpdate.localSeq.set(newShowMessage.localSeq.get());
                if (!ConversationDatabaseProvider.getInstance().updateConversation(sessionUserId, conversationUpdate)) {
                    final Throwable e = new IllegalAccessError("unexpected. updateConversation return false");
                    IMLog.e(e, "sessionUserId:%s, conversationType:%s, targetUserId:%s, localMessageId:%s",
                            sessionUserId,
                            conversationType,
                            targetUserId,
                            localMessageId);
                    RuntimeMode.fixme(e);
                }
                return;
            }
        }

        IMLog.v("updateConversationLastMessage ignore sessionUserId:%s, conversationType:%s, targetUserId:%s, localMessageId:%s",
                sessionUserId,
                conversationType,
                targetUserId,
                localMessageId);
    }

    @NonNull
    public TinyPage<IMConversation> pageQueryConversation(final long sessionUserId,
                                                          final long seq,
                                                          final int limit,
                                                          final int conversationType) {
        if (seq == 0) {
            // 读取第一页消息时，尝试同步用户信息
            UserInfoSyncManager.getInstance().enqueueSyncUserInfo(sessionUserId);
        }

        final TinyPage<Conversation> page = ConversationDatabaseProvider.getInstance().pageQueryConversation(
                sessionUserId, seq, limit, conversationType, false, null);

        final List<IMConversation> filterItems = new ArrayList<>();
        for (Conversation item : page.items) {
            filterItems.add(IMConversationFactory.create(item));
        }

        final TinyPage<IMConversation> result = new TinyPage<>();
        result.items = filterItems;
        result.hasMore = page.hasMore;
        if (filterItems.size() < page.items.size()) {
            result.hasMore = false;
        }

        IMLog.v(Objects.defaultObjectTag(this) + " pageQueryConversation result:%s with sessionUserId:%s, seq:%s, limit:%s, conversationType:%s",
                result, sessionUserId, seq, limit, conversationType);
        return result;
    }

}
