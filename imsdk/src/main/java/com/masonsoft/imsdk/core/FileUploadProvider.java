package com.masonsoft.imsdk.core;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.idonans.core.Progress;

/**
 * 文件上传服务，实现将文件内容上传至服务器并生成可访问网络地址
 *
 * @since 1.0
 */
public interface FileUploadProvider {

    @IntDef({SOURCE_CHAT, SOURCE_OTHER})
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.PARAMETER)
    @interface Source {
    }

    /**
     * 来源于聊天。如发送图片消息时需要上传的图片，发送视频消息时需要上传的视频。
     */
    int SOURCE_CHAT = 0;
    /**
     * 其它来源
     */
    int SOURCE_OTHER = 1;

    /**
     * @param filePath 待上传的文件地址
     * @param source   该上传需求的来源。可以根据不同来源来区分该上传的文件如何存储，如对于聊天中上传的文件，附加有效期。
     * @param mimeType 待上传的文件 mime 类型, 可能为空
     * @param progress 上传进度回调
     * @return 上传成功后的可访问网络地址.
     * @throws Throwable 上传失败时，抛出异常。如文件不存在，格式不支持，或者网络错误等。
     */
    @WorkerThread
    @NonNull
    String uploadFile(@NonNull final String filePath, @Source final int source, @Nullable final String mimeType, @NonNull final Progress progress) throws Throwable;

}
