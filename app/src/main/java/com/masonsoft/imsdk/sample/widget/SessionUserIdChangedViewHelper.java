package com.masonsoft.imsdk.sample.widget;

import com.masonsoft.imsdk.MSIMManager;
import com.masonsoft.imsdk.MSIMSessionListener;
import com.masonsoft.imsdk.MSIMSessionListenerProxy;

public abstract class SessionUserIdChangedViewHelper {

    private long mSessionUserId;

    public SessionUserIdChangedViewHelper() {
        mSessionUserId = MSIMManager.getInstance().getSessionUserId();
        MSIMManager.getInstance().addSessionListener(mSessionListener);
    }

    public long getSessionUserId() {
        return mSessionUserId;
    }

    protected abstract void onSessionUserIdChanged(long sessionUserId);

    @SuppressWarnings("FieldCanBeLocal")
    private final MSIMSessionListener mSessionListener = new MSIMSessionListenerProxy(new MSIMSessionListener() {
        @Override
        public void onSessionChanged() {
            sync();
        }

        @Override
        public void onSessionUserIdChanged() {
            sync();
        }

        private void sync() {
            final long sessionUserId = MSIMManager.getInstance().getSessionUserId();
            if (mSessionUserId != sessionUserId) {
                mSessionUserId = sessionUserId;
                SessionUserIdChangedViewHelper.this.onSessionUserIdChanged(sessionUserId);
            }
        }
    }, true);

}
