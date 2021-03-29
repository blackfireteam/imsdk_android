package com.masonsoft.imsdk.sample.widget;

import androidx.annotation.Nullable;

import com.idonans.core.thread.Threads;
import com.idonans.lang.DisposableHolder;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.IMMessageFactory;
import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageDatabaseProvider;
import com.masonsoft.imsdk.core.observable.MessageObservable;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.common.ObjectWrapper;

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

    private void requestLoadData(boolean reset) {
        // abort last
        mRequestHolder.set(null);

        if (reset) {
            onMessageChanged(null);
        }
        mRequestHolder.set(Single.fromCallable(
                () -> {
                    final Message message = MessageDatabaseProvider.getInstance().getMessage(
                            mSessionUserId,
                            mConversationType,
                            mTargetUserId,
                            mLocalMessageId
                    );
                    IMMessage imMessage = null;
                    if (message != null) {
                        imMessage = IMMessageFactory.create(message);
                    }
                    return new ObjectWrapper(imMessage);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(objectWrapper -> onMessageChanged((IMMessage) objectWrapper.getObject()), SampleLog::e));
    }

    protected abstract void onMessageChanged(@Nullable IMMessage imMessage);

    @SuppressWarnings("FieldCanBeLocal")
    private final MessageObservable.MessageObserver mMessageObserver = new MessageObservable.MessageObserver() {
        @Override
        public void onMessageChanged(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
            onMessageChangedInternal(sessionUserId, conversationType, targetUserId, localMessageId);
        }

        @Override
        public void onMessageCreated(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
            onMessageChangedInternal(sessionUserId, conversationType, targetUserId, localMessageId);
        }

        private void onMessageChangedInternal(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
            if (mSessionUserId == sessionUserId
                    && mConversationType == conversationType
                    && mTargetUserId == targetUserId
                    && mLocalMessageId == localMessageId) {
                Threads.postUi(() -> {
                    requestLoadData(false);
                });
            }
        }
    };

}