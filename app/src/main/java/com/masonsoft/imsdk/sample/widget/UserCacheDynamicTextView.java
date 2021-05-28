package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.masonsoft.imsdk.MSIMUserInfo;

public abstract class UserCacheDynamicTextView extends AppCompatTextView {

    protected final boolean DEBUG = true;
    private UserCacheChangedViewHelper mUserCacheChangedViewHelper;

    public UserCacheDynamicTextView(Context context) {
        this(context, null);
    }

    public UserCacheDynamicTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UserCacheDynamicTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public UserCacheDynamicTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mUserCacheChangedViewHelper = new UserCacheChangedViewHelper() {
            @Override
            protected void onUserCacheChanged(@Nullable MSIMUserInfo userInfo) {
                UserCacheDynamicTextView.this.onUserCacheUpdate(userInfo);
            }
        };
    }

    public void setTargetUserId(long targetUserId) {
        mUserCacheChangedViewHelper.setTargetUserId(targetUserId);
    }

    public long getTargetUserId() {
        return mUserCacheChangedViewHelper.getTargetUserId();
    }

    public void setExternalTargetUser(@Nullable MSIMUserInfo userInfo) {
        onUserCacheUpdate(userInfo);
    }

    protected abstract void onUserCacheUpdate(@Nullable MSIMUserInfo userInfo);

}
