package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMMessage;

/**
 * @since 1.0
 */
public class MSIMAudioElement extends MSIMElement {

    MSIMAudioElement(@NonNull IMMessage message) {
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

    public long getDurationMs() {
        return getDurationMs(0L);
    }

    public long getDurationMs(long defaultValue) {
        return getMessage().durationMs.getOrDefault(defaultValue);
    }

}
