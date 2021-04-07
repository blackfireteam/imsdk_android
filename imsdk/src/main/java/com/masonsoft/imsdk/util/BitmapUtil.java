package com.masonsoft.imsdk.util;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import com.idonans.core.manager.TmpFileManager;
import com.idonans.core.util.ContextUtil;
import com.idonans.core.util.FileUtil;
import com.idonans.core.util.IOUtil;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.lang.ImageInfo;

import java.io.File;
import java.io.InputStream;

/**
 * @since 1.0
 */
public class BitmapUtil {

    private BitmapUtil() {
    }

    @Nullable
    private static ImageInfo decodeImageInfoFromFile(@NonNull File file) throws Throwable {
        final String imageFilePath = file.getAbsolutePath();
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
        target.length = file.length();
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
    }

    /**
     * 解码出图片的基本信息，如果解码失败返回 null.
     */
    @Nullable
    public static ImageInfo decodeImageInfo(@Nullable Uri imageUri) {
        if (imageUri == null) {
            return null;
        }

        try {
            final String scheme = imageUri.getScheme();
            final String imageUriString = imageUri.toString();
            if ("file".equalsIgnoreCase(scheme)) {
                final String filePath = imageUriString.substring("file://".length() - 1);
                final ImageInfo imageInfo = decodeImageInfoFromFile(new File(filePath));
                if (imageInfo == null) {
                    return null;
                }
                imageInfo.uri = imageUri;
                return imageInfo;
            }

            if ("content".equalsIgnoreCase(scheme)) {
                InputStream is = null;
                File tmpFile = null;
                try {
                    tmpFile = TmpFileManager.getInstance().createNewTmpFileQuietly("imsdk_bitmap_decode", null);
                    if (tmpFile == null) {
                        return null;
                    }
                    is = ContextUtil.getContext().getContentResolver().openInputStream(imageUri);
                    IOUtil.copy(is, tmpFile, null, null);
                    final String filePath = tmpFile.getAbsolutePath();
                    final ImageInfo imageInfo = decodeImageInfoFromFile(new File(filePath));
                    if (imageInfo == null) {
                        return null;
                    }
                    imageInfo.uri = imageUri;
                    return imageInfo;
                } finally {
                    IOUtil.closeQuietly(is);
                    FileUtil.deleteFileQuietly(tmpFile);
                }
            } else {
                // 猜测是一个文件地址
                final ImageInfo imageInfo = decodeImageInfoFromFile(new File(imageUriString));
                if (imageInfo == null) {
                    return null;
                }
                imageInfo.uri = imageUri;
                return imageInfo;
            }
        } catch (Throwable e) {
            IMLog.e(e);
        }
        return null;
    }

}
