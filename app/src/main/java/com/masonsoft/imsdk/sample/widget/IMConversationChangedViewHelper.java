package com.masonsoft.imsdk.sample.widget;

import androidx.annotation.Nullable;

import com.masonsoft.imsdk.IMConversation;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMConversationManager;
import com.masonsoft.imsdk.core.observable.ConversationObservable;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.common.ObjectWrapper;

import io.github.idonans.core.thread.Threads;
import io.github.idonans.lang.DisposableHolder;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public abstract class IMConversationChangedViewHelper {

    private final DisposableHolder mRequestHolder = new DisposableHolder();

    private long mSessionUserId = Long.MIN_VALUE / 2;
    private long mConversationId = Long.MIN_VALUE / 2;

    public IMConversationChangedViewHelper() {
        ConversationObservable.DEFAULT.registerObserver(mConversationObserver);
    }

    public void setConversation(long sessionUserId, long conversationId) {
        if (mSessionUserId != sessionUserId
                || mConversationId != conversationId) {
            mSessionUserId = sessionUserId;
            mConversationId = conversationId;
            requestLoadData(true);
        }
    }

    public long getSessionUserId() {
        return mSessionUserId;
    }

    public long getConversationId() {
        return mConversationId;
    }

    private void requestLoadData(boolean reset) {
        // abort last
        mRequestHolder.set(null);

        if (reset) {
            onConversationChanged(null);
        }
        mRequestHolder.set(Single.fromCallable(
                () -> {
                    final IMConversation conversation = IMConversationManager.getInstance().getConversation(
                            mSessionUserId,
                            mConversationId
                    );
                    return new ObjectWrapper(conversation);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(objectWrapper -> onConversationChanged((IMConversation) objectWrapper.getObject()), SampleLog::e));
    }

    protected abstract void onConversationChanged(@Nullable IMConversation conversation);

    @SuppressWarnings("FieldCanBeLocal")
    private final ConversationObservable.ConversationObserver mConversationObserver = new ConversationObservable.ConversationObserver() {

        private boolean notMatch(long sessionUserId, long conversationId) {
            return (sessionUserId != IMConstants.ID_ANY && mSessionUserId != sessionUserId)
                    || (conversationId != IMConstants.ID_ANY && mConversationId != conversationId);
        }

        @Override
        public void onConversationChanged(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
            if (notMatch(sessionUserId, conversationId)) {
                return;
            }

            Threads.postUi(() -> onConversationChangedInternal(sessionUserId, conversationId));
        }

        @Override
        public void onConversationCreated(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
            Threads.postUi(() -> onConversationChangedInternal(sessionUserId, conversationId));
        }

        private void onConversationChangedInternal(long sessionUserId, long conversationId) {
            if (notMatch(sessionUserId, conversationId)) {
                return;
            }

            requestLoadData(false);
        }
    };

}
