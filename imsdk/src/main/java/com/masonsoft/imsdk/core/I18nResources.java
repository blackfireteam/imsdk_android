package com.masonsoft.imsdk.core;

import androidx.annotation.StringRes;

import io.github.idonans.core.util.ContextUtil;

public final class I18nResources {

    public static String getString(@StringRes int resId) {
        return ContextUtil.getContext().getString(resId);
    }

    public static String getString(@StringRes int resId, Object... formatArgs) {
        return ContextUtil.getContext().getString(resId, formatArgs);
    }

}
