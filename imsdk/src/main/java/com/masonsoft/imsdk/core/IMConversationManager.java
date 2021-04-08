package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.core.Singleton;
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
            RuntimeMode.throwIfDebug(e);
            // fallback
            return IMConversationFactory.create(insertConversation);
        }
    }

    public void updateConversationLastMessage(final long sessionUserId,
                                              final int conversationType,
                                              final long targetUserId,
                                              final long localMessageId) {
        final IMConversation imConversation = getOrCreateConversationByTargetUserId(
                sessionUserId, conversationType, targetUserId);
        if (imConversation.id.isUnset()) {
            final Throwable e = new IllegalAccessError("unexpected. conversation's id is unset");
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
            return;
        }
        if (imConversation.id.get() <= 0) {
            final Throwable e = new IllegalAccessError("unexpected. conversation's id is invalid " + imConversation.id.get());
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
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
            if (useNewShowMessageId) {
                // 更新 conversation 的 showMessageId 为 localMessageId
                final Conversation conversationUpdate = new Conversation();
                conversationUpdate.localId.set(imConversation.id.get());
                conversationUpdate.localShowMessageId.set(localMessageId);
                conversationUpdate.localTimeMs.set(newShowMessage.localTimeMs.get());
                conversationUpdate.localSeq.set(newShowMessage.localSeq.get());
                ConversationDatabaseProvider.getInstance().updateConversation(sessionUserId, conversationUpdate);
            }
        }
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
