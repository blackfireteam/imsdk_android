package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMMessage;

/**
 * @since 1.0
 */
public class MSIMVideoElement extends MSIMElement {

    MSIMVideoElement(@NonNull IMMessage message) {
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

    public long getDurationMilliseconds() {
        return getDurationMilliseconds(0L);
    }

    public long getDurationMilliseconds(long defaultValue) {
        return getMessage().durationMs.getOrDefault(defaultValue);
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

    public String getThumbPath() {
        return getThumbPath(null);
    }

    public String getThumbPath(String defaultValue) {
        return getMessage().localThumbOrigin.getOrDefault(defaultValue);
    }

    public String getThumbUrl() {
        return getThumbUrl(null);
    }

    public String getThumbUrl(String defaultValue) {
        return getMessage().thumb.getOrDefault(defaultValue);
    }

}
