package com.masonsoft.imsdk.sample;

import android.app.Application;
import android.util.Log;

import androidx.emoji.bundled.BundledEmojiCompatConfig;
import androidx.emoji.text.EmojiCompat;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.sample.common.TopActivity;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        IMLog.setLogLevel(Log.VERBOSE);
        SampleLog.setLogLevel(Log.VERBOSE);

        EmojiCompat.init(new BundledEmojiCompatConfig(this));

        registerActivityLifecycleCallbacks(TopActivity.getInstance().getActivityLifecycleCallbacks());
    }

}
