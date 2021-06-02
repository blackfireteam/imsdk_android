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

    public MSIMSdkListenerProxy(@Nullable MSIMSdkListener listener) {
        this(listener, false);
    }

    public MSIMSdkListenerProxy(@Nullable MSIMSdkListener listener, boolean runOnUiThread) {
        super(runOnUiThread);
        mOut = listener;
    }

    private final Object mConnectStateTag = new Object();

    @Override
    public void onConnecting() {
        dispatch(mConnectStateTag, () -> {
            if (mOut != null) {
                mOut.onConnecting();
            }
        });
    }

    @Override
    public void onConnectSuccess() {
        dispatch(mConnectStateTag, () -> {
            if (mOut != null) {
                mOut.onConnectSuccess();
            }
        });
    }

    @Override
    public void onConnectClosed() {
        dispatch(mConnectStateTag, () -> {
            if (mOut != null) {
                mOut.onConnectClosed();
            }
        });
    }

    private final Object mSignInStateTag = new Object();

    @Override
    public void onSigningIn() {
        dispatch(mSignInStateTag, () -> {
            if (mOut != null) {
                mOut.onSigningIn();
            }
        });
    }

    @Override
    public void onSignInSuccess() {
        dispatch(mSignInStateTag, () -> {
            if (mOut != null) {
                mOut.onSignInSuccess();
            }
        });
    }

    @Override
    public void onSignInFail(@NonNull GeneralResult result) {
        dispatch(mSignInStateTag, () -> {
            if (mOut != null) {
                mOut.onSignInFail(result);
            }
        });
    }

    @Override
    public void onKickedOffline() {
        dispatch(() -> {
            if (mOut != null) {
                mOut.onKickedOffline();
            }
        });
    }

    @Override
    public void onTokenExpired() {
        dispatch(() -> {
            if (mOut != null) {
                mOut.onTokenExpired();
            }
        });
    }

    private final Object mSignOutStateTag = new Object();

    @Override
    public void onSigningOut() {
        dispatch(mSignOutStateTag, () -> {
            if (mOut != null) {
                mOut.onSigningOut();
            }
        });
    }

    @Override
    public void onSignOutSuccess() {
        dispatch(mSignOutStateTag, () -> {
            if (mOut != null) {
                mOut.onSignOutSuccess();
            }
        });
    }

    @Override
    public void onSignOutFail(@NonNull GeneralResult result) {
        dispatch(mSignOutStateTag, () -> {
            if (mOut != null) {
                mOut.onSignOutFail(result);
            }
        });
    }

}
