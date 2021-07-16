package com.masonsoft.imsdk.sample;

import android.app.Application;
import android.util.Log;

import com.masonsoft.imsdk.MSIMManager;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.sample.im.DiscoverUserManager;
import com.masonsoft.imsdk.uikit.MSIMUikitLog;
import com.masonsoft.imsdk.uikit.MSIMUikitManager;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        final boolean debug = BuildConfig.DEBUG;
        if (debug) {
            IMLog.setLogLevel(Log.VERBOSE);
            MSIMUikitLog.setLogLevel(Log.VERBOSE);
            SampleLog.setLogLevel(Log.VERBOSE);
        }
        DiscoverUserManager.getInstance().start();

        // 初始化 im
        MSIMManager.getInstance().initSdk(Constants.SUB_APP, IMTokenOfflineManager.getInstance().getSdkListener());
        // 初始化 im ui kit
        MSIMUikitManager.getInstance().init();

        LocalSettingsManager.getInstance().start();
    }

}
