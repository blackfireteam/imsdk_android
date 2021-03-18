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
 * 发送视频类型的消息合法性检查
 */
public class SendMessageTypeVideoValidateProcessor extends SendMessageTypeValidateProcessor {

    public SendMessageTypeVideoValidateProcessor() {
        super(IMConstants.MessageType.VIDEO);
    }

    @Override
    protected boolean doTypeProcess(@NonNull IMSessionMessage target, int type) {
        final StateProp<String> body = target.getIMMessage().body;
        if (body.isUnset()) {
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    IMSessionMessage.EnqueueCallback.ERROR_CODE_VIDEO_MESSAGE_VIDEO_PATH_UNSET,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_video_message_video_path_unset)
            );
            return true;
        }

        String videoPath = body.get();
        if (videoPath != null) {
            videoPath = videoPath.trim();

            // 应用文件地址变更
            body.set(videoPath);
        }
        if (TextUtils.isEmpty(videoPath)) {
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    IMSessionMessage.EnqueueCallback.ERROR_CODE_VIDEO_MESSAGE_VIDEO_PATH_INVALID,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_video_message_video_path_invalid)
            );
            return true;
        }

        if (IMConstants.SendMessageOption.Video.DURATION_REQUIRED) {
            // 必须要有合法的时长参数
            final StateProp<Long> duration = target.getIMMessage().duration;
            if (duration.isUnset()
                    || duration.get() == null
                    || duration.get() <= 0) {
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        IMSessionMessage.EnqueueCallback.ERROR_CODE_VIDEO_MESSAGE_VIDEO_DURATION_INVALID,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_video_message_video_duration_invalid)
                );
                return true;
            }
        }

        // 是否验证本地视频地址
        boolean validateLocalVideoPath;
        if (URLUtil.isNetworkUrl(videoPath)) {
            // 文件本身是一个网络地址
            validateLocalVideoPath = false;
        } else {
            validateLocalVideoPath = true;
            if (videoPath.startsWith("file://")) {
                videoPath = videoPath.substring(6);

                // 应用文件地址变更
                body.set(videoPath);
            }
        }

        if (validateLocalVideoPath) {
            // 校验视频文件是否存在并且文件的大小的是否合法
            final File videoFile = new File(videoPath);
            if (!videoFile.exists() || !videoFile.isFile()) {
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        IMSessionMessage.EnqueueCallback.ERROR_CODE_VIDEO_MESSAGE_VIDEO_PATH_INVALID,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_video_message_video_path_invalid)
                );
                return true;
            }
            if (IMConstants.SendMessageOption.Video.MAX_FILE_SIZE > 0
                    && videoFile.length() > IMConstants.SendMessageOption.Video.MAX_FILE_SIZE) {
                // 视频文件太大
                final String maxFileSizeAsHumanString = HumanUtil.getHumanSizeFromByte(IMConstants.SendMessageOption.Video.MAX_FILE_SIZE);
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        IMSessionMessage.EnqueueCallback.ERROR_CODE_VIDEO_MESSAGE_VIDEO_FILE_SIZE_TOO_LARGE,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_video_message_video_file_size_too_large, maxFileSizeAsHumanString)
                );
                return true;
            }
        }

        /**
         * @see IMConstants.SendMessageOption.Video#THUMB_REQUIRED
         */
        // TODO 当需要验证封面图时, 验证视频的封面图地址是否合法，网络地址或者本地地址，如果本地地址，校验文件大小

        return false;
    }

}
