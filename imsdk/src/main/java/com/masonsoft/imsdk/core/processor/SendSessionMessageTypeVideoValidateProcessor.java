package com.masonsoft.imsdk.core.processor;

import android.net.Uri;
import android.text.TextUtils;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.EnqueueCallback;
import com.masonsoft.imsdk.IMSessionMessage;
import com.masonsoft.imsdk.R;
import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.lang.MediaInfo;
import com.masonsoft.imsdk.lang.StateProp;
import com.masonsoft.imsdk.util.MediaUtil;

import java.io.File;

import io.github.idonans.core.util.HumanUtil;

/**
 * 发送视频类型的消息合法性检查
 *
 * @since 1.0
 */
public class SendSessionMessageTypeVideoValidateProcessor extends SendSessionMessageTypeValidateProcessor {

    public SendSessionMessageTypeVideoValidateProcessor() {
        super(IMConstants.MessageType.VIDEO);
    }

    @Override
    protected boolean doTypeProcess(@NonNull IMSessionMessage target, int type) {
        // 验证视频信息
        if (validateVideo(target)) {
            return true;
        }

        // 验证封面图信息
        if (validateThumb(target)) {
            return true;
        }

        // 验证时长信息
        return validateDuration(target);
    }

    private boolean validateVideo(@NonNull IMSessionMessage target) {
        final StateProp<String> body = target.getIMMessage().body;
        if (body.isUnset()) {
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    EnqueueCallback.ERROR_CODE_VIDEO_MESSAGE_VIDEO_PATH_UNSET,
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
                    EnqueueCallback.ERROR_CODE_VIDEO_MESSAGE_VIDEO_PATH_INVALID,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_video_message_video_path_invalid)
            );
            return true;
        }

        final Uri videoUri = Uri.parse(videoPath);

        // 是否需要解码视频尺寸信息
        boolean requireDecodeVideoSize = true;

        final StateProp<Long> width = target.getIMMessage().width;
        final StateProp<Long> height = target.getIMMessage().height;
        if (!width.isUnset() && width.get() != null && width.get() > 0
                && !height.isUnset() && height.get() != null && height.get() > 0) {
            // 已经设置了合法的宽高值
            requireDecodeVideoSize = false;
        }

        if (URLUtil.isNetworkUrl(videoPath)) {
            // 视频本身是一个网络地址
            if (requireDecodeVideoSize) {
                // 网络地址的视频没有设置合法的宽高值，直接报错
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        EnqueueCallback.ERROR_CODE_VIDEO_MESSAGE_VIDEO_WIDTH_OR_HEIGHT_INVALID,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_video_message_video_width_or_height_invalid)
                );
                return true;
            }
        } else {
            // 分析视频信息
            final MediaInfo mediaInfo = MediaUtil.decodeMediaInfo(videoUri);
            if (mediaInfo == null || !mediaInfo.isVideo()) {
                // 解码视频信息失败, 通常来说都是由于视频格式不支持导致(或者视频 Uri 指向的不是一个真实的视频)
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        EnqueueCallback.ERROR_CODE_VIDEO_MESSAGE_VIDEO_FORMAT_NOT_SUPPORT,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_video_message_video_format_not_support)
                );
                return true;
            }

            width.set((long) mediaInfo.getViewWidth());
            height.set((long) mediaInfo.getViewHeight());

            // 校验视频文件的大小的是否合法
            if (IMConstants.SendMessageOption.Video.MAX_FILE_SIZE > 0
                    && mediaInfo.length > IMConstants.SendMessageOption.Video.MAX_FILE_SIZE) {
                // 视频文件太大
                final String maxFileSizeAsHumanString = HumanUtil.getHumanSizeFromByte(IMConstants.SendMessageOption.Video.MAX_FILE_SIZE);
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        EnqueueCallback.ERROR_CODE_VIDEO_MESSAGE_VIDEO_FILE_SIZE_TOO_LARGE,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_video_message_video_file_size_too_large, maxFileSizeAsHumanString)
                );
                return true;
            }
        }

        return false;
    }

    private boolean validateThumb(@NonNull IMSessionMessage target) {
        if (!IMConstants.SendMessageOption.Video.THUMB_REQUIRED) {
            return false;
        }

        final StateProp<String> thumb = target.getIMMessage().thumb;

        if (thumb.isUnset()) {
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    EnqueueCallback.ERROR_CODE_VIDEO_MESSAGE_VIDEO_THUMB_PATH_UNSET,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_video_message_video_thumb_path_unset)
            );
            return true;
        }

        String thumbPath = thumb.get();
        if (thumbPath != null) {
            thumbPath = thumbPath.trim();

            // 应用文件地址变更
            thumb.set(thumbPath);
        }
        if (TextUtils.isEmpty(thumbPath)) {
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    EnqueueCallback.ERROR_CODE_VIDEO_MESSAGE_VIDEO_THUMB_PATH_INVALID,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_video_message_video_thumb_path_invalid)
            );
            return true;
        }


        // 是否验证本地封面地址
        boolean validateLocalThumbPath;
        if (URLUtil.isNetworkUrl(thumbPath)) {
            // 文件本身是一个网络地址
            validateLocalThumbPath = false;
        } else {
            validateLocalThumbPath = true;
            if (thumbPath.startsWith("file://")) {
                thumbPath = thumbPath.substring(7);

                // 应用文件地址变更
                thumb.set(thumbPath);
            }
        }

        if (validateLocalThumbPath) {
            // 校验封面文件是否存在并且文件的大小的是否合法
            final File thumbFile = new File(thumbPath);
            if (!thumbFile.exists() || !thumbFile.isFile()) {
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        EnqueueCallback.ERROR_CODE_VIDEO_MESSAGE_VIDEO_THUMB_PATH_INVALID,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_video_message_video_thumb_path_invalid)
                );
                return true;
            }
            if (IMConstants.SendMessageOption.Video.MAX_THUMB_FILE_SIZE > 0
                    && thumbFile.length() > IMConstants.SendMessageOption.Video.MAX_THUMB_FILE_SIZE) {
                // 封面文件太大
                final String maxFileSizeAsHumanString = HumanUtil.getHumanSizeFromByte(IMConstants.SendMessageOption.Video.MAX_THUMB_FILE_SIZE);
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        EnqueueCallback.ERROR_CODE_VIDEO_MESSAGE_VIDEO_THUMB_FILE_SIZE_TOO_LARGE,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_video_message_video_thumb_file_size_too_large, maxFileSizeAsHumanString)
                );
                return true;
            }
        }

        return false;
    }

    private boolean validateDuration(@NonNull IMSessionMessage target) {
        if (!IMConstants.SendMessageOption.Video.DURATION_REQUIRED) {
            return false;
        }

        // 必须要有合法的时长参数
        final StateProp<Long> duration = target.getIMMessage().durationMs;
        if (duration.isUnset()
                || duration.get() == null
                || duration.get() <= 0) {
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    EnqueueCallback.ERROR_CODE_VIDEO_MESSAGE_VIDEO_DURATION_INVALID,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_video_message_video_duration_invalid)
            );
            return true;
        }

        return false;
    }

}
