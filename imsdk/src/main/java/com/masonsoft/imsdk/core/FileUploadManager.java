package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.core.Progress;
import com.idonans.core.Singleton;

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
            final FileUploadProvider provider = mProvider;
            if (provider != null) {
                return provider.uploadFile(filePath, progress);
            }
            throw new IllegalAccessError("provider not found");
        }

    }

}
