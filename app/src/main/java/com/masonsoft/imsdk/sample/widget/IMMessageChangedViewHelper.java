package com.masonsoft.imsdk.sample.widget;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.util.Pair;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMMessageManager;
import com.masonsoft.imsdk.core.observable.MessageObservable;
import com.masonsoft.imsdk.lang.ObjectWrapper;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.util.Objects;

import io.github.idonans.core.thread.Threads;
import io.github.idonans.core.util.Preconditions;
import io.github.idonans.lang.DisposableHolder;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public abstract class IMMessageChangedViewHelper {

    private final DisposableHolder mRequestHolder = new DisposableHolder();

    private long mSessionUserId = Long.MIN_VALUE / 2;
    private int mConversationType = Integer.MIN_VALUE / 2;
    private long mTargetUserId = Long.MIN_VALUE / 2;
    private long mLocalMessageId = Long.MIN_VALUE / 2;

    public IMMessageChangedViewHelper() {
        MessageObservable.DEFAULT.registerObserver(mMessageObserver);
    }

    public void setMessage(@NonNull IMMessage message) {
        if (message._sessionUserId.isUnset()) {
            SampleLog.e(new IllegalArgumentException("unexpected message._sessionUserId.isUnset()"));
            return;
        }
        if (message._conversationType.isUnset()) {
            SampleLog.e(new IllegalArgumentException("unexpected message._conversationType.isUnset()"));
            return;
        }
        if (message._targetUserId.isUnset()) {
            SampleLog.e(new IllegalArgumentException("unexpected message._targetUserId.isUnset()"));
            return;
        }
        if (message.id.isUnset()) {
            SampleLog.e(new IllegalArgumentException("unexpected message.id.isUnset()"));
            return;
        }
        setMessage(message._sessionUserId.get(),
                message._conversationType.get(),
                message._targetUserId.get(),
                message.id.get());
    }

    public void setMessage(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        if (mSessionUserId != sessionUserId
                || mConversationType != conversationType
                || mTargetUserId != targetUserId
                || mLocalMessageId != localMessageId) {
            mSessionUserId = sessionUserId;
            mConversationType = conversationType;
            mTargetUserId = targetUserId;
            mLocalMessageId = localMessageId;
            requestLoadData(true);
        }
    }

    public String getDebugString() {
        //noinspection StringBufferReplaceableByString
        final StringBuilder builder = new StringBuilder();
        builder.append(Objects.defaultObjectTag(this));
        builder.append(" sessionUserId:").append(this.mSessionUserId);
        builder.append(" conversationType:").append(this.mConversationType);
        builder.append(" targetUserId:").append(this.mTargetUserId);
        builder.append(" localMessageId:").append(this.mLocalMessageId);
        return builder.toString();
    }

    public long getSessionUserId() {
        return mSessionUserId;
    }

    public int getConversationType() {
        return mConversationType;
    }

    public long getTargetUserId() {
        return mTargetUserId;
    }

    public long getLocalMessageId() {
        return mLocalMessageId;
    }

    public void requestLoadData(boolean reset) {
        // abort last
        mRequestHolder.set(null);

        if (reset) {
            onMessageChanged(null, null);
        }
        mRequestHolder.set(Single.just("")
                .map(input -> {
                    final IMMessage imMessage = IMMessageManager.getInstance().getMessage(
                            mSessionUserId,
                            mConversationType,
                            mTargetUserId,
                            mLocalMessageId
                    );
                    return new ObjectWrapper(imMessage);
                })
                .map(input -> {
                    final Object customObject = loadCustomObject();
                    return Pair.create(input, customObject);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pair -> {
                    Preconditions.checkNotNull(pair.first);
                    onMessageChanged((IMMessage) pair.first.getObject(), pair.second);
                }, SampleLog::e));
    }

    @Nullable
    @WorkerThread
    protected Object loadCustomObject() {
        return null;
    }

    protected abstract void onMessageChanged(@Nullable IMMessage message, @Nullable Object customObject);

    @SuppressWarnings("FieldCanBeLocal")
    private final MessageObservable.MessageObserver mMessageObserver = new MessageObservable.MessageObserver() {

        private boolean notMatch(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
            return (sessionUserId != IMConstants.ID_ANY && mSessionUserId != sessionUserId)
                    || (conversationType != IMConstants.ID_ANY && mConversationType != conversationType)
                    || (targetUserId != IMConstants.ID_ANY && mTargetUserId != targetUserId)
                    || (localMessageId != IMConstants.ID_ANY && mLocalMessageId != localMessageId);
        }

        @Override
        public void onMessageChanged(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
            if (notMatch(sessionUserId, conversationType, targetUserId, localMessageId)) {
                return;
            }

            Threads.postUi(() -> onMessageChangedInternal(sessionUserId, conversationType, targetUserId, localMessageId));
        }

        @Override
        public void onMessageCreated(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
            if (notMatch(sessionUserId, conversationType, targetUserId, localMessageId)) {
                return;
            }

            Threads.postUi(() -> onMessageChangedInternal(sessionUserId, conversationType, targetUserId, localMessageId));
        }

        @Override
        public void onMessageBlockChanged(long sessionUserId, int conversationType, long targetUserId, long fromBlockId, long toBlockId) {
            // ignore
        }

        @Override
        public void onMultiMessageChanged(long sessionUserId) {
            if (notMatch(sessionUserId, IMConstants.ID_ANY, IMConstants.ID_ANY, IMConstants.ID_ANY)) {
                return;
            }

            Threads.postUi(() -> onMessageChangedInternal(sessionUserId, IMConstants.ID_ANY, IMConstants.ID_ANY, IMConstants.ID_ANY));
        }

        private void onMessageChangedInternal(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
            if (notMatch(sessionUserId, conversationType, targetUserId, localMessageId)) {
                return;
            }

            requestLoadData(false);
        }
    };

}
