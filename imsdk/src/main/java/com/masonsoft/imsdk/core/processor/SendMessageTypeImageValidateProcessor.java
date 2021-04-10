package com.masonsoft.imsdk.core.processor;

import android.net.Uri;
import android.text.TextUtils;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.IMSessionMessage;
import com.masonsoft.imsdk.R;
import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.lang.ImageInfo;
import com.masonsoft.imsdk.lang.StateProp;
import com.masonsoft.imsdk.util.BitmapUtil;

import io.github.idonans.core.util.HumanUtil;

/**
 * 发送图片类型的消息合法性检查
 *
 * @since 1.0
 */
public class SendMessageTypeImageValidateProcessor extends SendMessageTypeValidateProcessor {

    public SendMessageTypeImageValidateProcessor() {
        super(IMConstants.MessageType.IMAGE);
    }

    @Override
    protected boolean doTypeProcess(@NonNull IMSessionMessage target, int type) {
        final StateProp<String> body = target.getIMMessage().body;
        if (body.isUnset()) {
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    IMSessionMessage.EnqueueCallback.ERROR_CODE_IMAGE_MESSAGE_IMAGE_PATH_UNSET,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_image_message_image_path_unset)
            );
            return true;
        }

        final String bodyUri = body.get();
        if (TextUtils.isEmpty(bodyUri)) {
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    IMSessionMessage.EnqueueCallback.ERROR_CODE_IMAGE_MESSAGE_IMAGE_PATH_INVALID,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_image_message_image_path_invalid)
            );
            return true;
        }
        final Uri imageUri = Uri.parse(bodyUri);

        // 是否需要解码图片尺寸信息
        boolean requireDecodeImageSize = true;

        final StateProp<Long> width = target.getIMMessage().width;
        final StateProp<Long> height = target.getIMMessage().height;
        if (!width.isUnset() && width.get() != null && width.get() > 0
                && !height.isUnset() && height.get() != null && height.get() > 0) {
            // 已经设置了合法的宽高值
            requireDecodeImageSize = false;
        }

        if (URLUtil.isNetworkUrl(bodyUri)) {
            // 文件本身是一个网络地址
            if (requireDecodeImageSize) {
                // 网络地址的图片没有设置合法的宽高值，直接报错(不适宜去下载图片再解码宽高值，会阻塞队列执行速度)
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        IMSessionMessage.EnqueueCallback.ERROR_CODE_IMAGE_MESSAGE_IMAGE_WIDTH_OR_HEIGHT_INVALID,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_image_message_image_width_or_height_invalid)
                );
                return true;
            }
        } else {
            // 分析图片信息
            final ImageInfo imageInfo = BitmapUtil.decodeImageInfo(imageUri);
            if (imageInfo == null) {
                // 解码图片信息失败, 通常来说都是由于图片格式不支持导致(或者图片 Uri 指向的不是一张真实的图片)
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        IMSessionMessage.EnqueueCallback.ERROR_CODE_IMAGE_MESSAGE_IMAGE_FORMAT_NOT_SUPPORT,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_image_message_image_format_not_support)
                );
                return true;
            }

            // 设置图片宽高值(覆盖)
            width.set((long) imageInfo.getViewWidth());
            height.set((long) imageInfo.getViewHeight());

            // 校验图片文件的大小的是否合法
            if (IMConstants.SendMessageOption.Image.MAX_FILE_SIZE > 0
                    && imageInfo.length > IMConstants.SendMessageOption.Image.MAX_FILE_SIZE) {
                // 图片文件太大
                final String maxFileSizeAsHumanString = HumanUtil.getHumanSizeFromByte(IMConstants.SendMessageOption.Image.MAX_FILE_SIZE);
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        IMSessionMessage.EnqueueCallback.ERROR_CODE_IMAGE_MESSAGE_IMAGE_FILE_SIZE_TOO_LARGE,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_image_message_image_file_size_too_large, maxFileSizeAsHumanString)
                );
                return true;
            }
        }

        return false;
    }

}
