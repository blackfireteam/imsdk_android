package com.masonsoft.imsdk.lang;

import android.net.Uri;

/**
 * @since 1.0
 */
public class ImageInfo {

    /**
     * 图片的原始宽度
     *
     * @see #rotate
     */
    public int width;
    /**
     * 图片的原始高度
     *
     * @see #rotate
     */
    public int height;
    /**
     * 图片的 mime 类型，如：image/png
     */
    public String mimeType;
    /**
     * 图片的 uri
     */
    public Uri uri;
    /**
     * 图片对应的文件内容的长度
     */
    public long length;
    /**
     * 图片的旋转角度(jpeg的图片可能有旋转角度)，取值范围 [0, 90, 180, 270]
     */
    public int rotate;

    /**
     * 视觉上的图片宽度
     */
    public int getViewWidth() {
        if (rotate == 90 || rotate == 270) {
            //noinspection SuspiciousNameCombination
            return this.height;
        }
        return this.width;
    }

    /**
     * 视觉上的图片高度
     */
    public int getViewHeight() {
        if (rotate == 90 || rotate == 270) {
            //noinspection SuspiciousNameCombination
            return this.width;
        }
        return this.height;
    }

    /**
     * 判断是否是 gif 图
     */
    public boolean isGif() {
        return "image/gif".equals(this.mimeType);
    }

    /**
     * 判断是否是 jpeg 图
     */
    public boolean isJpeg() {
        return "image/jpeg".equals(this.mimeType);
    }

}
