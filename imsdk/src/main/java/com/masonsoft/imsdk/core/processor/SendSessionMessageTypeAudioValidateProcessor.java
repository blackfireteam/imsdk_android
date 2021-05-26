package com.masonsoft.imsdk.core.processor;

import android.net.Uri;
import android.text.TextUtils;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMMessage;
import com.masonsoft.imsdk.core.IMSessionMessage;
import com.masonsoft.imsdk.lang.GeneralResult;
import com.masonsoft.imsdk.lang.MediaInfo;
import com.masonsoft.imsdk.lang.StateProp;
import com.masonsoft.imsdk.util.MediaUtil;

import java.io.File;

import io.github.idonans.core.util.HumanUtil;

/**
 * 发送语音类型的消息合法性检查
 *
 * @since 1.0
 */
public class SendSessionMessageTypeAudioValidateProcessor extends SendSessionMessageTypeValidateProcessor {

    public SendSessionMessageTypeAudioValidateProcessor() {
        super(IMConstants.MessageType.AUDIO);
    }

    @Override
    protected boolean doTypeProcess(@NonNull IMSessionMessage target, int type) {
        if (validateAudio(target)) {
            return true;
        }

        return validateDuration(target);
    }

    private boolean validateAudio(@NonNull IMSessionMessage target) {
        final StateProp<String> audio = target.getMessage().body;
        if (audio.isUnset()) {
            target.getEnqueueCallback().onCallback(
                    GeneralResult.valueOf(GeneralResult.ERROR_CODE_AUDIO_MESSAGE_AUDIO_PATH_UNSET)
                            .withPayload(target)
            );
            return true;
        }

        String audioPath = audio.get();
        if (audioPath != null) {
            audioPath = audioPath.trim();

            // 应用文件地址变更
            audio.set(audioPath);
        }
        if (TextUtils.isEmpty(audioPath)) {
            target.getEnqueueCallback().onCallback(
                    GeneralResult.valueOf(GeneralResult.ERROR_CODE_AUDIO_MESSAGE_AUDIO_PATH_INVALID)
                            .withPayload(target)
            );
            return true;
        }

        if (URLUtil.isNetworkUrl(audioPath)) {
            // 文件本身是一个网络地址
            return false;
        }

        if (audioPath.startsWith("file://")) {
            audioPath = audioPath.substring(7);

            // 应用文件地址变更
            audio.set(audioPath);
        }

        // 校验语音文件是否存在并且文件的大小的是否合法
        final File audioFile = new File(audioPath);
        if (!audioFile.exists() || !audioFile.isFile()) {
            target.getEnqueueCallback().onCallback(
                    GeneralResult.valueOf(GeneralResult.ERROR_CODE_AUDIO_MESSAGE_AUDIO_PATH_INVALID)
                            .withPayload(target)
            );
            return true;
        }
        if (IMConstants.SendMessageOption.Audio.MAX_FILE_SIZE > 0
                && audioFile.length() > IMConstants.SendMessageOption.Audio.MAX_FILE_SIZE) {
            // 语音文件太大
            final String maxFileSizeAsHumanString = HumanUtil.getHumanSizeFromByte(IMConstants.SendMessageOption.Audio.MAX_FILE_SIZE);
            target.getEnqueueCallback().onCallback(
                    GeneralResult.valueOf(GeneralResult.ERROR_CODE_AUDIO_MESSAGE_AUDIO_FILE_SIZE_TOO_LARGE)
                            .withPayload(target)
            );
            return true;
        }

        return false;
    }

    private boolean validateDuration(@NonNull IMSessionMessage target) {
        final StateProp<Long> duration = target.getMessage().durationMs;
        if (!duration.isUnset()
                && duration.get() != null
                && duration.get() > 0) {
            // 已经设置了合法的时长
            return false;
        }

        // 从音频文件中获取时长信息
        final IMMessage message = target.getMessage();
        final String audioPath = message.body.get();
        if (URLUtil.isNetworkUrl(audioPath)) {
            // 文件本身是一个网络地址
            // 网络地址无法获取时长信息
            target.getEnqueueCallback().onCallback(
                    GeneralResult.valueOf(GeneralResult.ERROR_CODE_AUDIO_MESSAGE_AUDIO_DURATION_INVALID)
                            .withPayload(target)
            );
            return true;
        }

        long decodeDuration = 0L;
        final MediaInfo mediaInfo = MediaUtil.decodeMediaInfo(Uri.parse(audioPath));
        if (mediaInfo != null) {
            decodeDuration = mediaInfo.durationMs;
        }
        if (decodeDuration <= 0) {
            // 语音时长无效
            target.getEnqueueCallback().onCallback(
                    GeneralResult.valueOf(GeneralResult.ERROR_CODE_AUDIO_MESSAGE_AUDIO_DURATION_INVALID)
                            .withPayload(target)
            );
            return true;
        }

        // 应用时长信息变更
        duration.set(decodeDuration);

        return false;
    }

}
