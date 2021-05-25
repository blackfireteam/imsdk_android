package com.masonsoft.imsdk;

import com.masonsoft.imsdk.core.IMMessage;

/**
 * @since 1.0
 */
public class MSIMCustomElement extends MSIMElement {

    public String getText() {
        return getText(null);
    }

    public String getText(String defaultValue) {
        final IMMessage message = getMessage();
        if (message == null) {
            return defaultValue;
        }
        return message.body.getOrDefault(defaultValue);
    }

}
