package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.user.UserInfo;

import io.github.idonans.lang.util.ViewUtil;

public class UserCacheGoldFlagView extends UserCacheDynamicImageView {

    public UserCacheGoldFlagView(Context context) {
        this(context, null);
    }

    public UserCacheGoldFlagView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UserCacheGoldFlagView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    public UserCacheGoldFlagView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        setImageResource(R.drawable.imsdk_sample_ic_profile_gold);
        setScaleType(ScaleType.CENTER_INSIDE);
    }

    @Override
    protected void onUserCacheUpdate(@Nullable UserInfo userInfo) {
        boolean gold = false;
        if (userInfo != null) {
            if (!userInfo.gold.isUnset()) {
                gold = userInfo.gold.get() == IMConstants.TRUE;
            }
        }

        ViewUtil.setVisibilityIfChanged(this, gold ? View.VISIBLE : View.GONE);
    }

}
