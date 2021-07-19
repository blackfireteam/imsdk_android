package com.masonsoft.imsdk;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMMessage;
import com.masonsoft.imsdk.core.IMMessageFactory;

/**
 * @since 1.0
 */
public class MSIMMessageFactory {

    @NonNull
    public static MSIMMessage createTextMessage(String text) {
        final IMMessage message = IMMessageFactory.createTextMessage(text);
        return new MSIMMessage(message);
    }

    @NonNull
    public static MSIMMessage createImageMessage(Uri imageUri) {
        final IMMessage message = IMMessageFactory.createImageMessage(imageUri);
        return new MSIMMessage(message);
    }

    @NonNull
    public static MSIMMessage createAudioMessage(String localAudioPath) {
        final IMMessage message = IMMessageFactory.createAudioMessage(localAudioPath);
        return new MSIMMessage(message);
    }

    @NonNull
    public static MSIMMessage createVideoMessage(Uri videoUrl) {
        final IMMessage message = IMMessageFactory.createVideoMessage(videoUrl);
        return new MSIMMessage(message);
    }

    /**
     * 自定义信令消息
     */
    @NonNull
    public static MSIMMessage createCustomSignalingMessage(String text) {
        final IMMessage message = IMMessageFactory.createCustomSignalingMessage(text);
        return new MSIMMessage(message);
    }

    @NonNull
    public static MSIMMessage createCustomMessage(String text, boolean supportCount, boolean supportRecall) {
        final IMMessage message = IMMessageFactory.createCustomMessage(text, supportCount, supportRecall);
        return new MSIMMessage(message);
    }

    @NonNull
    public static MSIMMessage setPushInfo(@NonNull MSIMMessage message, String pushTitle, String pushBody, String pushSound) {
        final IMMessage msg = message.getMessage();
        msg.pushTitle.set(pushTitle);
        msg.pushBody.set(pushBody);
        msg.pushSound.set(pushSound);
        return message;
    }

}
