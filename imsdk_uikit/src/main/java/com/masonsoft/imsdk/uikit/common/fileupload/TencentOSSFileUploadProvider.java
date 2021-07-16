package com.masonsoft.imsdk.uikit.common.fileupload;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.FileUploadProvider;
import com.masonsoft.imsdk.uikit.util.FilenameUtil;
import com.tencent.cos.xml.CosXmlService;
import com.tencent.cos.xml.CosXmlServiceConfig;
import com.tencent.cos.xml.model.object.PutObjectRequest;
import com.tencent.cos.xml.model.object.PutObjectResult;
import com.tencent.qcloud.core.auth.QCloudCredentialProvider;
import com.tencent.qcloud.core.auth.ShortTimeCredentialProvider;

import io.github.idonans.core.Progress;
import io.github.idonans.core.util.ContextUtil;
import io.github.idonans.core.util.FileUtil;
import io.github.idonans.core.util.Preconditions;

/**
 * 基于腾讯云对象存储实现的文件上传服务
 */
public class TencentOSSFileUploadProvider implements FileUploadProvider {

    private static final String SECRET_ID = "AKIDiARZwekKIK7f18alpjsqdOzmQAplexA5";
    private static final String SECRET_KEY = "f7MLJ3YnoX2KLKBmBeAVeWNVLaYEmGYa";
    private static final String REGION_NAME = "ap-chengdu";

    public TencentOSSFileUploadProvider() {
    }

    @NonNull
    @Override
    public String uploadFile(@NonNull String filePath, @Source int source, @Nullable String mimeType, @NonNull Progress progress) throws Throwable {
        final String fileExtension = FileUtil.getFileExtensionFromUrl(filePath);

        final QCloudCredentialProvider credentialProvider =
                new ShortTimeCredentialProvider(SECRET_ID, SECRET_KEY, 300);
        final CosXmlServiceConfig serviceConfig = new CosXmlServiceConfig.Builder()
                .setRegion(REGION_NAME)
                .isHttps(true) // 使用 HTTPS 请求, 默认为 HTTP 请求
                .builder();
        final CosXmlService cosXmlService =
                new CosXmlService(ContextUtil.getContext(), serviceConfig, credentialProvider);

        final String bucket = "msim-1252460681";
        final String dir = createDir(source, mimeType);
        final String cosPath = dir + "/Android_" + FilenameUtil.createUnionFilename(fileExtension, mimeType);
        //noinspection UnnecessaryLocalVariable
        final String srcPath = filePath;

        final PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, cosPath, srcPath);
        putObjectRequest.setProgressListener((complete, target) -> progress.set(target, complete));
        final PutObjectResult putObjectResult = cosXmlService.putObject(putObjectRequest);

        final String accessUrl = putObjectResult.accessUrl;
        Preconditions.checkNotNull(accessUrl);
        return accessUrl;
    }

    @NonNull
    private String createDir(@Source int source, @Nullable String mimeType) {
        if (source == SOURCE_OTHER) {
            return "common";
        } else {
            // SOURCE_CHAT
            String dir = "im_image";
            if (mimeType != null) {
                if (mimeType.startsWith("video/")) {
                    dir = "im_video";
                } else if (mimeType.startsWith("audio/")) {
                    dir = "im_voice";
                }
            }
            return dir;
        }
    }

}
