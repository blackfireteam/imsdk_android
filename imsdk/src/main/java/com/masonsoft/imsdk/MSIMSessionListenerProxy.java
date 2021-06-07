package com.masonsoft.imsdk;

import androidx.annotation.Nullable;

/**
 * @since 1.0
 */
public class MSIMSessionListenerProxy extends AutoRemoveDuplicateRunnable implements MSIMSessionListener {

    @Nullable
    private final MSIMSessionListener mOut;

    public MSIMSessionListenerProxy(@Nullable MSIMSessionListener listener) {
        this(listener, false);
    }

    public MSIMSessionListenerProxy(@Nullable MSIMSessionListener listener, boolean runOnUiThread) {
        super(runOnUiThread);
        mOut = listener;
    }

    @Override
    public void onSessionUserIdChanged() {
        final String tag = getOnSessionUserIdChangedTag();
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onSessionUserIdChanged();
            }
        });
    }

    @Nullable
    protected String getOnSessionUserIdChangedTag() {
        return "onSessionUserIdChanged";
    }

    @Override
    public void onSessionChanged() {
        final String tag = getOnSessionChangedTag();
        dispatch(tag, () -> {
            if (mOut != null) {
                mOut.onSessionChanged();
            }
        });
    }

    @Nullable
    protected String getOnSessionChangedTag() {
        return "onSessionChanged";
    }

}
