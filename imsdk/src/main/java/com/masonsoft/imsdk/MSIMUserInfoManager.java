package com.masonsoft.imsdk;

import androidx.annotation.Nullable;

import com.masonsoft.imsdk.user.UserInfo;
import com.masonsoft.imsdk.user.UserInfoManager;

import io.github.idonans.core.Singleton;

/**
 * @since 1.0
 */
public class MSIMUserInfoManager {

    private static final Singleton<MSIMUserInfoManager> INSTANCE = new Singleton<MSIMUserInfoManager>() {
        @Override
        protected MSIMUserInfoManager create() {
            return new MSIMUserInfoManager();
        }
    };

    static MSIMUserInfoManager getInstance() {
        return INSTANCE.get();
    }

    private MSIMUserInfoManager() {
    }

    @Nullable
    public MSIMUserInfo getUserInfo(long userId) {
        final UserInfo userInfo = UserInfoManager.getInstance().getByUserId(userId);
        if (userInfo == null) {
            return null;
        }
        return new MSIMUserInfo(userInfo);
    }

    public void insertOrUpdateUserInfo(@Nullable MSIMUserInfo.Editor editor) {
        if (editor == null) {
            return;
        }
        final UserInfo userInfo = editor.getUserInfoUpdate();
        UserInfoManager.getInstance().updateManual(
                userInfo.uid.get(),
                userInfo
        );
    }

}
