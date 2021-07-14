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
import com.masonsoft.imsdk.sample.im.DiscoverUserManager;
import com.masonsoft.imsdk.uikit.IMUIKitComponentManager;
import com.masonsoft.imsdk.uikit.IMUIKitLog;
import com.masonsoft.imsdk.uikit.app.chat.SingleChatActivity;
import com.masonsoft.imsdk.uikit.common.TopActivity;
import com.masonsoft.imsdk.uikit.util.OkHttpClientUtil;

import io.github.idonans.core.manager.ProcessManager;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        final boolean debug = BuildConfig.DEBUG;
        if (debug) {
            IMLog.setLogLevel(Log.VERBOSE);
            IMUIKitLog.setLogLevel(Log.VERBOSE);
        }
        LocalSettingsManager.getInstance().start();
        DiscoverUserManager.getInstance().start();
        if (debug) {
            DebugManager.getInstance().start();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WebView.setDataDirectorySuffix(ProcessManager.getInstance().getProcessTag());
        }

        // 设置文件上服务
        FileUploadManager.getInstance().setFileUploadProvider(new TencentOSSFileUploadProvider());

        if (debug) {
            SampleLog.setLogLevel(Log.VERBOSE);
        }

        EmojiCompat.init(new BundledEmojiCompatConfig(this));

        // 初始化 im
        MSIMManager.getInstance().initSdk(Constants.SUB_APP, IMTokenOfflineManager.getInstance().getSdkListener());
        // 设置 im ui kit 组件跳转事件
        IMUIKitComponentManager.getInstance().setOnConversationViewClickListener(
                (activity, sessionUserId, conversationId, targetUserId) ->
                        SingleChatActivity.start(activity, targetUserId)
        );

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
