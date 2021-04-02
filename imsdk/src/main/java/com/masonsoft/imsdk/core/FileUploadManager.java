package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

import com.idonans.core.Progress;
import com.idonans.core.Singleton;
import com.idonans.core.util.ContextUtil;
import com.idonans.core.util.FileUtil;
import com.masonsoft.imsdk.lang.ImageInfo;
import com.masonsoft.imsdk.util.BitmapUtil;
import com.masonsoft.imsdk.util.Objects;

import java.io.File;

import top.zibin.luban.Luban;

/**
 * 文件上传管理
 *
 * @since 1.0
 */
public class FileUploadManager {

    private static final Singleton<FileUploadManager> INSTANCE = new Singleton<FileUploadManager>() {
        @Override
        protected FileUploadManager create() {
            return new FileUploadManager();
        }
    };

    public static FileUploadManager getInstance() {
        return INSTANCE.get();
    }

    private static class MemoryFullCache {

        private static final MemoryFullCache DEFAULT = new MemoryFullCache();

        private static final int MEMORY_CACHE_SIZE = 100;
        @NonNull
        private final LruCache<String, String> mFullCaches = new LruCache<>(MEMORY_CACHE_SIZE);

        private void addFullCache(@NonNull String filePath, @NonNull String accessUrl) {
            mFullCaches.put(filePath, accessUrl);
        }

        private void removeFullCache(@NonNull String filePath) {
            mFullCaches.remove(filePath);
        }

        @Nullable
        private String getFullCache(@NonNull String filePath) {
            return mFullCaches.get(filePath);
        }
    }

    @NonNull
    private final FileUploadProviderProxy mProviderProxy = new FileUploadProviderProxy();

    private FileUploadManager() {
    }

    public void setFileUploadProvider(@Nullable FileUploadProvider provider) {
        mProviderProxy.setProvider(provider);
    }

    @NonNull
    protected FileUploadProvider getFileUploadProvider() {
        return mProviderProxy;
    }

    private static class FileUploadProviderProxy implements FileUploadProvider {

        @Nullable
        private FileUploadProvider mProvider;

        public void setProvider(@Nullable FileUploadProvider provider) {
            mProvider = provider;
        }

        @NonNull
        @Override
        public String uploadFile(@NonNull String filePath, @NonNull Progress progress) throws Throwable {
            final String cache = MemoryFullCache.DEFAULT.getFullCache(filePath);
            if (cache != null) {
                IMLog.v(Objects.defaultObjectTag(this) + " uploadFile cache hit. %s -> %s",
                        filePath, cache);
                return cache;
            }

            final String compressFilePath = compressImage(filePath);
            IMLog.v(Objects.defaultObjectTag(this) + " compress image %s -> %s", filePath, compressFilePath);

            final FileUploadProvider provider = mProvider;
            if (provider != null) {
                final String accessUrl = provider.uploadFile(compressFilePath, progress);
                MemoryFullCache.DEFAULT.addFullCache(filePath, accessUrl);
                return accessUrl;
            }
            throw new IllegalAccessError("provider not found");
        }

        /**
         * 压缩图片
         *
         * @param filePath 图片的本地路径
         * @return 如果成功压缩图片，返回压缩后的图片路径。如果不是图片格式，返回原始路径。
         * @throws Throwable
         */
        @NonNull
        private String compressImage(@NonNull String filePath) throws Throwable {
            final File file = new File(filePath);
            if (!FileUtil.isFile(file)) {
                throw new IllegalAccessError(filePath + " is not a exists file");
            }

            final ImageInfo imageInfo = BitmapUtil.decodeImageInfo(filePath);
            if (imageInfo == null) {
                // not a image
                return filePath;
            }

            if (imageInfo.isGif()) {
                // gif 图不压缩
                return filePath;
            }

            // 压缩图片
            final File compressedFile = Luban.with(ContextUtil.getContext())
                    .get(filePath);
            return compressedFile.getAbsolutePath();
        }

    }

}
