package com.masonsoft.imsdk;

import com.masonsoft.imsdk.core.IMMessage;

/**
 * @since 1.0
 */
public class MSIMAudioElement extends MSIMElement {

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

}
