package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.user.UserInfo;

public class MSIMUserInfo {

    @NonNull
    private final UserInfo mUserInfo;

    MSIMUserInfo(@NonNull UserInfo userInfo) {
        mUserInfo = userInfo;
    }

    @NonNull
    UserInfo getUserInfo() {
        return mUserInfo;
    }

    public long getUserId() {
        return getUserId(0L);
    }

    public long getUserId(long defaultValue) {
        return mUserInfo.uid.getOrDefault(defaultValue);
    }

    public String getNickname() {
        return getNickname(null);
    }

    public String getNickname(String defaultValue) {
        return mUserInfo.nickname.getOrDefault(defaultValue);
    }

    public String getAvatar() {
        return getAvatar(null);
    }

    public String getAvatar(String defaultValue) {
        return mUserInfo.avatar.getOrDefault(defaultValue);
    }

    public boolean isGold() {
        return isGold(false);
    }

    public boolean isGold(boolean defaultValue) {
        if (mUserInfo.gold.isUnset()) {
            return defaultValue;
        }
        final Integer gold = mUserInfo.gold.get();
        if (gold == null) {
            return defaultValue;
        }
        return gold == IMConstants.TRUE;
    }

    public boolean isVerified() {
        return isVerified(false);
    }

    public boolean isVerified(boolean defaultValue) {
        if (mUserInfo.verified.isUnset()) {
            return defaultValue;
        }
        final Integer verified = mUserInfo.verified.get();
        if (verified == null) {
            return defaultValue;
        }
        return verified == IMConstants.TRUE;
    }

}
