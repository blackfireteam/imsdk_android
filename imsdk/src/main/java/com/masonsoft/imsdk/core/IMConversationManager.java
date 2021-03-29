package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.core.Singleton;
import com.masonsoft.imsdk.IMConversation;
import com.masonsoft.imsdk.IMConversationFactory;
import com.masonsoft.imsdk.core.db.Conversation;
import com.masonsoft.imsdk.core.db.ConversationDatabaseProvider;
import com.masonsoft.imsdk.core.db.ConversationFactory;

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

}
