package com.masonsoft.imsdk.lang;

public class GeneralErrorCodeException extends RuntimeException {

    public final int errorCode;

    public GeneralErrorCodeException(int errorCode) {
        this.errorCode = errorCode;
    }

}
