package com.masonsoft.imsdk.lang;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @since 1.0
 */
public class ActivityLifecycleCallbacksAdapter implements Application.ActivityLifecycleCallbacks {
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        // ignore
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        // ignore
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        // ignore
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        // ignore
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        // ignore
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        // ignore
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        // ignore
    }
}
