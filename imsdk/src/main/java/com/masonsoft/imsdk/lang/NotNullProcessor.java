package com.masonsoft.imsdk.lang;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @since 1.0
 */
public abstract class NotNullProcessor<T> implements Processor<T> {

    @Override
    public final boolean doProcess(@Nullable T target) {
        if (target != null) {
            return doNotNullProcess(target);
        }
        // fallback
        return false;
    }

    protected abstract boolean doNotNullProcess(@NonNull T target);

}
