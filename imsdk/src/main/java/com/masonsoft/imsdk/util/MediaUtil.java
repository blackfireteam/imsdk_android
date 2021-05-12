package com.masonsoft.imsdk.util;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.lang.MediaInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import io.github.idonans.core.manager.TmpFileManager;
import io.github.idonans.core.util.ContextUtil;
import io.github.idonans.core.util.FileUtil;
import io.github.idonans.core.util.IOUtil;
import io.github.idonans.core.util.Preconditions;

/**
 * @since 1.0
 */
public class MediaUtil {

    private MediaUtil() {
    }

    @NonNull
    private static MediaInfo decodeMediaInfoFromFile(@NonNull File file) throws Throwable {
        final MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try (FileInputStream fis = new FileInputStream(file)) {
            mediaMetadataRetriever.setDataSource(fis.getFD());
            final String mimeTypeOrigin = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            if (TextUtils.isEmpty(mimeTypeOrigin)) {
                throw new IllegalStateException("out mimeType:" + mimeTypeOrigin + " invalid, media file path:" + file.getAbsolutePath());
            }

            final String mimeType = mimeTypeOrigin.trim().toLowerCase();
            final MediaInfo mediaInfo = new MediaInfo();
            mediaInfo.mimeType = mimeType;
            mediaInfo.length = file.length();
            if (mimeType.startsWith("video/")) {
                // 视频
                final String width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                Preconditions.checkNotNull(width);
                final String height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                Preconditions.checkNotNull(height);
                final String durationMs = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                Preconditions.checkNotNull(durationMs);
                mediaInfo.width = Integer.parseInt(width);
                mediaInfo.height = Integer.parseInt(height);
                mediaInfo.durationMs = Integer.parseInt(durationMs);
                final String rotate = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                if (!TextUtils.isEmpty(rotate)) {
                    mediaInfo.rotate = Integer.parseInt(rotate);
                }
                // 解码视频封面
                final Bitmap thumb = mediaMetadataRetriever.getFrameAtTime();
                if (thumb != null) {
                    final String thumbFilePath = BitmapUtil.saveToTmpFile(thumb);
                    if (thumbFilePath != null) {
                        mediaInfo.thumbFilePath = thumbFilePath;
                    }
                }
            } else if (mimeType.startsWith("audio/")) {
                // 音频
                final String duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                Preconditions.checkNotNull(duration);
                mediaInfo.durationMs = Integer.parseInt(duration);
            } else {
                throw new IllegalStateException("out mimeType:" + mimeType + " invalid, media file path:" + file.getAbsolutePath());
            }

            return mediaInfo;
        } catch (Throwable e) {
            IMLog.e(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 解码出音频或视频的基本信息，如果解码失败返回 null.
     */
    @Nullable
    public static MediaInfo decodeMediaInfo(@Nullable Uri mediaUri) {
        if (mediaUri == null) {
            return null;
        }

        try {
            final String scheme = mediaUri.getScheme();
            final String imageUriString = mediaUri.toString();
            if ("file".equalsIgnoreCase(scheme)) {
                final String filePath = imageUriString.substring(7);
                final MediaInfo mediaInfo = decodeMediaInfoFromFile(new File(filePath));
                mediaInfo.uri = mediaUri;
                return mediaInfo;
            }

            if ("content".equalsIgnoreCase(scheme)) {
                InputStream is = null;
                File tmpFile = null;
                try {
                    tmpFile = TmpFileManager.getInstance().createNewTmpFileQuietly("__decode_media_copy_", null);
                    if (tmpFile == null) {
                        IMLog.e("tmp file create fail");
                        return null;
                    }
                    is = ContextUtil.getContext().getContentResolver().openInputStream(mediaUri);
                    IOUtil.copy(is, tmpFile, null, null);
                    final String filePath = tmpFile.getAbsolutePath();
                    final MediaInfo mediaInfo = decodeMediaInfoFromFile(new File(filePath));
                    mediaInfo.uri = mediaUri;
                    return mediaInfo;
                } finally {
                    IOUtil.closeQuietly(is);
                    FileUtil.deleteFileQuietly(tmpFile);
                }
            } else {
                // 猜测是一个文件地址
                final MediaInfo mediaInfo = decodeMediaInfoFromFile(new File(imageUriString));
                mediaInfo.uri = mediaUri;
                return mediaInfo;
            }
        } catch (Throwable e) {
            IMLog.e(e);
        }
        return null;
    }

}
