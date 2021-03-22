package com.masonsoft.imsdk.sample;

import android.app.Application;
import android.util.Log;

import com.masonsoft.imsdk.core.IMLog;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        IMLog.setLogLevel(Log.VERBOSE);
        SampleLog.setLogLevel(Log.VERBOSE);
    }

}
