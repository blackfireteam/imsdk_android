package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMConversation;
import com.masonsoft.imsdk.core.IMConversationManager;
import com.masonsoft.imsdk.core.IMMessageQueueManager;
import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.core.db.TinyPage;
import com.masonsoft.imsdk.lang.GeneralResult;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.util.Preconditions;

/**
 * @since 1.0
 */
public class MSIMConversationManager {

    private static final Singleton<MSIMConversationManager> INSTANCE = new Singleton<MSIMConversationManager>() {
        @Override
        protected MSIMConversationManager create() {
            return new MSIMConversationManager();
        }
    };

    static MSIMConversationManager getInstance() {
        return INSTANCE.get();
    }

    public void deleteConversation(MSIMConversation conversation) {
        deleteConversation(conversation, null);
    }

    public void deleteConversation(MSIMConversation conversation, @Nullable MSIMCallback<GeneralResult> callback) {
        IMMessageQueueManager.getInstance().enqueueDeleteConversationActionMessage(conversation.getConversation(), callback);
    }

    @Nullable
    public MSIMConversation getConversation(final long conversationId) {
        final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
        if (sessionUserId <= 0) {
            return null;
        }

        final IMConversation conversation = IMConversationManager.getInstance().getConversation(sessionUserId, conversationId);
        if (conversation == null) {
            return null;
        }

        return new MSIMConversation(conversation);
    }

    @Nullable
    public MSIMConversation getConversationByTargetUserId(final long targetUserId) {
        final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
        if (sessionUserId <= 0) {
            return null;
        }

        final IMConversation conversation = IMConversationManager.getInstance().getConversationByTargetUserId(
                sessionUserId,
                IMConstants.ConversationType.C2C,
                targetUserId
        );
        if (conversation == null) {
            return null;
        }
        return new MSIMConversation(conversation);
    }

    public int getAllUnreadCount() {
        final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
        if (sessionUserId <= 0) {
            return 0;
        }
        return IMConversationManager.getInstance().getAllUnreadCount(sessionUserId);
    }

    @NonNull
    public TinyPage<MSIMConversation> pageQueryConversation(final long seq, final int limit) {
        final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
        if (sessionUserId <= 0) {
            return new TinyPage<>();
        }

        final TinyPage<IMConversation> page = IMConversationManager.getInstance().pageQueryConversation(
                sessionUserId,
                seq,
                limit,
                IMConstants.ConversationType.C2C
        );
        return page.transform(conversation -> {
            Preconditions.checkNotNull(conversation);
            return new MSIMConversation(conversation);
        });
    }

}
