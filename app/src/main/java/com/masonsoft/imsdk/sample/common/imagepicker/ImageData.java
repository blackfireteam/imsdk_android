package com.masonsoft.imsdk.sample.common.imagepicker;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.google.common.base.Preconditions;
import com.idonans.core.WeakAbortSignal;
import com.idonans.core.thread.Threads;
import com.idonans.core.util.AbortUtil;
import com.idonans.core.util.ContextUtil;
import com.idonans.core.util.IOUtil;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.SampleLog;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ImageData {

    @NonNull
    public final ImageBucket allImageInfoListBucket;

    @NonNull
    public final List<ImageBucket> allSubBuckets;

    @NonNull
    public final Map<String, ImageInfo> allImageInfoListMap;

    public ImageBucket bucketSelected;

    @NonNull
    public final List<ImageInfo> imageInfoListSelected = new ArrayList<>();

    @NonNull
    public final ImageSelector imageSelector;

    public ImageData(@NonNull ImageBucket allImageInfoListBucket, @NonNull List<ImageBucket> allSubBuckets, @NonNull Map<String, ImageInfo> allImageInfoListMap, @NonNull ImageSelector imageSelector) {
        this.allImageInfoListBucket = allImageInfoListBucket;
        this.allSubBuckets = allSubBuckets;
        this.allImageInfoListMap = allImageInfoListMap;
        this.imageSelector = imageSelector;
    }

    /**
     * 获取选择图片的选中顺序，如果没有选中返回 -1.
     *
     * @param imageInfo
     * @return
     */
    public int indexOfSelected(ImageInfo imageInfo) {
        return imageInfoListSelected.indexOf(imageInfo);
    }

    public static class ImageInfo {
        @NonNull
        public String path;
        public long size;
        public int width;
        public int height;
        public String mimeType;
        public String title;
        public long addTime;
        public int id;
        @NonNull
        public ImageBucket imageBucket;

        public boolean isImageMimeType() {
            return this.mimeType != null && this.mimeType.startsWith("image/");
        }

        /**
         * 估算图片解码到内存之后的 byte 大小
         */
        public long getImageMemorySize() {
            return this.width * this.height * 4;
        }

        public boolean isImageMemorySizeTooLarge() {
            return getImageMemorySize() > Constants.SELECTOR_MAX_IMAGE_SIZE;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ImageInfo imageInfo = (ImageInfo) o;
            return Objects.equals(path, imageInfo.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }
    }

    public static class ImageBucket {
        /**
         * 是否是总的那个 bucket, 包含了所有的图片
         */
        public boolean allImageInfos;
        public String name;
        public String path;
        @Nullable
        public ImageInfo cover;

        @NonNull
        public final List<ImageInfo> imageInfos = new ArrayList<>();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ImageBucket that = (ImageBucket) o;
            return allImageInfos == that.allImageInfos &&
                    ObjectsCompat.equals(path, that.path);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(allImageInfos, path);
        }
    }

    public interface ImageLoaderCallback {
        void onLoadFinish(@NonNull ImageData imageData);
    }

    public static class ImageLoader extends WeakAbortSignal implements Runnable, Closeable {

        private final ImageSelector mImageSelector;

        public ImageLoader(ImageLoaderCallback callback, ImageSelector imageSelector) {
            super(callback);
            if (imageSelector == null) {
                imageSelector = new ImageSelector.SimpleImageSelector();
            }
            mImageSelector = imageSelector;
        }

        public void start() {
            Threads.postBackground(this);
        }

        @Nullable
        private ImageLoaderCallback getCallback() {
            ImageLoaderCallback callback = (ImageLoaderCallback) getObject();
            if (isAbort()) {
                return null;
            }
            return callback;
        }

        @Override
        public void run() {
            final ImageBucket allImageInfosBucket = new ImageBucket();
            allImageInfosBucket.allImageInfos = true;

            final List<ImageBucket> allBuckets = new ArrayList<>();
            allBuckets.add(allImageInfosBucket);

            final Map<String, ImageInfo> allImageInfosMap = new HashMap<>();

            Cursor cursor = null;
            try {
                AbortUtil.throwIfAbort(this);

                Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver contentResolver = ContextUtil.getContext().getContentResolver();
                cursor = contentResolver.query(uri, allColumns(), null, null, null);
                Preconditions.checkNotNull(cursor);

                for (cursor.moveToLast(); !cursor.isBeforeFirst(); cursor.moveToPrevious()) {
                    AbortUtil.throwIfAbort(this);

                    ImageInfo itemImageInfo = cursorToImageInfo(cursor);
                    if (itemImageInfo == null) {
                        continue;
                    }

                    if (!mImageSelector.accept(itemImageInfo)) {
                        continue;
                    }

                    if (allImageInfosBucket.cover == null) {
                        allImageInfosBucket.cover = itemImageInfo;
                    }
                    allImageInfosBucket.imageInfos.add(itemImageInfo);
                    allImageInfosMap.put(itemImageInfo.path, itemImageInfo);

                    ImageBucket newBucket = createImageBucket(itemImageInfo);
                    ImageBucket oldBucket = queryOldImageBucket(allBuckets, newBucket);
                    ImageBucket targetBucket;
                    if (oldBucket != null) {
                        // update old bucket
                        oldBucket.imageInfos.add(itemImageInfo);
                        targetBucket = oldBucket;
                    } else {
                        // add new bucket;
                        allBuckets.add(newBucket);
                        targetBucket = newBucket;
                    }
                    itemImageInfo.imageBucket = targetBucket;
                }

                AbortUtil.throwIfAbort(this);
                ImageLoaderCallback callback = getCallback();
                if (callback != null) {
                    callback.onLoadFinish(new ImageData(allImageInfosBucket, allBuckets, allImageInfosMap, mImageSelector));
                }
            } catch (Throwable e) {
                SampleLog.e(e);
                ImageLoaderCallback callback = getCallback();
                if (callback != null) {
                    callback.onLoadFinish(new ImageData(allImageInfosBucket, allBuckets, allImageInfosMap, mImageSelector));
                }
            } finally {
                IOUtil.closeQuietly(cursor);
            }
        }

        @Nullable
        private ImageBucket queryOldImageBucket(List<ImageBucket> allSubBuckets, ImageBucket query) {
            for (ImageBucket oldBucket : allSubBuckets) {
                if (ObjectsCompat.equals(oldBucket, query)) {
                    return oldBucket;
                }
            }
            return null;
        }

        @NonNull
        private ImageBucket createImageBucket(ImageInfo imageInfo) {
            ImageBucket target = new ImageBucket();
            target.cover = imageInfo;
            target.imageInfos.add(imageInfo);

            File dir = new File(imageInfo.path).getParentFile();
            target.path = dir.getAbsolutePath();
            target.name = dir.getName();

            return target;
        }

        @NonNull
        private String[] allColumns() {
            return new String[]{
                    MediaStore.Images.Media.DATA,           //图片的真实路径  /storage/emulated/0/pp/downloader/wallpaper/aaa.jpg
                    MediaStore.Images.Media.SIZE,           //图片的大小，long型  132492
                    MediaStore.Images.Media.WIDTH,          //图片的宽度，int型  1920
                    MediaStore.Images.Media.HEIGHT,         //图片的高度，int型  1080
                    MediaStore.Images.Media.MIME_TYPE,      //图片的类型     image/jpeg
                    MediaStore.Images.Media.TITLE,
                    MediaStore.Images.Media.DATE_ADDED,    //添加时间
                    MediaStore.Images.Media._ID      //id
            };
        }

        @Nullable
        private ImageInfo cursorToImageInfo(Cursor cursor) {
            ImageInfo target = new ImageInfo();
            int index = -1;
            target.path = cursor.getString(++index);
            target.size = cursor.getLong(++index);
            target.width = cursor.getInt(++index);
            target.height = cursor.getInt(++index);
            target.mimeType = cursor.getString(++index);
            if (target.mimeType != null) {
                target.mimeType = target.mimeType.trim().toLowerCase();
            }
            target.title = cursor.getString(++index);
            target.addTime = cursor.getLong(++index);
            target.id = cursor.getInt(++index);

            if (TextUtils.isEmpty(target.path)) {
                SampleLog.v("invalid path:%s", target.path);
                return null;
            }
            if (TextUtils.isEmpty(target.mimeType)) {
                SampleLog.v("invalid mimeType:%s", target.mimeType);
                return null;
            }
            return target;
        }

        @Override
        public void close() {
            setAbort();
        }

    }

}
