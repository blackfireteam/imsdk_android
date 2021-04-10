package com.masonsoft.imsdk.sample.widget;

import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.core.observable.SessionObservable;

import io.github.idonans.core.thread.Threads;

public abstract class SessionUserIdChangedViewHelper {

    private long mSessionUserId;

    public SessionUserIdChangedViewHelper() {
        mSessionUserId = IMSessionManager.getInstance().getSessionUserId();

        SessionObservable.DEFAULT.registerObserver(mSessionObserver);
    }

    public long getSessionUserId() {
        return mSessionUserId;
    }

    protected abstract void onSessionUserIdChanged(long sessionUserId);

    @SuppressWarnings("FieldCanBeLocal")
    private final SessionObservable.SessionObserver mSessionObserver = new SessionObservable.SessionObserver() {
        @Override
        public void onSessionChanged() {
            sync();
        }

        @Override
        public void onSessionUserIdChanged() {
            sync();
        }

        private void sync() {
            Threads.postUi(() -> {
                final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
                if (mSessionUserId != sessionUserId) {
                    mSessionUserId = sessionUserId;
                    SessionUserIdChangedViewHelper.this.onSessionUserIdChanged(sessionUserId);
                }
            });
        }
    };

}
