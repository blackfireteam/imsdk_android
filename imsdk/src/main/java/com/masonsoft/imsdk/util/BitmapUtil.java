package com.masonsoft.imsdk.util;

import android.graphics.BitmapFactory;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.lang.ImageInfo;

/**
 * @since 1.0
 */
public class BitmapUtil {

    private BitmapUtil() {
    }

    /**
     * 解码出图片的基本信息，如果解码失败返回 null.
     */
    @Nullable
    public static ImageInfo decodeImageInfo(@Nullable String imageFilePath) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFilePath, options);
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                throw new IllegalStateException("out width:" + options.outWidth + " or height:" + options.outHeight + " invalid, imageFilePath:" + imageFilePath);
            }
            final String mimeType = options.outMimeType;
            if (TextUtils.isEmpty(mimeType)) {
                throw new IllegalStateException("out mimeType:" + mimeType + " invalid, imageFilePath:" + imageFilePath);
            }
            final ImageInfo target = new ImageInfo();
            target.width = options.outWidth;
            target.height = options.outHeight;
            target.filePath = imageFilePath;
            target.mimeType = mimeType.toLowerCase();

            // 如果是 jpeg, 校验图片的旋转方向
            if (target.isJpeg()) {
                final ExifInterface exif = new ExifInterface(imageFilePath);
                final int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                    target.rotate = 90;
                } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                    target.rotate = 180;
                } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                    target.rotate = 270;
                }
            }

            return target;
        } catch (Throwable e) {
            IMLog.e(e);
        }
        return null;
    }

}
