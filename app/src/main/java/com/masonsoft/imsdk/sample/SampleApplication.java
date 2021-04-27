package com.masonsoft.imsdk.sample;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.util.Log;
import android.view.Choreographer;
import android.webkit.WebView;

import androidx.emoji.bundled.BundledEmojiCompatConfig;
import androidx.emoji.text.EmojiCompat;

import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.common.logging.FLogDefaultLoggingDelegate;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.backends.okhttp3.OkHttpNetworkFetcher;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.masonsoft.imsdk.core.DebugManager;
import com.masonsoft.imsdk.core.FileUploadManager;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMManager;
import com.masonsoft.imsdk.core.observable.KickedObservable;
import com.masonsoft.imsdk.sample.app.main.MainActivity;
import com.masonsoft.imsdk.sample.common.TopActivity;
import com.masonsoft.imsdk.sample.im.DiscoverUserManager;
import com.masonsoft.imsdk.sample.util.OkHttpClientUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.github.idonans.core.manager.ProcessManager;
import io.github.idonans.core.thread.Threads;
import io.github.idonans.core.util.FileUtil;
import io.github.idonans.dynamic.DynamicLog;

public class SampleApplication extends Application {

    private final KickedObservable.KickedObserver mKickedObserver = (session, sessionUserId) -> {
        final Activity topActivity = TopActivity.getInstance().get();
        if (topActivity == null) {
            return;
        }
        MainActivity.start(topActivity, true);
    };

    @Override
    public void onCreate() {
        super.onCreate();

        IMLog.setLogLevel(Log.VERBOSE);
        LocalSettingsManager.getInstance().attach();
        DiscoverUserManager.getInstance().attach();
        IMManager.getInstance().attach();
        DebugManager.getInstance().start();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WebView.setDataDirectorySuffix(ProcessManager.getInstance().getProcessTag());
        }

        // 设置文件上服务
        FileUploadManager.getInstance().setFileUploadProvider(new TencentOSSFileUploadProvider());

        // 设置踢下线监听
        KickedObservable.DEFAULT.registerObserver(mKickedObserver);

        SampleLog.setLogLevel(Log.VERBOSE);
        DynamicLog.setLogLevel(Log.VERBOSE);

        EmojiCompat.init(new BundledEmojiCompatConfig(this));

        initFresco();
        registerActivityLifecycleCallbacks(TopActivity.getInstance().getActivityLifecycleCallbacks());

        addAnrDebug();
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

    private final long mAnrTimeout = TimeUnit.SECONDS.toNanos(2);
    private long mLastFrameTimeNanos;

    private void addAnrDebug() {
        final Choreographer.FrameCallback callback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                Choreographer.getInstance().postFrameCallback(this);
                final long lastFrameTimeNanos = mLastFrameTimeNanos;
                mLastFrameTimeNanos = frameTimeNanos;
                if (lastFrameTimeNanos > 0) {
                    final long dur = frameTimeNanos - lastFrameTimeNanos;
                    if (dur > mAnrTimeout) {
                        printAnrStack(dur);
                    }
                }
            }
        };
        Choreographer.getInstance().postFrameCallback(callback);
    }

    private void printAnrStack(final long dur) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final PrintStream ps = new PrintStream(os);
        new RuntimeException("anr found dur:" + dur + "ns, " + TimeUnit.NANOSECONDS.toMillis(dur) + "ms").printStackTrace(ps);
        ps.flush();
        final byte[] stack = os.toByteArray();
        try {
            System.err.write(stack);
        } catch (IOException e) {
            SampleLog.e(e);
        }
        printAnrStackToFileAsync(stack);
    }

    private void printAnrStackToFileAsync(final byte[] stack) {
        Threads.postBackground(() -> {
            final File cacheDir = FileUtil.getAppCacheDir();
            if (cacheDir == null) {
                SampleLog.e("printAnrStackToFileAsync cache dir is null");
                return;
            }
            final String filename = "anr_" + new SimpleDateFormat("yyyyMMdd_HH_mm_ss", Locale.CHINA).format(new Date());
            final String anrFile = FileUtil.createSimilarFileQuietly(new File(cacheDir, filename).getAbsolutePath());
            if (anrFile == null) {
                SampleLog.e("printAnrStackToFileAsync anrFile is null");
                return;
            }
            try (FileOutputStream fos = new FileOutputStream(new File(anrFile))) {
                fos.write(stack);
                fos.flush();
            } catch (Throwable e) {
                SampleLog.e(e);
            }
        });
    }

}
