package com.masonsoft.imsdk.sample.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.SampleLog;

public class ActivityUtil {

    private ActivityUtil() {
    }

    public static boolean requestBackPressed(Fragment fragment) {
        if (fragment != null) {
            final Activity activity = fragment.getActivity();
            if (activity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
                return false;
            }
            if (activity.isFinishing()) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_FINISHING);
                return false;
            }
            activity.onBackPressed();
            return true;
        } else {
            SampleLog.e(Constants.ErrorLog.FRAGMENT_IS_NULL);
            return false;
        }
    }

    @Nullable
    public static Activity getActivity(@Nullable Context context) {
        while (context != null) {
            if (context instanceof Activity) {
                return (Activity) context;
            }

            if (context instanceof ContextWrapper) {
                final Context baseContext = ((ContextWrapper) context).getBaseContext();
                if (baseContext == context) {
                    return null;
                }
                context = baseContext;
            } else {
                return null;
            }
        }
        return null;
    }

    @Nullable
    public static AppCompatActivity getActiveAppCompatActivity(@Nullable Context context) {
        final Activity activity = getActivity(context);
        if (activity == null) {
            SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
            return null;
        }
        if (!(activity instanceof AppCompatActivity)) {
            SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NOT_APP_COMPAT_ACTIVITY);
            return null;
        }

        final AppCompatActivity appCompatActivity = (AppCompatActivity) activity;
        if (appCompatActivity.isFinishing()) {
            SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_FINISHING);
            return null;
        }
        if (appCompatActivity.getSupportFragmentManager().isStateSaved()) {
            SampleLog.e(Constants.ErrorLog.FRAGMENT_MANAGER_STATE_SAVED);
            return null;
        }
        return appCompatActivity;
    }

}
