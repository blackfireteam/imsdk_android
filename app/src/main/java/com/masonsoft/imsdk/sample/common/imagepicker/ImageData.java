package com.masonsoft.imsdk.sample.common.imagepicker;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.idonans.core.WeakAbortSignal;
import com.idonans.core.thread.Threads;
import com.idonans.core.util.AbortUtil;
import com.idonans.core.util.ContextUtil;
import com.idonans.core.util.HumanUtil;
import com.idonans.core.util.IOUtil;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.util.Preconditions;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ImageData {

    private static final boolean USE_CONTENT_URI = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;

    @NonNull
    public final ImageBucket allImageInfoListBucket;

    @NonNull
    public final List<ImageBucket> allSubBuckets;

    @NonNull
    public final Map<Uri, ImageInfo> allImageInfoListMap;

    public ImageBucket bucketSelected;

    @NonNull
    public final List<ImageInfo> imageInfoListSelected = new ArrayList<>();

    @NonNull
    public final ImageSelector imageSelector;

    public ImageData(@NonNull ImageBucket allImageInfoListBucket, @NonNull List<ImageBucket> allSubBuckets, @NonNull Map<Uri, ImageInfo> allImageInfoListMap, @NonNull ImageSelector imageSelector) {
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
        public Uri uri;
        public long size;
        public int width;
        public int height;
        public String mimeType;
        public String title;
        public long addTime;
        public int id;
        @NonNull
        public ImageBucket imageBucket;

        private String bucketId;
        private String bucketDisplayName;

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
            return Objects.equals(uri, imageInfo.uri);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.uri);
        }

        @NonNull
        public String toShortString() {
            //noinspection StringBufferReplaceableByString
            final StringBuilder builder = new StringBuilder();
            builder.append("ImageInfo");
            builder.append(" uri:").append(this.uri);
            builder.append(" size:").append(this.size).append(" ").append(HumanUtil.getHumanSizeFromByte(this.size));
            builder.append(" width:").append(this.width);
            builder.append(" height:").append(this.height);
            builder.append(" mimeType:").append(this.mimeType);
            builder.append(" title:").append(this.title);
            builder.append(" addTime:").append(this.addTime);
            builder.append(" id:").append(this.id);
            builder.append(" bucketId:").append(this.bucketId);
            builder.append(" bucketDisplayName:").append(this.bucketDisplayName);
            return builder.toString();
        }

        @Override
        @NonNull
        public String toString() {
            return this.toShortString();
        }
    }

    public static class ImageBucket {
        /**
         * 是否是总的那个 bucket, 包含了所有的图片
         */
        public boolean allImageInfo;
        public String bucketDisplayName;
        public String bucketId;
        @Nullable
        public ImageInfo cover;

        @NonNull
        public final List<ImageInfo> imageInfoList = new ArrayList<>();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ImageBucket that = (ImageBucket) o;
            return allImageInfo == that.allImageInfo &&
                    ObjectsCompat.equals(bucketId, that.bucketId);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(allImageInfo, bucketId);
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
            final ImageBucket allImageInfoBucket = new ImageBucket();
            allImageInfoBucket.allImageInfo = true;

            final List<ImageBucket> allBuckets = new ArrayList<>();
            allBuckets.add(allImageInfoBucket);

            final Map<Uri, ImageInfo> allImageInfoMap = new HashMap<>();

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

                    if (allImageInfoBucket.cover == null) {
                        allImageInfoBucket.cover = itemImageInfo;
                    }
                    allImageInfoBucket.imageInfoList.add(itemImageInfo);
                    allImageInfoMap.put(itemImageInfo.uri, itemImageInfo);

                    ImageBucket newBucket = createImageBucket(itemImageInfo);
                    ImageBucket oldBucket = queryOldImageBucket(allBuckets, newBucket);
                    ImageBucket targetBucket;
                    if (oldBucket != null) {
                        // update old bucket
                        oldBucket.imageInfoList.add(itemImageInfo);
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
                    callback.onLoadFinish(new ImageData(allImageInfoBucket, allBuckets, allImageInfoMap, mImageSelector));
                }
            } catch (Throwable e) {
                SampleLog.e(e);
                ImageLoaderCallback callback = getCallback();
                if (callback != null) {
                    callback.onLoadFinish(new ImageData(allImageInfoBucket, allBuckets, allImageInfoMap, mImageSelector));
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
            target.imageInfoList.add(imageInfo);
            target.bucketId = imageInfo.bucketId;
            target.bucketDisplayName = imageInfo.bucketDisplayName;
            return target;
        }

        @NonNull
        private String[] allColumns() {
            if (USE_CONTENT_URI) {
                return new String[]{
                        //////////////////////////////////////////////////////
                        //////////////////////////////////////////////////////
                        MediaStore.Images.Media.SIZE,           //图片的大小，long型  132492
                        MediaStore.Images.Media.WIDTH,          //图片的宽度，int型  1920
                        MediaStore.Images.Media.HEIGHT,         //图片的高度，int型  1080
                        MediaStore.Images.Media.MIME_TYPE,      //图片的类型     image/jpeg
                        MediaStore.Images.Media.TITLE,
                        MediaStore.Images.Media.DATE_ADDED,    //添加时间
                        MediaStore.Images.Media._ID,      //id
                        //////////////////////////////////////////////////////
                        //////////////////////////////////////////////////////
                        MediaStore.Images.Media.BUCKET_ID,
                        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                        //////////////////////////////////////////////////////
                        //////////////////////////////////////////////////////
                };
            } else {
                return new String[]{
                        //////////////////////////////////////////////////////
                        //////////////////////////////////////////////////////
                        MediaStore.Images.Media.SIZE,           //图片的大小，long型  132492
                        MediaStore.Images.Media.WIDTH,          //图片的宽度，int型  1920
                        MediaStore.Images.Media.HEIGHT,         //图片的高度，int型  1080
                        MediaStore.Images.Media.MIME_TYPE,      //图片的类型     image/jpeg
                        MediaStore.Images.Media.TITLE,
                        MediaStore.Images.Media.DATE_ADDED,    //添加时间
                        MediaStore.Images.Media._ID,      //id
                        //////////////////////////////////////////////////////
                        //////////////////////////////////////////////////////
                        MediaStore.Images.Media.DATA,           // 图片的真实路径  /storage/emulated/0/pp/downloader/wallpaper/aaa.jpg
                        //////////////////////////////////////////////////////
                        //////////////////////////////////////////////////////
                };
            }
        }

        @Nullable
        private ImageInfo cursorToImageInfo(Cursor cursor) {
            ImageInfo target = new ImageInfo();
            int index = -1;
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

            if (USE_CONTENT_URI) {
                target.bucketId = cursor.getString(++index);
                target.bucketDisplayName = cursor.getString(++index);
                target.uri = MediaStore.Images.Media
                        .EXTERNAL_CONTENT_URI
                        .buildUpon()
                        .appendPath(String.valueOf(target.id))
                        .build();
            } else {
                final String path = cursor.getString(++index);
                if (TextUtils.isEmpty(path)) {
                    SampleLog.v("invalid path:%s, target:%s", path, target);
                    return null;
                }
                final File dir = new File(path).getParentFile();
                Preconditions.checkNotNull(dir);
                target.bucketId = dir.getAbsolutePath();
                target.bucketDisplayName = dir.getName();
                target.uri = Uri.fromFile(new File(path));
            }

            IMLog.v("cursorToImageInfo USE_CONTENT_URI:%s -> %s", USE_CONTENT_URI, target);

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
