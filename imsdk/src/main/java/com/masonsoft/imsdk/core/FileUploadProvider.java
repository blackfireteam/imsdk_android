package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import io.github.idonans.core.Progress;

/**
 * 文件上传服务，实现将文件内容上传至服务器并生成可访问网络地址
 *
 * @since 1.0
 */
public interface FileUploadProvider {

    /**
     * @param filePath 待上传的文件地址
     * @param mimeType 待上传的文件 mime 类型, 可能为空
     * @param progress 上传进度回调
     * @return 上传成功后的可访问网络地址.
     * @throws Throwable 上传失败时，抛出异常。如文件不存在，格式不支持，或者网络错误等。
     */
    @WorkerThread
    @NonNull
    String uploadFile(@NonNull final String filePath, @Nullable final String mimeType, @NonNull final Progress progress) throws Throwable;

}
