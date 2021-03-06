package com.masonsoft.imsdk.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.lang.ImageInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import io.github.idonans.core.manager.TmpFileManager;
import io.github.idonans.core.util.ContextUtil;
import io.github.idonans.core.util.FileUtil;
import io.github.idonans.core.util.IOUtil;

/**
 * @since 1.0
 */
public class BitmapUtil {

    private BitmapUtil() {
    }

    @NonNull
    private static ImageInfo decodeImageInfoFromFile(@NonNull File file) throws Throwable {
        final String imageFilePath = file.getAbsolutePath();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFilePath, options);
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw new IllegalStateException("out width:" + options.outWidth + " or height:" + options.outHeight + " invalid, imageFilePath:" + imageFilePath);
        }
        final String mimeTypeOrigin = options.outMimeType;
        if (TextUtils.isEmpty(mimeTypeOrigin)) {
            throw new IllegalStateException("out mimeType:" + mimeTypeOrigin + " invalid, imageFilePath:" + imageFilePath);
        }
        final String mimeType = mimeTypeOrigin.trim().toLowerCase();
        final ImageInfo target = new ImageInfo();
        target.width = options.outWidth;
        target.height = options.outHeight;
        target.length = file.length();
        target.mimeType = mimeType;

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
                final String filePath = imageUriString.substring(7);
                final ImageInfo imageInfo = decodeImageInfoFromFile(new File(filePath));
                imageInfo.uri = imageUri;
                return imageInfo;
            }

            if ("content".equalsIgnoreCase(scheme)) {
                InputStream is = null;
                File tmpFile = null;
                try {
                    tmpFile = TmpFileManager.getInstance().createNewTmpFileQuietly("__decode_image_copy_", null);
                    if (tmpFile == null) {
                        IMLog.e("tmp file create fail");
                        return null;
                    }
                    is = ContextUtil.getContext().getContentResolver().openInputStream(imageUri);
                    IOUtil.copy(is, tmpFile, null, null);
                    final String filePath = tmpFile.getAbsolutePath();
                    final ImageInfo imageInfo = decodeImageInfoFromFile(new File(filePath));
                    imageInfo.uri = imageUri;
                    return imageInfo;
                } finally {
                    IOUtil.closeQuietly(is);
                    FileUtil.deleteFileQuietly(tmpFile);
                }
            } else {
                // 猜测是一个文件地址
                final ImageInfo imageInfo = decodeImageInfoFromFile(new File(imageUriString));
                imageInfo.uri = imageUri;
                return imageInfo;
            }
        } catch (Throwable e) {
            IMLog.w(e);
        }
        return null;
    }

    /**
     * 将图片保存至临时文件
     */
    @Nullable
    public static String saveToTmpFile(Bitmap bitmap) {
        return saveToTmpFile(bitmap, false);
    }

    /**
     * 将图片保存至临时文件
     */
    @Nullable
    public static String saveToTmpFile(Bitmap bitmap, boolean png) {
        boolean success = false;
        File file = null;
        try {
            file = TmpFileManager.getInstance().createNewTmpFileQuietly("__tmp_bitmap_", png ? ".png" : ".jpeg");
            if (file == null) {
                return null;
            }

            try (OutputStream os = new FileOutputStream(file)) {
                if (bitmap.compress(
                        png ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG,
                        85,
                        os)) {
                    success = true;
                    return file.getAbsolutePath();
                }
            }
        } catch (Throwable e) {
            IMLog.w(e);
        } finally {
            if (!success) {
                FileUtil.deleteFileQuietly(file);
            }
        }
        return null;
    }

}
