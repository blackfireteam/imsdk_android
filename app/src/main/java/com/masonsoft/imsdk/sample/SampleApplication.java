package com.masonsoft.imsdk.sample;

import android.app.Application;
import android.os.Build;
import android.util.Log;
import android.webkit.WebView;

import androidx.emoji.bundled.BundledEmojiCompatConfig;
import androidx.emoji.text.EmojiCompat;

import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.common.logging.FLogDefaultLoggingDelegate;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.backends.okhttp3.OkHttpNetworkFetcher;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.masonsoft.imsdk.MSIMManager;
import com.masonsoft.imsdk.core.DebugManager;
import com.masonsoft.imsdk.core.FileUploadManager;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.sample.common.TopActivity;
import com.masonsoft.imsdk.sample.im.DiscoverUserManager;
import com.masonsoft.imsdk.sample.util.OkHttpClientUtil;

import io.github.idonans.core.manager.ProcessManager;
import io.github.idonans.dynamic.DynamicLog;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        IMLog.setLogLevel(Log.VERBOSE);
        LocalSettingsManager.getInstance().start();
        DiscoverUserManager.getInstance().start();
        DebugManager.getInstance().start();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WebView.setDataDirectorySuffix(ProcessManager.getInstance().getProcessTag());
        }

        // 设置文件上服务
        FileUploadManager.getInstance().setFileUploadProvider(new TencentOSSFileUploadProvider());

        SampleLog.setLogLevel(Log.VERBOSE);
        DynamicLog.setLogLevel(Log.VERBOSE);

        EmojiCompat.init(new BundledEmojiCompatConfig(this));

        // 初始化 im
        MSIMManager.getInstance().initSdk("appId", IMTokenOfflineManager.getInstance().getSdkListener());

        initFresco();
        registerActivityLifecycleCallbacks(TopActivity.getInstance().getActivityLifecycleCallbacks());
    }

    private void initFresco() {
        if (IMLog.getLogLevel() <= Log.DEBUG) {
            FLogDefaultLoggingDelegate.getInstance().setMinimumLoggingLevel(Log.DEBUG);
        }
        Fresco.initialize(this, ImagePipelineConfig.newBuilder(this)
                .setDownsampleEnabled(true)
                .setNetworkFetcher(new OkHttpNetworkFetcher(OkHttpClientUtil.createDefaultOkHttpClient()))
                .setMainDiskCacheConfig(DiskCacheConfig.newBuilder(this)
                        .setBaseDirectoryName(ProcessManager.getInstance().getProcessTag() + "_fresco_main")
                        .build())
                .setSmallImageDiskCacheConfig(DiskCacheConfig.newBuilder(this)
                        .setBaseDirectoryName(ProcessManager.getInstance().getProcessTag() + "_fresco_small")
                        .build())
                .build());
    }

}
