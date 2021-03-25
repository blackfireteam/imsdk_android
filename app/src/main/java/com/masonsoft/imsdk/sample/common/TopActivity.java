package com.masonsoft.imsdk.sample.common;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.core.Singleton;

import java.lang.ref.WeakReference;

public class TopActivity {

    private static final Singleton<TopActivity> INSTANCE = new Singleton<TopActivity>() {
        @Override
        protected TopActivity create() {
            return new TopActivity();
        }
    };

    public static TopActivity getInstance() {
        return INSTANCE.get();
    }

    @NonNull
    private WeakReference<Activity> mTopActivityRef = new WeakReference<>(null);

    private final Application.ActivityLifecycleCallbacks mActivityLifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            putActivity(activity);
        }

        @Override
        public void onActivityStarted(Activity activity) {
            putActivity(activity);
        }

        @Override
        public void onActivityResumed(Activity activity) {
            putActivity(activity);
        }

        @Override
        public void onActivityPaused(Activity activity) {
            // ignore
        }

        @Override
        public void onActivityStopped(Activity activity) {
            removeActivity(activity);
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            removeActivity(activity);
        }

        private void putActivity(Activity activity) {
            mTopActivityRef = new WeakReference<>(activity);
        }

        private void removeActivity(Activity activity) {
            if (mTopActivityRef.get() == activity) {
                mTopActivityRef = new WeakReference<>(null);
            }
        }

    };

    private TopActivity() {
    }

    public Application.ActivityLifecycleCallbacks getActivityLifecycleCallbacks() {
        return mActivityLifecycleCallbacks;
    }

    @Nullable
    public Activity get() {
        return mTopActivityRef.get();
    }

    @Nullable
    public <T> T get(Class<T> clazz) {
        Activity activity = get();
        if (clazz.isInstance(activity)) {
            return clazz.cast(activity);
        }
        return null;
    }

}
