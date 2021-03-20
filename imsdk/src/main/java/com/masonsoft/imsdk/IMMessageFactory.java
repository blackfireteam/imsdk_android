package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMConstants;

public class IMMessageFactory {

    private IMMessageFactory() {
    }

    /**
     * 文本消息
     */
    @NonNull
    public static IMMessage createTextMessage(String text) {
        final IMMessage target = new IMMessage();
        target.type.set(IMConstants.MessageType.TEXT);
        target.body.set(text);
        return target;
    }

    /**
     * 图片消息
     *
     * @param localImagePath 图片的本地完整路径
     */
    @NonNull
    public static IMMessage createImageMessage(String localImagePath) {
        final IMMessage target = new IMMessage();
        target.type.set(IMConstants.MessageType.IMAGE);
        target.body.set(localImagePath);
        return target;
    }

    /**
     * 图片消息
     *
     * @param localImagePath 图片的本地完整路径
     * @param width          图片的宽度
     * @param height         图片的高度
     */
    @NonNull
    public static IMMessage createImageMessage(
            String localImagePath,
            long width,
            long height) {
        final IMMessage target = new IMMessage();
        target.type.set(IMConstants.MessageType.IMAGE);
        target.body.set(localImagePath);
        target.width.set(width);
        target.height.set(height);
        return target;
    }

    /**
     * 语音消息
     *
     * @param localAudioPath 语音文件的本地完整路径
     * @param duration       语音时长，单位毫秒
     */
    @NonNull
    public static IMMessage createAudioMessage(String localAudioPath, long duration) {
        final IMMessage target = new IMMessage();
        target.type.set(IMConstants.MessageType.AUDIO);
        target.body.set(localAudioPath);
        target.duration.set(duration);
        return target;
    }

    /**
     * 视频消息
     *
     * @param localVideoPath 视频文件的本地完整路径
     * @param duration       视频时长，单位毫秒
     * @param width          视频宽度
     * @param height         视频高度
     */
    @NonNull
    public static IMMessage createVideoMessage(
            String localVideoPath,
            long duration,
            long width,
            long height,
            String localVideoThumbPath) {
        final IMMessage target = new IMMessage();
        target.type.set(IMConstants.MessageType.VIDEO);
        target.body.set(localVideoPath);
        target.duration.set(duration);
        target.width.set(width);
        target.height.set(height);
        target.thumb.set(localVideoThumbPath);
        return target;
    }

    /**
     * 复制一份具有相同内容的 IMMessage
     */
    @NonNull
    public static IMMessage copy(@NonNull IMMessage input) {
        final IMMessage target = new IMMessage();
        target.apply(input);
        return target;
    }

}
