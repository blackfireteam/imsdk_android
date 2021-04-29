package com.masonsoft.imsdk.sample;

import android.app.Application;
import android.os.Build;
import android.os.Looper;
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
import com.masonsoft.imsdk.sample.common.TopActivity;
import com.masonsoft.imsdk.sample.im.DiscoverUserManager;
import com.masonsoft.imsdk.sample.util.OkHttpClientUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import io.github.idonans.core.manager.ProcessManager;
import io.github.idonans.core.thread.Threads;
import io.github.idonans.core.util.FileUtil;
import io.github.idonans.dynamic.DynamicLog;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        IMLog.setLogLevel(Log.VERBOSE);
        IMKickedManager.getInstance().start();
        LocalSettingsManager.getInstance().start();
        DiscoverUserManager.getInstance().start();
        IMManager.getInstance().start();
        DebugManager.getInstance().start();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WebView.setDataDirectorySuffix(ProcessManager.getInstance().getProcessTag());
        }

        // 设置文件上服务
        FileUploadManager.getInstance().setFileUploadProvider(new TencentOSSFileUploadProvider());

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
                    final long mAnrTimeout = TimeUnit.MILLISECONDS.toNanos(IMLog.getLogLevel() <= Log.VERBOSE ? 50 : 1500);
                    if (dur > mAnrTimeout) {
                        printAnrStack(dur);
                    }
                }
            }
        };
        Choreographer.getInstance().postFrameCallback(callback);
    }

    private void printAnrStack(final long dur) {
        try {
            final Thread mainThread = Looper.getMainLooper().getThread();
            final Map<Thread, StackTraceElement[]> stackTraces = new TreeMap<>((lhs, rhs) -> {
                if (lhs == rhs)
                    return 0;
                if (lhs == mainThread)
                    return 1;
                if (rhs == mainThread)
                    return -1;
                return rhs.getName().compareTo(lhs.getName());
            });
            for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
                stackTraces.put(entry.getKey(), entry.getValue());
            }
            printAnrStackToFileAsync(dur, stackTraces);
        } catch (Throwable e) {
            SampleLog.e(e);
        }
    }

    private void printAnrStackToFileAsync(final long dur, final Map<Thread, StackTraceElement[]> stackTraces) {
        Threads.postBackground(() -> {
            final File cacheDir = FileUtil.getAppCacheDir();
            if (cacheDir == null) {
                SampleLog.e("printAnrStackToFileAsync cache dir is null");
                return;
            }
            final long durMs = TimeUnit.NANOSECONDS.toMillis(dur);
            final String filename = "anr_" + new SimpleDateFormat("yyyyMMdd_HH_mm_ss", Locale.CHINA).format(new Date()) + "_" + durMs + "ms";
            final String anrFile = FileUtil.createSimilarFileQuietly(new File(cacheDir, filename).getAbsolutePath());
            if (anrFile == null) {
                SampleLog.e("printAnrStackToFileAsync anrFile is null");
                return;
            }
            try (PrintWriter writer = new PrintWriter(new FileWriter(new File(anrFile)))) {
                writer.println("--------------------------------------------------------------");
                writer.println("--------------------------------------------------------------");
                writer.println("---------------- anr " + durMs + " ms ------------------------");
                writer.println("--------------------------------------------------------------");
                writer.println("--------------------------------------------------------------");
                for (Map.Entry<Thread, StackTraceElement[]> entry : stackTraces.entrySet()) {
                    writer.println("----------------------------");
                    writer.println(entry.getKey());
                    writer.println("-------");
                    for (StackTraceElement stackTraceElement : entry.getValue()) {
                        writer.println(stackTraceElement);
                    }
                    writer.println("----------------------------");
                }
                writer.println("--------------------------------------------------------------");
                writer.println("--------------------------------------------------------------");
                writer.println("--------------------------------------------------------------");
                writer.println("--------------------------------------------------------------");
                writer.flush();
            } catch (Throwable e) {
                SampleLog.e(e);
            }
        });
    }

}
