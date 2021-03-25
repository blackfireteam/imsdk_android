package com.masonsoft.imsdk.sample.util;

import androidx.annotation.StringRes;

import com.idonans.core.util.ContextUtil;
import com.idonans.core.util.ToastUtil;
import com.masonsoft.imsdk.sample.R;

public class TipUtil {

    public static void show(String message) {
        ToastUtil.show(message);
    }

    public static void show(@StringRes int resId) {
        show(ContextUtil.getContext().getString(resId));
    }

    public static void show(@StringRes int resId, Object... formatArgs) {
        show(ContextUtil.getContext().getString(resId, formatArgs));
    }

    public static void showNetworkError() {
        show(R.string.imsdk_sample_tip_text_network_error);
    }

}
