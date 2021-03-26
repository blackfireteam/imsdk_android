package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.user.UserInfo;

public class UserCacheAvatar extends ImageLayout {

    private static final int AVATAR_SIZE_SMALL = 0;
    private static final int AVATAR_SIZE_MIDDLE = 1;
    private static final int AVATAR_SIZE_LARGE = 2;

    @IntDef({AVATAR_SIZE_SMALL, AVATAR_SIZE_MIDDLE, AVATAR_SIZE_LARGE})
    public @interface AvatarSize {
    }

    @AvatarSize
    private int mAvatarSize = AVATAR_SIZE_SMALL;
    private UserCacheChangedViewHelper mUserCacheChangedViewHelper;

    @Nullable
    private UserInfo mCacheUserInfo;

    public UserCacheAvatar(Context context) {
        this(context, null);
    }

    public UserCacheAvatar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UserCacheAvatar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, R.style.UserCacheAvatar);
    }

    public UserCacheAvatar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.UserCacheAvatar, defStyleAttr,
                defStyleRes);
        mAvatarSize = a.getInt(R.styleable.UserCacheAvatar_avatarSize, mAvatarSize);
        a.recycle();

        mUserCacheChangedViewHelper = new UserCacheChangedViewHelper() {
            @Override
            protected void onUserCacheChanged(@Nullable UserInfo userInfo) {
                mCacheUserInfo = userInfo;
                if (mCacheUserInfo == null) {
                    loadAvatar(null);
                } else {
                    loadAvatar(mCacheUserInfo.avatar.getOrDefault(null));
                }
                invalidate();
            }
        };
    }

    public void setTargetUserId(long targetUserId) {
        mUserCacheChangedViewHelper.setTargetUserId(targetUserId);
    }

    public long getTargetUserId() {
        return mUserCacheChangedViewHelper.getTargetUserId();
    }

    private void loadAvatar(String url) {
        /*
        // 约束图片地址的网络尺寸
        // TODO
        final int limitSize;
        if (mAvatarSize == AVATAR_SIZE_LARGE) {
            limitSize = Constants.AvatarSize.LARGE;
        } else if (mAvatarSize == AVATAR_SIZE_MIDDLE) {
            limitSize = Constants.AvatarSize.MIDDLE;
        } else {
            limitSize = Constants.AvatarSize.SMALL;
        }

        url = new OSSImageUrlBuilder(url)
                .scale(limitSize, limitSize)
                .formatStatic()
                .build();
         */

        setUrl(url);
    }

}