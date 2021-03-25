package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;

public class SessionUserCacheNameText extends UserCacheName {

    @SuppressWarnings("FieldCanBeLocal")
    private SessionUserIdChangedViewHelper mSessionUserIdChangedViewHelper;

    public SessionUserCacheNameText(Context context) {
        this(context, null);
    }

    public SessionUserCacheNameText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SessionUserCacheNameText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mSessionUserIdChangedViewHelper = new SessionUserIdChangedViewHelper() {
            @Override
            protected void onSessionUserIdChanged(long sessionUserId) {
                SessionUserCacheNameText.this.setTargetUserId(sessionUserId);
            }
        };
        setTargetUserId(mSessionUserIdChangedViewHelper.getSessionUserId());
    }

}
