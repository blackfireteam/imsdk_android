package com.masonsoft.imsdk.lang;

import android.net.Uri;

/**
 * @since 1.0
 */
public class MediaInfo {

    /**
     * 媒体内容宽度(视频格式时有效)
     */
    public int width;
    /**
     * 媒体内容高度(视频格式时有效)
     */
    public int height;
    /**
     * 媒体的 mime 类型，如：audio/mp4
     */
    public String mimeType;
    /**
     * 媒体的 uri
     */
    public Uri uri;
    /**
     * 媒体对应的文件内容的长度
     */
    public long length;
    /**
     * 媒体内容的时长, 音频或视频的时长 ms
     */
    public long durationMs;
    /**
     * 视频的旋转角度(视频可能有旋转角度)，取值范围 [0, 90, 180, 270]
     */
    public int rotate;
    /**
     * 缩略图文件，如解码出的视频的缩略图
     */
    public String thumbFilePath;

    /**
     * 视觉上的视频宽度
     */
    public int getViewWidth() {
        if (rotate == 90 || rotate == 270) {
            //noinspection SuspiciousNameCombination
            return this.height;
        }
        return this.width;
    }

    /**
     * 视觉上的视频高度
     */
    public int getViewHeight() {
        if (rotate == 90 || rotate == 270) {
            //noinspection SuspiciousNameCombination
            return this.width;
        }
        return this.height;
    }

    public boolean isVideo() {
        return this.mimeType != null && this.mimeType.startsWith("video/");
    }

    public boolean isAudio() {
        return this.mimeType != null && this.mimeType.startsWith("audio/");
    }

}
