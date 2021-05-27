package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.IMConversation;
import com.masonsoft.imsdk.core.IMConversationManager;
import com.masonsoft.imsdk.core.IMMessageQueueManager;
import com.masonsoft.imsdk.core.db.TinyPage;
import com.masonsoft.imsdk.core.observable.ConversationObservable;
import com.masonsoft.imsdk.lang.GeneralResult;
import com.masonsoft.imsdk.util.WeakObservable;

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

    @NonNull
    private final WeakObservable<MSIMConversationListener> mConversationListeners = new WeakObservable<>();
    @SuppressWarnings("FieldCanBeLocal")
    private final ConversationObservable.ConversationObserver mConversationObserver = new ConversationObservable.ConversationObserver() {
        @Override
        public void onConversationChanged(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
            mConversationListeners.forEach(listener -> {
                if (listener != null) {
                    listener.onConversationChanged(sessionUserId, conversationId, conversationType, targetUserId);
                }
            });
        }

        @Override
        public void onConversationCreated(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
            mConversationListeners.forEach(listener -> {
                if (listener != null) {
                    listener.onConversationCreated(sessionUserId, conversationId, conversationType, targetUserId);
                }
            });
        }
    };

    private MSIMConversationManager() {
        ConversationObservable.DEFAULT.registerObserver(mConversationObserver);
    }

    public void addConversationListener(@Nullable MSIMConversationListener listener) {
        if (listener != null) {
            mConversationListeners.registerObserver(listener);
        }
    }

    public void removeConversationListener(@Nullable MSIMConversationListener listener) {
        if (listener != null) {
            mConversationListeners.unregisterObserver(listener);
        }
    }

    public void deleteConversation(MSIMConversation conversation) {
        deleteConversation(conversation, null);
    }

    public void deleteConversation(MSIMConversation conversation, @Nullable MSIMCallback<GeneralResult> callback) {
        IMMessageQueueManager.getInstance().enqueueDeleteConversationActionMessage(conversation.getConversation(), callback);
    }

    @Nullable
    public MSIMConversation getConversation(final long sessionUserId, final long conversationId) {
        final IMConversation conversation = IMConversationManager.getInstance().getConversation(sessionUserId, conversationId);
        if (conversation == null) {
            return null;
        }

        return new MSIMConversation(conversation);
    }

    @Nullable
    public MSIMConversation getConversationByTargetUserId(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId) {
        final IMConversation conversation = IMConversationManager.getInstance().getConversationByTargetUserId(
                sessionUserId,
                conversationType,
                targetUserId
        );
        if (conversation == null) {
            return null;
        }
        return new MSIMConversation(conversation);
    }

    public int getAllUnreadCount(final long sessionUserId) {
        return IMConversationManager.getInstance().getAllUnreadCount(sessionUserId);
    }

    @NonNull
    public TinyPage<MSIMConversation> pageQueryConversation(
            final long sessionUserId,
            final long seq,
            final int limit,
            final int conversationType) {
        final TinyPage<IMConversation> page = IMConversationManager.getInstance().pageQueryConversation(
                sessionUserId,
                seq,
                limit,
                conversationType
        );
        return page.transform(conversation -> {
            Preconditions.checkNotNull(conversation);
            return new MSIMConversation(conversation);
        });
    }

}
