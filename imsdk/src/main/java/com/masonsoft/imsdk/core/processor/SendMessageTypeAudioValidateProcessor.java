package com.masonsoft.imsdk.core.processor;

import android.text.TextUtils;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;

import com.idonans.core.util.HumanUtil;
import com.masonsoft.imsdk.IMSessionMessage;
import com.masonsoft.imsdk.R;
import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.lang.StateProp;

import java.io.File;

/**
 * 发送语音类型的消息合法性检查
 *
 * @since 1.0
 */
public class SendMessageTypeAudioValidateProcessor extends SendMessageTypeValidateProcessor {

    public SendMessageTypeAudioValidateProcessor() {
        super(IMConstants.MessageType.AUDIO);
    }

    @Override
    protected boolean doTypeProcess(@NonNull IMSessionMessage target, long type) {
        if (validateAudio(target)) {
            return true;
        }

        return validateDuration(target);
    }

    private boolean validateAudio(@NonNull IMSessionMessage target) {
        final StateProp<String> audio = target.getIMMessage().body;
        if (audio.isUnset()) {
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    IMSessionMessage.EnqueueCallback.ERROR_CODE_AUDIO_MESSAGE_AUDIO_PATH_UNSET,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_audio_message_audio_path_unset)
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
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    IMSessionMessage.EnqueueCallback.ERROR_CODE_AUDIO_MESSAGE_AUDIO_PATH_INVALID,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_audio_message_audio_path_invalid)
            );
            return true;
        }

        if (URLUtil.isNetworkUrl(audioPath)) {
            // 文件本身是一个网络地址
            return false;
        }

        if (audioPath.startsWith("file://")) {
            audioPath = audioPath.substring(6);

            // 应用文件地址变更
            audio.set(audioPath);
        }

        // 校验语音文件是否存在并且文件的大小的是否合法
        final File audioFile = new File(audioPath);
        if (!audioFile.exists() || !audioFile.isFile()) {
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    IMSessionMessage.EnqueueCallback.ERROR_CODE_AUDIO_MESSAGE_AUDIO_PATH_INVALID,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_audio_message_audio_path_invalid)
            );
            return true;
        }
        if (IMConstants.SendMessageOption.Audio.MAX_FILE_SIZE > 0
                && audioFile.length() > IMConstants.SendMessageOption.Audio.MAX_FILE_SIZE) {
            // 语音文件太大
            final String maxFileSizeAsHumanString = HumanUtil.getHumanSizeFromByte(IMConstants.SendMessageOption.Audio.MAX_FILE_SIZE);
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    IMSessionMessage.EnqueueCallback.ERROR_CODE_AUDIO_MESSAGE_AUDIO_FILE_SIZE_TOO_LARGE,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_audio_message_audio_file_size_too_large, maxFileSizeAsHumanString)
            );
            return true;
        }

        return false;
    }

    private boolean validateDuration(@NonNull IMSessionMessage target) {
        if (!IMConstants.SendMessageOption.Audio.DURATION_REQUIRED) {
            return false;
        }

        // 必须要有合法的时长参数
        final StateProp<Long> duration = target.getIMMessage().duration;
        if (duration.isUnset()
                || duration.get() == null
                || duration.get() <= 0) {
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    IMSessionMessage.EnqueueCallback.ERROR_CODE_AUDIO_MESSAGE_AUDIO_DURATION_INVALID,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_audio_message_audio_duration_invalid)
            );
            return true;
        }

        return false;
    }

}
