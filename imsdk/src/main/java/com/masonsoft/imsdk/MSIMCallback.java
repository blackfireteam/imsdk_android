package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

/**
 * @since 1.0
 */
public interface MSIMCallback<T> {

    void onCallback(@NonNull T payload);

}
