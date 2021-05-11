package com.masonsoft.imsdk.sample.common.mediapicker;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.SampleLog;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.idonans.core.WeakAbortSignal;
import io.github.idonans.core.thread.Threads;
import io.github.idonans.core.util.AbortUtil;
import io.github.idonans.core.util.ContextUtil;
import io.github.idonans.core.util.HumanUtil;
import io.github.idonans.core.util.IOUtil;
import io.github.idonans.core.util.Preconditions;

public class MediaData {

    private static final boolean USE_CONTENT_URI = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;

    @NonNull
    public final MediaBucket allMediaInfoListBucket;

    @NonNull
    public final List<MediaBucket> allSubBuckets;

    @NonNull
    public final Map<Uri, MediaInfo> allMediaInfoListMap;

    public MediaBucket bucketSelected;

    @NonNull
    public final List<MediaInfo> mMediaInfoListSelected = new ArrayList<>();

    @NonNull
    public final MediaSelector mMediaSelector;

    public MediaData(@NonNull MediaBucket allMediaInfoListBucket, @NonNull List<MediaBucket> allSubBuckets, @NonNull Map<Uri, MediaInfo> allMediaInfoListMap, @NonNull MediaSelector mediaSelector) {
        this.allMediaInfoListBucket = allMediaInfoListBucket;
        this.allSubBuckets = allSubBuckets;
        this.allMediaInfoListMap = allMediaInfoListMap;
        this.mMediaSelector = mediaSelector;
    }

    /**
     * 获取选择 media 的选中顺序，如果没有选中返回 -1.
     *
     * @param mediaInfo
     * @return
     */
    public int indexOfSelected(MediaInfo mediaInfo) {
        return mMediaInfoListSelected.indexOf(mediaInfo);
    }

    public static class MediaInfo {
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
        public MediaBucket mMediaBucket;

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
            MediaInfo mediaInfo = (MediaInfo) o;
            return Objects.equals(uri, mediaInfo.uri);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.uri);
        }

        @NonNull
        public String toShortString() {
            //noinspection StringBufferReplaceableByString
            final StringBuilder builder = new StringBuilder();
            builder.append(com.masonsoft.imsdk.util.Objects.defaultObjectTag(this));
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

    public static class MediaBucket {
        /**
         * 是否是总的那个 bucket, 包含了所有的图片
         */
        public boolean allMediaInfo;
        public String bucketDisplayName;
        public String bucketId;
        @Nullable
        public MediaInfo cover;

        @NonNull
        public final List<MediaInfo> mediaInfoList = new ArrayList<>();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MediaBucket that = (MediaBucket) o;
            return allMediaInfo == that.allMediaInfo &&
                    ObjectsCompat.equals(bucketId, that.bucketId);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(allMediaInfo, bucketId);
        }
    }

    public interface MediaLoaderCallback {
        void onLoadFinish(@NonNull MediaData mediaData);
    }

    public static class MediaLoader extends WeakAbortSignal implements Runnable, Closeable {

        private final MediaSelector mMediaSelector;

        public MediaLoader(MediaLoaderCallback callback, MediaSelector mediaSelector) {
            super(callback);
            if (mediaSelector == null) {
                mediaSelector = new MediaSelector.SimpleMediaSelector();
            }
            mMediaSelector = mediaSelector;
        }

        public void start() {
            Threads.postBackground(this);
        }

        @Nullable
        private MediaLoaderCallback getCallback() {
            MediaLoaderCallback callback = (MediaLoaderCallback) getObject();
            if (isAbort()) {
                return null;
            }
            return callback;
        }

        @Override
        public void run() {
            final MediaBucket allMediaInfoBucket = new MediaBucket();
            allMediaInfoBucket.allMediaInfo = true;

            final List<MediaBucket> allBuckets = new ArrayList<>();
            allBuckets.add(allMediaInfoBucket);

            final Map<Uri, MediaInfo> allMediaInfoMap = new HashMap<>();

            Cursor cursor = null;
            try {
                AbortUtil.throwIfAbort(this);

                Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver contentResolver = ContextUtil.getContext().getContentResolver();
                cursor = contentResolver.query(uri, allColumns(), null, null, null);
                Preconditions.checkNotNull(cursor);

                for (cursor.moveToLast(); !cursor.isBeforeFirst(); cursor.moveToPrevious()) {
                    AbortUtil.throwIfAbort(this);

                    MediaInfo itemMediaInfo = cursorToMediaInfo(cursor);
                    if (itemMediaInfo == null) {
                        continue;
                    }

                    if (!mMediaSelector.accept(itemMediaInfo)) {
                        continue;
                    }

                    if (allMediaInfoBucket.cover == null) {
                        allMediaInfoBucket.cover = itemMediaInfo;
                    }
                    allMediaInfoBucket.mediaInfoList.add(itemMediaInfo);
                    allMediaInfoMap.put(itemMediaInfo.uri, itemMediaInfo);

                    MediaBucket newBucket = createMediaBucket(itemMediaInfo);
                    MediaBucket oldBucket = queryOldMediaBucket(allBuckets, newBucket);
                    MediaBucket targetBucket;
                    if (oldBucket != null) {
                        // update old bucket
                        oldBucket.mediaInfoList.add(itemMediaInfo);
                        targetBucket = oldBucket;
                    } else {
                        // add new bucket;
                        allBuckets.add(newBucket);
                        targetBucket = newBucket;
                    }
                    itemMediaInfo.mMediaBucket = targetBucket;
                }

                AbortUtil.throwIfAbort(this);
                MediaLoaderCallback callback = getCallback();
                if (callback != null) {
                    callback.onLoadFinish(new MediaData(allMediaInfoBucket, allBuckets, allMediaInfoMap, mMediaSelector));
                }
            } catch (Throwable e) {
                SampleLog.e(e);
                MediaLoaderCallback callback = getCallback();
                if (callback != null) {
                    callback.onLoadFinish(new MediaData(allMediaInfoBucket, allBuckets, allMediaInfoMap, mMediaSelector));
                }
            } finally {
                IOUtil.closeQuietly(cursor);
            }
        }

        @Nullable
        private MediaBucket queryOldMediaBucket(List<MediaBucket> allSubBuckets, MediaBucket query) {
            for (MediaBucket oldBucket : allSubBuckets) {
                if (ObjectsCompat.equals(oldBucket, query)) {
                    return oldBucket;
                }
            }
            return null;
        }

        @NonNull
        private MediaBucket createMediaBucket(MediaInfo mediaInfo) {
            MediaBucket target = new MediaBucket();
            target.cover = mediaInfo;
            target.mediaInfoList.add(mediaInfo);
            target.bucketId = mediaInfo.bucketId;
            target.bucketDisplayName = mediaInfo.bucketDisplayName;
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
        private MediaInfo cursorToMediaInfo(Cursor cursor) {
            MediaInfo target = new MediaInfo();
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

            IMLog.v("cursorToMediaInfo USE_CONTENT_URI:%s -> %s", USE_CONTENT_URI, target);

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
