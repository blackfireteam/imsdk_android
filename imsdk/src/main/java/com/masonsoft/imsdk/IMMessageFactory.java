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
    public static IMMessage createTextMessage(@NonNull String text) {
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
    public static IMMessage createImageMessage(@NonNull String localImagePath) {
        final IMMessage target = new IMMessage();
        target.type.set(IMConstants.MessageType.IMAGE);
        target.body.set(localImagePath);
        return target;
    }

    /**
     * 复制一份具有相同内容的 IMMessage
     */
    @NonNull
    public static IMMessage copy(@NonNull IMMessage input) {
        final IMMessage target = new IMMessage();
        target.id.apply(input.id);
        target.seq.apply(input.seq);
        target.fromUserId.apply(input.fromUserId);
        target.toUserId.apply(input.toUserId);
        target.timeMs.apply(input.timeMs);
        target.type.apply(input.type);
        target.title.apply(input.title);
        target.body.apply(input.body);
        target.thumb.apply(input.thumb);
        target.width.apply(input.width);
        target.height.apply(input.height);
        target.duration.apply(input.duration);
        target.lat.apply(input.lat);
        target.lng.apply(input.lng);
        target.zoom.apply(input.zoom);
        target.sendState.apply(input.sendState);
        target.sendProgress.apply(input.sendProgress);
        return target;
    }

}
