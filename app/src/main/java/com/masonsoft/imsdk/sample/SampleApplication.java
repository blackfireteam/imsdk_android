package com.masonsoft.imsdk.sample;

import android.app.Application;
import android.util.Log;

import androidx.emoji.bundled.BundledEmojiCompatConfig;
import androidx.emoji.text.EmojiCompat;

import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.common.logging.FLogDefaultLoggingDelegate;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.backends.okhttp3.OkHttpNetworkFetcher;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.idonans.core.manager.ProcessManager;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.sample.common.TopActivity;
import com.masonsoft.imsdk.sample.util.OkHttpClientUtil;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        IMLog.setLogLevel(Log.VERBOSE);
        SampleLog.setLogLevel(Log.VERBOSE);

        EmojiCompat.init(new BundledEmojiCompatConfig(this));

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
