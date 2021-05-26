package com.masonsoft.imsdk.core.processor;

import android.net.Uri;
import android.text.TextUtils;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMSessionMessage;
import com.masonsoft.imsdk.lang.GeneralResult;
import com.masonsoft.imsdk.lang.ImageInfo;
import com.masonsoft.imsdk.lang.StateProp;
import com.masonsoft.imsdk.util.BitmapUtil;

import io.github.idonans.core.util.HumanUtil;

/**
 * 发送图片类型的消息合法性检查
 *
 * @since 1.0
 */
public class SendSessionMessageTypeImageValidateProcessor extends SendSessionMessageTypeValidateProcessor {

    public SendSessionMessageTypeImageValidateProcessor() {
        super(IMConstants.MessageType.IMAGE);
    }

    @Override
    protected boolean doTypeProcess(@NonNull IMSessionMessage target, int type) {
        final StateProp<String> body = target.getMessage().body;
        if (body.isUnset()) {
            target.getEnqueueCallback().onCallback(
                    GeneralResult.valueOf(GeneralResult.ERROR_CODE_IMAGE_MESSAGE_IMAGE_PATH_UNSET)
                            .withPayload(target)
            );
            return true;
        }

        final String bodyUri = body.get();
        if (TextUtils.isEmpty(bodyUri)) {
            target.getEnqueueCallback().onCallback(
                    GeneralResult.valueOf(GeneralResult.ERROR_CODE_IMAGE_MESSAGE_IMAGE_PATH_INVALID)
                            .withPayload(target)
            );
            return true;
        }
        final Uri imageUri = Uri.parse(bodyUri);

        // 是否需要解码图片尺寸信息
        boolean requireDecodeImageSize = true;

        final StateProp<Long> width = target.getMessage().width;
        final StateProp<Long> height = target.getMessage().height;
        if (!width.isUnset() && width.get() != null && width.get() > 0
                && !height.isUnset() && height.get() != null && height.get() > 0) {
            // 已经设置了合法的宽高值
            requireDecodeImageSize = false;
        }

        if (URLUtil.isNetworkUrl(bodyUri)) {
            // 文件本身是一个网络地址
            if (requireDecodeImageSize) {
                // 网络地址的图片没有设置合法的宽高值，直接报错(不适宜去下载图片再解码宽高值，会阻塞队列执行速度)
                target.getEnqueueCallback().onCallback(
                        GeneralResult.valueOf(GeneralResult.ERROR_CODE_IMAGE_MESSAGE_IMAGE_WIDTH_OR_HEIGHT_INVALID)
                                .withPayload(target)
                );
                return true;
            }
        } else {
            // 分析图片信息
            final ImageInfo imageInfo = BitmapUtil.decodeImageInfo(imageUri);
            if (imageInfo == null) {
                // 解码图片信息失败, 通常来说都是由于图片格式不支持导致(或者图片 Uri 指向的不是一张真实的图片)
                target.getEnqueueCallback().onCallback(
                        GeneralResult.valueOf(GeneralResult.ERROR_CODE_IMAGE_MESSAGE_IMAGE_FORMAT_NOT_SUPPORT)
                                .withPayload(target)
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
                target.getEnqueueCallback().onCallback(
                        GeneralResult.valueOf(GeneralResult.ERROR_CODE_IMAGE_MESSAGE_IMAGE_FILE_SIZE_TOO_LARGE)
                                .withPayload(target)
                );
                return true;
            }
        }

        return false;
    }

}
