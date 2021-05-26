package com.masonsoft.imsdk;

import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.IMMessage;

/**
 * @since 1.0
 */
public class MSIMMessage {

    @Nullable
    private IMMessage mMessage;

    void setMessage(@Nullable IMMessage message) {
        mMessage = message;
    }

    public long getMessageId() {
        return getMessageId(0L);
    }

    public long getMessageId(long defaultValue) {
        if (mMessage == null) {
            return defaultValue;
        }
        return mMessage.id.getOrDefault(defaultValue);
    }

    /**
     * @see MSIMConstants.MessageType
     */
    public int getMessageType() {
        return getMessageType(0);
    }

    /**
     * @see MSIMConstants.MessageType
     */
    public int getMessageType(int defaultValue) {
        if (mMessage == null) {
            return defaultValue;
        }
        return mMessage.type.getOrDefault(defaultValue);
    }

    /**
     * 判断这一条消息是否是已撤回的消息
     *
     * @see MSIMConstants.MessageType
     * @see #getMessageType()
     */
    public boolean isRevoked() {
        final int messageType = getMessageType(-1);
        return messageType == MSIMConstants.MessageType.REVOKED;
    }

    public long getTimeMilliseconds() {
        return getTimeMilliseconds(0L);
    }

    public long getTimeMilliseconds(long defaultValue) {
        if (mMessage == null) {
            return defaultValue;
        }
        return mMessage.timeMs.getOrDefault(defaultValue);
    }

    public long getSender() {
        return getSender(0L);
    }

    public long getSender(long defaultValue) {
        if (mMessage == null) {
            return defaultValue;
        }
        return mMessage.fromUserId.getOrDefault(defaultValue);
    }

    public long getReceiver() {
        return getReceiver(0L);
    }

    public long getReceiver(long defaultValue) {
        if (mMessage == null) {
            return defaultValue;
        }
        return mMessage.toUserId.getOrDefault(defaultValue);
    }

    /**
     * @see MSIMConstants.SendStatus
     */
    public int getSendStatus() {
        return getSendStatus(0);
    }

    /**
     * @see MSIMConstants.SendStatus
     */
    public int getSendStatus(int defaultValue) {
        if (mMessage == null) {
            return defaultValue;
        }
        return mMessage.sendState.getOrDefault(defaultValue);
    }

    @Nullable
    public MSIMTextElement getTextElement() {
        final int messageType = getMessageType(-1);
        if (messageType == MSIMConstants.MessageType.TEXT) {
            MSIMTextElement element = new MSIMTextElement();
            element.setMessage(mMessage);
            return element;
        }
        return null;
    }

    @Nullable
    public MSIMImageElement getImageElement() {
        final int messageType = getMessageType(-1);
        if (messageType == MSIMConstants.MessageType.IMAGE) {
            MSIMImageElement element = new MSIMImageElement();
            element.setMessage(mMessage);
            return element;
        }
        return null;
    }

    @Nullable
    public MSIMAudioElement getAudioElement() {
        final int messageType = getMessageType(-1);
        if (messageType == MSIMConstants.MessageType.AUDIO) {
            MSIMAudioElement element = new MSIMAudioElement();
            element.setMessage(mMessage);
            return element;
        }
        return null;
    }

    @Nullable
    public MSIMVideoElement getVideoElement() {
        final int messageType = getMessageType(-1);
        if (messageType == MSIMConstants.MessageType.VIDEO) {
            MSIMVideoElement element = new MSIMVideoElement();
            element.setMessage(mMessage);
            return element;
        }
        return null;
    }

    @Nullable
    public MSIMCustomElement getCustomElement() {
        final int messageType = getMessageType(-1);
        if (messageType == MSIMConstants.MessageType.FIRST_CUSTOM_MESSAGE) {
            MSIMCustomElement element = new MSIMCustomElement();
            element.setMessage(mMessage);
            return element;
        }
        return null;
    }

}
