package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMMessage;

/**
 * 自定义信令消息
 *
 * @since 1.0
 */
public class MSIMCustomSignalingElement extends MSIMElement {

    MSIMCustomSignalingElement(@NonNull IMMessage message) {
        super(message);
    }

    public String getText() {
        return getText(null);
    }

    public String getText(String defaultValue) {
        return getMessage().body.getOrDefault(defaultValue);
    }

}
