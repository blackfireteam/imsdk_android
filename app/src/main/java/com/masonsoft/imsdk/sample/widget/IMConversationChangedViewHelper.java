package com.masonsoft.imsdk.sample.widget;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.util.Pair;

import com.masonsoft.imsdk.IMConversation;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMConversationManager;
import com.masonsoft.imsdk.core.observable.ConversationObservable;
import com.masonsoft.imsdk.lang.ObjectWrapper;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.util.Objects;

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

    public String getDebugString() {
        //noinspection StringBufferReplaceableByString
        final StringBuilder builder = new StringBuilder();
        builder.append(Objects.defaultObjectTag(this));
        builder.append(" sessionUserId:").append(this.mSessionUserId);
        builder.append(" conversationId:").append(this.mConversationId);
        return builder.toString();
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
            onConversationChanged(null, null);
        }
        mRequestHolder.set(Single.just("")
                .map(input -> {
                    final IMConversation conversation = IMConversationManager.getInstance().getConversation(
                            mSessionUserId,
                            mConversationId
                    );
                    return new ObjectWrapper(conversation);
                })
                .map(input -> {
                    final Object customObject = loadCustomObject();
                    return Pair.create(input, customObject);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pair -> onConversationChanged((IMConversation) pair.first.getObject(), pair.second), SampleLog::e));
    }

    @Nullable
    @WorkerThread
    protected Object loadCustomObject() {
        return null;
    }

    protected abstract void onConversationChanged(@Nullable IMConversation conversation, @Nullable Object customObject);

    @SuppressWarnings("FieldCanBeLocal")
    private final ConversationObservable.ConversationObserver mConversationObserver = new ConversationObservable.ConversationObserver() {

        private boolean notMatch(long sessionUserId, long conversationId) {
            return !IMConstants.isIdMatch(mSessionUserId, sessionUserId)
                    || !IMConstants.isIdMatch(mConversationId, conversationId);
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
