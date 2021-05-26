package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMMessage;

/**
 * @since 1.0
 */
public class MSIMImageElement extends MSIMElement {

    MSIMImageElement(@NonNull IMMessage message) {
        super(message);
    }

    public String getPath() {
        return getPath(null);
    }

    public String getPath(String defaultValue) {
        return getMessage().localBodyOrigin.getOrDefault(defaultValue);
    }

    public String getUrl() {
        return getUrl(null);
    }

    public String getUrl(String defaultValue) {
        return getMessage().body.getOrDefault(defaultValue);
    }

    public long getWidth() {
        return getWidth(0L);
    }

    public long getWidth(long defaultValue) {
        return getMessage().width.getOrDefault(defaultValue);
    }

    public long getHeight() {
        return getHeight(0L);
    }

    public long getHeight(long defaultValue) {
        return getMessage().height.getOrDefault(defaultValue);
    }

}
