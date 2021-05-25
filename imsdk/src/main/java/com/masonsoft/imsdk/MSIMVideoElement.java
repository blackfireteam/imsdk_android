package com.masonsoft.imsdk;

import com.masonsoft.imsdk.core.IMMessage;

/**
 * @since 1.0
 */
public class MSIMVideoElement extends MSIMElement {

    public String getPath() {
        return getPath(null);
    }

    public String getPath(String defaultValue) {
        final IMMessage message = getMessage();
        if (message == null) {
            return defaultValue;
        }

        return message.localBodyOrigin.getOrDefault(defaultValue);
    }

    public String getUrl() {
        return getUrl(null);
    }

    public String getUrl(String defaultValue) {
        final IMMessage message = getMessage();
        if (message == null) {
            return defaultValue;
        }
        return message.body.getOrDefault(defaultValue);
    }

    public long getDurationMilliseconds() {
        return getDurationMilliseconds(0L);
    }

    public long getDurationMilliseconds(long defaultValue) {
        final IMMessage message = getMessage();
        if (message == null) {
            return defaultValue;
        }
        return message.durationMs.getOrDefault(defaultValue);
    }

    public long getWidth() {
        return getWidth(0L);
    }

    public long getWidth(long defaultValue) {
        final IMMessage message = getMessage();
        if (message == null) {
            return defaultValue;
        }
        return message.width.getOrDefault(defaultValue);
    }

    public long getHeight() {
        return getHeight(0L);
    }

    public long getHeight(long defaultValue) {
        final IMMessage message = getMessage();
        if (message == null) {
            return defaultValue;
        }
        return message.height.getOrDefault(defaultValue);
    }

    public String getThumb() {
        return getThumb(null);
    }

    public String getThumb(String defaultValue) {
        final IMMessage message = getMessage();
        if (message == null) {
            return defaultValue;
        }
        return message.thumb.getOrDefault(defaultValue);
    }

}
