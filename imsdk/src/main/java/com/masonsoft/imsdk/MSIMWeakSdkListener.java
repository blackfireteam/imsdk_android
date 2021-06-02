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

    private final Object mSignInStateTag = new Object();
    private final Object mConnectStateTag = new Object();
    private final Object mSignOutStateTag = new Object();

    public MSIMWeakSdkListener(@Nullable MSIMSdkListener listener) {
        this(listener, false);
    }

    public MSIMWeakSdkListener(@Nullable MSIMSdkListener listener, boolean runOnUiThread) {
        super(runOnUiThread);
        mOutRef = new WeakReference<>(listener);
    }

    @Override
    public void onConnecting() {
        final Object tag = getOnConnectingTag();
        dispatch(tag, () -> {
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onConnecting();
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
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onConnectSuccess();
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
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onConnectClosed();
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
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onSigningIn();
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
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onSignInSuccess();
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
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onSignInFail(result);
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
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onKickedOffline();
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
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onTokenExpired();
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
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onSigningOut();
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
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onSignOutSuccess();
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
            final MSIMSdkListener out = mOutRef.get();
            if (out != null) {
                out.onSignOutFail(result);
            }
        });
    }

    @Nullable
    protected Object getOnSignOutFailTag(@NonNull GeneralResult result) {
        return mSignOutStateTag;
    }

}
