package com.masonsoft.imsdk.sample.util;

import android.app.Activity;

import androidx.fragment.app.Fragment;

import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.SampleLog;

public class BackStackUtil {

    private BackStackUtil() {
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

}
