package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.masonsoft.imsdk.user.UserInfo;

public class UserCacheName extends UserCacheDynamicTextView {

    public UserCacheName(Context context) {
        this(context, null);
    }

    public UserCacheName(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UserCacheName(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onUserCacheUpdate(@Nullable UserInfo userInfo) {
        if (userInfo == null) {
            setText(null);
        } else {
            setText(userInfo.nickname.getOrDefault(null));
        }
    }

}
