package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * @since 1.0
 */
public class MSIMWeakSessionListener extends AutoRemoveDuplicateRunnable implements MSIMSessionListener {

    @NonNull
    private final WeakReference<MSIMSessionListener> mOutRef;

    public MSIMWeakSessionListener(@Nullable MSIMSessionListener callback) {
        this(callback, false);
    }

    public MSIMWeakSessionListener(@Nullable MSIMSessionListener callback, boolean runOnUiThread) {
        super(runOnUiThread);
        mOutRef = new WeakReference<>(callback);
    }

    @Override
    public void onSessionChanged() {
        final String tag = getOnSessionChangedTag();
        dispatch(tag, () -> {
            final MSIMSessionListener out = mOutRef.get();
            if (out != null) {
                out.onSessionChanged();
            }
        });
    }

    @Nullable
    protected String getOnSessionChangedTag() {
        return "onSessionChanged";
    }

    @Override
    public void onSessionUserIdChanged() {
        final String tag = getOnSessionUserIdChanged();
        dispatch(tag, () -> {
            final MSIMSessionListener out = mOutRef.get();
            if (out != null) {
                out.onSessionUserIdChanged();
            }
        });
    }

    @Nullable
    protected String getOnSessionUserIdChanged() {
        return "onSessionUserIdChanged";
    }

}
