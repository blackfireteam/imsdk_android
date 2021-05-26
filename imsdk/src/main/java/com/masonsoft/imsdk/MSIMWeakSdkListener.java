package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.lang.GeneralResult;

import java.lang.ref.WeakReference;

import io.github.idonans.core.thread.Threads;

/**
 * @since 1.0
 */
public class MSIMWeakSdkListener implements MSIMSdkListener {

    @NonNull
    private final WeakReference<MSIMSdkListener> mOutRef;
    private final boolean mRunOnUiThread;

    public MSIMWeakSdkListener(@Nullable MSIMSdkListener listener) {
        this(listener, false);
    }

    public MSIMWeakSdkListener(@Nullable MSIMSdkListener listener, boolean runOnUiThread) {
        mOutRef = new WeakReference<>(listener);
        mRunOnUiThread = runOnUiThread;
    }

    private void runOrPost(Runnable runnable) {
        if (mRunOnUiThread) {
            Threads.postUi(runnable);
        } else {
            runnable.run();
        }
    }

    @Override
    public void onConnecting() {
        runOrPost(() -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onConnecting();
            }
        });
    }

    @Override
    public void onConnectSuccess() {
        runOrPost(() -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onConnectSuccess();
            }
        });
    }

    @Override
    public void onConnectClosed() {
        runOrPost(() -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onConnectClosed();
            }
        });
    }

    @Override
    public void onSigningIn() {
        runOrPost(() -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onSigningIn();
            }
        });
    }

    @Override
    public void onSignInSuccess() {
        runOrPost(() -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onSignInSuccess();
            }
        });
    }

    @Override
    public void onSignInFail(@NonNull GeneralResult result) {
        runOrPost(() -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onSignInFail(result);
            }
        });
    }

    @Override
    public void onKickedOffline() {
        runOrPost(() -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onKickedOffline();
            }
        });
    }

    @Override
    public void onTokenExpired() {
        runOrPost(() -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onTokenExpired();
            }
        });
    }

    @Override
    public void onSigningOut() {
        runOrPost(() -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onSigningOut();
            }
        });
    }

    @Override
    public void onSignOutSuccess() {
        runOrPost(() -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onSignOutSuccess();
            }
        });
    }

    @Override
    public void onSignOutFail(@NonNull GeneralResult result) {
        runOrPost(() -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onSignOutFail(result);
            }
        });
    }

}
