package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.IMMessage;
import com.masonsoft.imsdk.util.Objects;

/**
 * @since 1.0
 */
public class MSIMMessage {

    @NonNull
    private final IMMessage mMessage;

    MSIMMessage(@NonNull IMMessage message) {
        mMessage = message;
    }

    @NonNull
    IMMessage getMessage() {
        return mMessage;
    }

    public long getSessionUserId() {
        return getSessionUserId(0L);
    }

    public long getSessionUserId(long defaultValue) {
        return mMessage._sessionUserId.getOrDefault(defaultValue);
    }

    public int getConversationType() {
        return getConversationType(0);
    }

    public int getConversationType(int defaultValue) {
        return mMessage._conversationType.getOrDefault(defaultValue);
    }

    public long getTargetUserId() {
        return getTargetUserId(0L);
    }

    public long getTargetUserId(long defaultValue) {
        return mMessage._targetUserId.getOrDefault(defaultValue);
    }

    public long getMessageId() {
        return getMessageId(0L);
    }

    public long getMessageId(long defaultValue) {
        return mMessage.id.getOrDefault(defaultValue);
    }

    public long getServerMessageId() {
        return getServerMessageId(0L);
    }

    public long getServerMessageId(long defaultValue) {
        return mMessage.serverMessageId.getOrDefault(defaultValue);
    }

    public long getSeq() {
        return getSeq(0L);
    }

    public long getSeq(long defaultValue) {
        return mMessage.seq.getOrDefault(defaultValue);
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
        return mMessage.type.getOrDefault(defaultValue);
    }

    public String getBody() {
        return getBody(null);
    }

    public String getBody(String defaultValue) {
        return mMessage.body.getOrDefault(defaultValue);
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

    public long getLastModify() {
        return getLastModify(0L);
    }

    public long getLastModify(long defaultValue) {
        return mMessage.lastModifyMs.getOrDefault(defaultValue);
    }

    public long getTimeMs() {
        return getTimeMs(0L);
    }

    public long getTimeMs(long defaultValue) {
        return mMessage.timeMs.getOrDefault(defaultValue);
    }

    public long getSender() {
        return getSender(0L);
    }

    public long getSender(long defaultValue) {
        return mMessage.fromUserId.getOrDefault(defaultValue);
    }

    public long getReceiver() {
        return getReceiver(0L);
    }

    public long getReceiver(long defaultValue) {
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
        return mMessage.sendState.getOrDefault(defaultValue);
    }

    public float getSendProgress() {
        return getSendProgress(0f);
    }

    public float getSendProgress(float defaultValue) {
        return mMessage.sendProgress.getOrDefault(defaultValue);
    }

    @Nullable
    public MSIMTextElement getTextElement() {
        final int messageType = getMessageType(-1);
        if (messageType == MSIMConstants.MessageType.TEXT) {
            return new MSIMTextElement(mMessage);
        }
        return null;
    }

    @Nullable
    public MSIMImageElement getImageElement() {
        final int messageType = getMessageType(-1);
        if (messageType == MSIMConstants.MessageType.IMAGE) {
            return new MSIMImageElement(mMessage);
        }
        return null;
    }

    @Nullable
    public MSIMAudioElement getAudioElement() {
        final int messageType = getMessageType(-1);
        if (messageType == MSIMConstants.MessageType.AUDIO) {
            return new MSIMAudioElement(mMessage);
        }
        return null;
    }

    @Nullable
    public MSIMVideoElement getVideoElement() {
        final int messageType = getMessageType(-1);
        if (messageType == MSIMConstants.MessageType.VIDEO) {
            return new MSIMVideoElement(mMessage);
        }
        return null;
    }

    @Nullable
    public MSIMCustomSignalingElement getCustomSignalingElement() {
        final int messageType = getMessageType(-1);
        if (messageType == MSIMConstants.MessageType.CUSTOM_MESSAGE_SIGNALING) {
            return new MSIMCustomSignalingElement(mMessage);
        }
        return null;
    }

    @Nullable
    public MSIMCustomElement getCustomElement() {
        final int messageType = getMessageType(-1);
        if (MSIMConstants.MessageType.isCustomMessage(messageType)) {
            return new MSIMCustomElement(mMessage);
        }
        return null;
    }

    @NonNull
    @Override
    public String toString() {
        //noinspection StringBufferReplaceableByString
        final StringBuilder builder = new StringBuilder();
        builder.append(Objects.defaultObjectTag(this));
        builder.append(" ").append(mMessage);
        return builder.toString();
    }

}
