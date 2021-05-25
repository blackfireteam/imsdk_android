package com.masonsoft.imsdk;

import com.masonsoft.imsdk.core.IMMessage;

/**
 * @since 1.0
 */
public abstract class MSIMElement {

    private IMMessage mMessage;

    void setMessage(IMMessage message) {
        mMessage = message;
    }

    IMMessage getMessage() {
        return mMessage;
    }

}
