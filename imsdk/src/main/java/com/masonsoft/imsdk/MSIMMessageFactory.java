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
        final MSIMMessage target = new MSIMMessage();
        target.setMessage(message);
        return target;
    }

    @NonNull
    public static MSIMMessage createImageMessage(Uri imageUri) {
        final IMMessage message = IMMessageFactory.createImageMessage(imageUri);
        final MSIMMessage target = new MSIMMessage();
        target.setMessage(message);
        return target;
    }

    @NonNull
    public static MSIMMessage createAudioMessage(String localAudioPath) {
        final IMMessage message = IMMessageFactory.createAudioMessage(localAudioPath);
        final MSIMMessage target = new MSIMMessage();
        target.setMessage(message);
        return target;
    }

    @NonNull
    public static MSIMMessage createVideoMessage(Uri videoUrl) {
        final IMMessage message = IMMessageFactory.createVideoMessage(videoUrl);
        final MSIMMessage target = new MSIMMessage();
        target.setMessage(message);
        return target;
    }

    @NonNull
    public static MSIMMessage createCustomMessage(String text) {
        final IMMessage message = IMMessageFactory.createCustomMessage(text);
        final MSIMMessage target = new MSIMMessage();
        target.setMessage(message);
        return target;
    }

}
