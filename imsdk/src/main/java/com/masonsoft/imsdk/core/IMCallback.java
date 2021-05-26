package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;

/**
 * @since 1.0
 */
public interface IMCallback<T> {

    void onCallback(@NonNull T payload);

}
