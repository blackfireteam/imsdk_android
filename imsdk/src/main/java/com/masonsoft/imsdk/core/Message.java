package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;

/**
 * 传输于长连接上的原始消息
 */
public class Message {

    /**
     * proto buff 编码
     */
    private final int mType;

    /**
     * proto buff 的序列化数据
     */
    @NonNull
    private final byte[] mData;

    public Message(int type, @NonNull byte[] data) {
        this.mType = type;
        this.mData = data;
    }

    public int getType() {
        return mType;
    }

    @NonNull
    public byte[] getData() {
        return mData;
    }

    @NonNull
    public String toShortString() {
        return String.format("Message[type:%s, data length:%s]", mType, mData.length);
    }

    @Override
    @NonNull
    public String toString() {
        return toShortString();
    }

}
