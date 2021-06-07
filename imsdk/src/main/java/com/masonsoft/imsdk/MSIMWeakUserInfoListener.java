package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * @since 1.0
 */
public class MSIMWeakUserInfoListener extends AutoRemoveDuplicateRunnable implements MSIMUserInfoListener {

    @NonNull
    private final WeakReference<MSIMUserInfoListener> mOutRef;

    public MSIMWeakUserInfoListener(@Nullable MSIMUserInfoListener callback) {
        this(callback, false);
    }

    public MSIMWeakUserInfoListener(@Nullable MSIMUserInfoListener callback, boolean runOnUiThread) {
        super(runOnUiThread);
        mOutRef = new WeakReference<>(callback);
    }

    @Override
    public void onUserInfoChanged(long userId) {
        final String tag = getOnUserInfoChangedTag(userId);
        dispatch(tag, () -> {
            final MSIMUserInfoListener out = mOutRef.get();
            if (out != null) {
                out.onUserInfoChanged(userId);
            }
        });
    }

    @Nullable
    protected String getOnUserInfoChangedTag(long userId) {
        return "onUserInfoChanged_" + userId;
    }

}
