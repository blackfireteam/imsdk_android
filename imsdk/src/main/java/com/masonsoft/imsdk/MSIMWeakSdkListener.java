package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.lang.GeneralResult;

import java.lang.ref.WeakReference;

/**
 * @since 1.0
 */
public class MSIMWeakSdkListener extends AutoRemoveDuplicateRunnable implements MSIMSdkListener {

    @NonNull
    private final WeakReference<MSIMSdkListener> mOutRef;

    public MSIMWeakSdkListener(@Nullable MSIMSdkListener listener) {
        this(listener, false);
    }

    public MSIMWeakSdkListener(@Nullable MSIMSdkListener listener, boolean runOnUiThread) {
        super(runOnUiThread);
        mOutRef = new WeakReference<>(listener);
    }

    private final Object mConnectStateTag = new Object();

    @Override
    public void onConnecting() {
        dispatch(mConnectStateTag, () -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onConnecting();
            }
        });
    }

    @Override
    public void onConnectSuccess() {
        dispatch(mConnectStateTag, () -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onConnectSuccess();
            }
        });
    }

    @Override
    public void onConnectClosed() {
        dispatch(mConnectStateTag, () -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onConnectClosed();
            }
        });
    }

    private final Object mSignInStateTag = new Object();

    @Override
    public void onSigningIn() {
        dispatch(mSignInStateTag, () -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onSigningIn();
            }
        });
    }

    @Override
    public void onSignInSuccess() {
        dispatch(mSignInStateTag, () -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onSignInSuccess();
            }
        });
    }

    @Override
    public void onSignInFail(@NonNull GeneralResult result) {
        dispatch(mSignInStateTag, () -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onSignInFail(result);
            }
        });
    }

    @Override
    public void onKickedOffline() {
        dispatch(() -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onKickedOffline();
            }
        });
    }

    @Override
    public void onTokenExpired() {
        dispatch(() -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onTokenExpired();
            }
        });
    }

    private final Object mSignOutStateTag = new Object();

    @Override
    public void onSigningOut() {
        dispatch(mSignOutStateTag, () -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onSigningOut();
            }
        });
    }

    @Override
    public void onSignOutSuccess() {
        dispatch(mSignOutStateTag, () -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onSignOutSuccess();
            }
        });
    }

    @Override
    public void onSignOutFail(@NonNull GeneralResult result) {
        dispatch(mSignOutStateTag, () -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onSignOutFail(result);
            }
        });
    }

}
