package com.masonsoft.imsdk.util;

import android.graphics.BitmapFactory;

import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.IMLog;

/**
 * @since 1.0
 */
public class BitmapUtil {

    private BitmapUtil() {
    }

    /**
     * 解码出图片的宽度与高度信息，如果解码失败返回 null.<br>
     * 如果返回值不是 null, 则长度必然是 2， 并且第一个值是图片的宽度(必然大于0)，第二个值是图片的高度(必然大于0).
     */
    @Nullable
    public static int[] decodeImageSize(@Nullable String imageFilePath) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFilePath, options);
            if (options.outWidth > 0 && options.outHeight > 0) {
                return new int[]{options.outWidth, options.outHeight};
            } else {
                throw new IllegalStateException("out width:" + options.outWidth + " or height:" + options.outHeight + " invalid, imageFilePath:" + imageFilePath);
            }
        } catch (Throwable e) {
            IMLog.e(e);
        }
        return null;
    }

}
