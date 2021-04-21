package com.masonsoft.imsdk.sample.util;

import org.apache.commons.codec.digest.DigestUtils;

public class RequestSignUtil {

    private RequestSignUtil() {
    }

    public static String calSign(String appSecret, int nonce, long timestamp/*秒*/) {
        final String input = appSecret + nonce + timestamp;
        return DigestUtils.sha1Hex(input);
    }

}
