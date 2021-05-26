package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMMessage;

/**
 * @since 1.0
 */
public class MSIMCustomElement extends MSIMElement {

    MSIMCustomElement(@NonNull IMMessage message) {
        super(message);
    }

    public String getText() {
        return getText(null);
    }

    public String getText(String defaultValue) {
        return getMessage().body.getOrDefault(defaultValue);
    }

}
