package com.masonsoft.imsdk;

import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.user.UserInfo;

public class MSIMUserInfo {

    @Nullable
    private UserInfo mUserInfo;

    void setUserInfo(@Nullable UserInfo userInfo) {
        mUserInfo = userInfo;
    }

    public long getUserId() {
        return getUserId(0L);
    }

    public long getUserId(long defaultValue) {
        if (mUserInfo == null) {
            return defaultValue;
        }
        return mUserInfo.uid.getOrDefault(defaultValue);
    }

    public String getNickname() {
        return getNickname(null);
    }

    public String getNickname(String defaultValue) {
        if (mUserInfo == null) {
            return defaultValue;
        }
        return mUserInfo.nickname.getOrDefault(defaultValue);
    }

    public String getAvatar() {
        return getAvatar(null);
    }

    public String getAvatar(String defaultValue) {
        if (mUserInfo == null) {
            return defaultValue;
        }
        return mUserInfo.avatar.getOrDefault(defaultValue);
    }

    public boolean hasGold() {
        return hasGold(false);
    }

    public boolean hasGold(boolean defaultValue) {
        if (mUserInfo == null) {
            return defaultValue;
        }
        if (mUserInfo.gold.isUnset()) {
            return defaultValue;
        }
        final Integer gold = mUserInfo.gold.get();
        if (gold == null) {
            return defaultValue;
        }
        return gold == IMConstants.TRUE;
    }

    public boolean hasVerified() {
        return hasVerified(false);
    }

    public boolean hasVerified(boolean defaultValue) {
        if (mUserInfo == null) {
            return defaultValue;
        }
        if (mUserInfo.gold.isUnset()) {
            return defaultValue;
        }
        final Integer verified = mUserInfo.verified.get();
        if (verified == null) {
            return defaultValue;
        }
        return verified == IMConstants.TRUE;
    }

}
