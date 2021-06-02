package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.lang.GeneralResult;

/**
 * @since 1.0
 */
public class MSIMSdkListenerProxy extends AutoRemoveDuplicateRunnable implements MSIMSdkListener {

    @Nullable
    private final MSIMSdkListener mOut;

    private final Object mConnectStateTag = new Object();
    private final Object mSignInStateTag = new Object();
    private final Object mSignOutStateTag = new Object();

    public MSIMSdkListenerProxy(@Nullable MSIMSdkListener listener) {
        this(listener, false);
    }

    public MSIMSdkListenerProxy(@Nullable MSIMSdkListener listener, boolean runOnUiThread) {
        super(runOnUiThread);
        mOut = listener;
    }

    @Override
    public void onConnecting() {
        final Object tag = getOnConnectingTag();
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onConnecting();
            }
        });
    }

    @Nullable
    protected Object getOnConnectingTag() {
        return mConnectStateTag;
    }

    @Override
    public void onConnectSuccess() {
        final Object tag = getOnConnectSuccessTag();
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onConnectSuccess();
            }
        });
    }

    @Nullable
    protected Object getOnConnectSuccessTag() {
        return mConnectStateTag;
    }

    @Override
    public void onConnectClosed() {
        final Object tag = getOnConnectClosedTag();
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onConnectClosed();
            }
        });
    }

    @Nullable
    protected Object getOnConnectClosedTag() {
        return mConnectStateTag;
    }

    @Override
    public void onSigningIn() {
        final Object tag = getOnSigningInTag();
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onSigningIn();
            }
        });
    }

    @Nullable
    protected Object getOnSigningInTag() {
        return mSignInStateTag;
    }

    @Override
    public void onSignInSuccess() {
        final Object tag = getOnSignInSuccessTag();
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onSignInSuccess();
            }
        });
    }

    @Nullable
    protected Object getOnSignInSuccessTag() {
        return mSignInStateTag;
    }

    @Override
    public void onSignInFail(@NonNull GeneralResult result) {
        final Object tag = getOnSignInFailTag(result);
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onSignInFail(result);
            }
        });
    }

    @Nullable
    protected Object getOnSignInFailTag(@NonNull GeneralResult result) {
        return mSignInStateTag;
    }

    @Override
    public void onKickedOffline() {
        final Object tag = getOnKickedOfflineTag();
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onKickedOffline();
            }
        });
    }

    @Nullable
    protected Object getOnKickedOfflineTag() {
        return null;
    }

    @Override
    public void onTokenExpired() {
        final Object tag = getOnTokenExpiredTag();
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onTokenExpired();
            }
        });
    }

    @Nullable
    protected Object getOnTokenExpiredTag() {
        return null;
    }


    @Override
    public void onSigningOut() {
        final Object tag = getOnSigningOutTag();
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onSigningOut();
            }
        });
    }

    @Nullable
    protected Object getOnSigningOutTag() {
        return mSignOutStateTag;
    }

    @Override
    public void onSignOutSuccess() {
        final Object tag = getOnSignOutSuccessTag();
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onSignOutSuccess();
            }
        });
    }

    @Nullable
    protected Object getOnSignOutSuccessTag() {
        return mSignOutStateTag;
    }

    @Override
    public void onSignOutFail(@NonNull GeneralResult result) {
        final Object tag = getOnSignOutFailTag(result);
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onSignOutFail(result);
            }
        });
    }

    @Nullable
    protected Object getOnSignOutFailTag(@NonNull GeneralResult result) {
        return mSignOutStateTag;
    }

}
