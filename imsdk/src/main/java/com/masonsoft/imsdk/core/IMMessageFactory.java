package com.masonsoft.imsdk.core;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.db.LocalSendingMessage;
import com.masonsoft.imsdk.core.db.Message;

import java.util.Objects;

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
     * @param imageUri 图片 Uri
     */
    @NonNull
    public static IMMessage createImageMessage(Uri imageUri) {
        final IMMessage target = new IMMessage();
        target.type.set(IMConstants.MessageType.IMAGE);
        target.body.set(imageUri.toString());
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
     */
    @NonNull
    public static IMMessage createAudioMessage(String localAudioPath) {
        final IMMessage target = new IMMessage();
        target.type.set(IMConstants.MessageType.AUDIO);
        target.body.set(localAudioPath);
        return target;
    }

    /**
     * 视频消息
     *
     * @param videoUrl 视频 Uri
     */
    @NonNull
    public static IMMessage createVideoMessage(Uri videoUrl) {
        final IMMessage target = new IMMessage();
        target.type.set(IMConstants.MessageType.VIDEO);
        target.body.set(videoUrl.toString());
        return target;
    }

    /**
     * 视频消息
     *
     * @param localVideoPath 视频文件的本地完整路径
     * @param durationMs     视频时长，单位毫秒
     * @param width          视频宽度
     * @param height         视频高度
     */
    @NonNull
    public static IMMessage createVideoMessage(
            String localVideoPath,
            long durationMs,
            long width,
            long height,
            String localVideoThumbPath) {
        final IMMessage target = new IMMessage();
        target.type.set(IMConstants.MessageType.VIDEO);
        target.body.set(localVideoPath);
        target.durationMs.set(durationMs);
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

    @NonNull
    public static IMMessage create(@NonNull Message input) {
        final IMMessage target = new IMMessage();
        target._sessionUserId.apply(input._sessionUserId);
        target._conversationType.apply(input._conversationType);
        target._targetUserId.apply(input._targetUserId);
        target.id.apply(input.localId);
        target.serverMessageId.apply(input.remoteMessageId);
        target.lastModifyMs.apply(input.localLastModifyMs);
        target.seq.apply(input.localSeq);
        target.fromUserId.apply(input.fromUserId);
        target.toUserId.apply(input.toUserId);
        target.timeMs.apply(input.localTimeMs);
        target.type.apply(input.messageType);
        target.title.apply(input.title);
        target.body.apply(input.body);
        target.localBodyOrigin.apply(input.localBodyOrigin);
        target.thumb.apply(input.thumb);
        target.localThumbOrigin.apply(input.localThumbOrigin);
        target.width.apply(input.width);
        target.height.apply(input.height);
        target.durationMs.apply(input.durationMs);
        target.lat.apply(input.lat);
        target.lng.apply(input.lng);
        target.zoom.apply(input.zoom);
        return target;
    }

    @NonNull
    public static IMMessage merge(@NonNull IMMessage input, @Nullable LocalSendingMessage sendingMessage) {
        final IMMessage target = copy(input);
        if (sendingMessage == null) {
            return target;
        }

        if (!Objects.equals(target._conversationType.get(), sendingMessage.conversationType.get())) {
            final Throwable e = new IllegalArgumentException("unexpected merge fail, conversationType not match " + target + " <-> " + sendingMessage);
            IMLog.e(e);
            return target;
        }

        if (!Objects.equals(target._targetUserId.get(), sendingMessage.targetUserId.get())) {
            final Throwable e = new IllegalArgumentException("unexpected merge fail, targetUserId not match " + target + " <-> " + sendingMessage);
            IMLog.e(e);
            return target;
        }

        if (!Objects.equals(target.id.get(), sendingMessage.messageLocalId.get())) {
            final Throwable e = new IllegalArgumentException("unexpected merge fail, messageLocalId not match " + target + " <-> " + sendingMessage);
            IMLog.e(e);
            return target;
        }

        if (!input.lastModifyMs.isUnset()
                && !sendingMessage.localLastModifyMs.isUnset()) {
            final long lastModifyMs1 = input.lastModifyMs.get();
            final long lastModifyMs2 = sendingMessage.localLastModifyMs.get();
            if (lastModifyMs1 < lastModifyMs2) {
                input.lastModifyMs.set(lastModifyMs2);
            }
        }

        if (sendingMessage.localSendStatus.isUnset()) {
            final Throwable e = new IllegalArgumentException("unexpected. sendingMessage.localSendStatus.isUnset()");
            IMLog.e(e);
        }
        if (sendingMessage.errorCode.isUnset()) {
            final Throwable e = new IllegalArgumentException("unexpected. sendingMessage.errorCode.isUnset()");
            IMLog.e(e);
        }
        if (sendingMessage.errorMessage.isUnset()) {
            final Throwable e = new IllegalArgumentException("unexpected. sendingMessage.errorMessage.isUnset()");
            IMLog.e(e);
        }

        input.sendState.apply(sendingMessage.localSendStatus);
        input.errorCode.apply(sendingMessage.errorCode);
        input.errorMessage.apply(sendingMessage.errorMessage);
        return input;
    }

}
