package com.masonsoft.imsdk.sample;

import com.google.gson.Gson;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.core.IMConstants;

import java.util.HashMap;
import java.util.Map;

public class CustomIMMessageFactory {

    /**
     * 自定义消息(喜欢)
     */
    public static IMMessage createCustomMessageLike() {
        final IMMessage target = new IMMessage();
        target.type.set(IMConstants.MessageType.FIRST_CUSTOM_MESSAGE);

        final Map<String, Object> body = new HashMap<>();
        body.put("type", 1);
        body.put("desc", "like");
        target.body.set(new Gson().toJson(body));

        return target;
    }

}