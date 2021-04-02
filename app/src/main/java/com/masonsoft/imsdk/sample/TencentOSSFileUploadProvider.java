package com.masonsoft.imsdk.sample;

import androidx.annotation.NonNull;

import com.idonans.core.Progress;
import com.idonans.core.util.ContextUtil;
import com.idonans.core.util.FileUtil;
import com.masonsoft.imsdk.core.FileUploadProvider;
import com.masonsoft.imsdk.sample.util.FilenameUtil;
import com.masonsoft.imsdk.util.Preconditions;
import com.tencent.cos.xml.CosXmlService;
import com.tencent.cos.xml.CosXmlServiceConfig;
import com.tencent.cos.xml.model.object.PutObjectRequest;
import com.tencent.cos.xml.model.object.PutObjectResult;
import com.tencent.cos.xml.transfer.TransferConfig;
import com.tencent.cos.xml.transfer.TransferManager;
import com.tencent.qcloud.core.auth.QCloudCredentialProvider;
import com.tencent.qcloud.core.auth.ShortTimeCredentialProvider;

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
    public String uploadFile(@NonNull String filePath, @NonNull Progress progress) throws Throwable {
        final String fileExtension = FileUtil.getFileExtensionFromUrl(filePath);

        final QCloudCredentialProvider credentialProvider =
                new ShortTimeCredentialProvider(SECRET_ID, SECRET_KEY, 300);
        final CosXmlServiceConfig serviceConfig = new CosXmlServiceConfig.Builder()
                .setRegion(REGION_NAME)
                .isHttps(true) // 使用 HTTPS 请求, 默认为 HTTP 请求
                .builder();
        final CosXmlService cosXmlService =
                new CosXmlService(ContextUtil.getContext(), serviceConfig, credentialProvider);

        final TransferConfig transferConfig = new TransferConfig.Builder().build();
        final TransferManager transferManager =
                new TransferManager(cosXmlService, transferConfig);
        final String bucket = "msim-1252460681";
        final String cosPath = "im_image/Android_" + FilenameUtil.createUnionFilename(fileExtension);
        //noinspection UnnecessaryLocalVariable
        final String srcPath = filePath;

        final PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, cosPath, srcPath);
        putObjectRequest.setProgressListener((complete, target) -> progress.set(target, complete));
        final PutObjectResult putObjectResult = cosXmlService.putObject(putObjectRequest);

        final String accessUrl = putObjectResult.accessUrl;
        Preconditions.checkNotNull(accessUrl);
        return accessUrl;
    }

}
