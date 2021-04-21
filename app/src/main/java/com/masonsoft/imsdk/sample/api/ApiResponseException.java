package com.masonsoft.imsdk.sample.api;

public class ApiResponseException extends RuntimeException {

    public int code;
    public String message;

    public ApiResponseException(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
