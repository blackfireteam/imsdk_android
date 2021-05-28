package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    public static class Editor {

        @NonNull
        private final UserInfo mUserInfoUpdate;

        public Editor(final long userId) {
            mUserInfoUpdate = new UserInfo();
            mUserInfoUpdate.uid.set(userId);
        }

        @NonNull
        public MSIMUserInfo getUserInfo() {
            return new MSIMUserInfo(mUserInfoUpdate);
        }

        @NonNull
        UserInfo getUserInfoUpdate() {
            return mUserInfoUpdate;
        }

        @NonNull
        public Editor setAvatar(@Nullable final String avatar) {
            mUserInfoUpdate.avatar.set(avatar);
            return this;
        }

        @NonNull
        public Editor setNickname(@Nullable final String nickname) {
            mUserInfoUpdate.nickname.set(nickname);
            return this;
        }

        @NonNull
        public Editor setGold(final boolean gold) {
            mUserInfoUpdate.gold.set(gold ? IMConstants.TRUE : IMConstants.FALSE);
            return this;
        }

        @NonNull
        public Editor setVerified(final boolean verified) {
            mUserInfoUpdate.verified.set(verified ? IMConstants.TRUE : IMConstants.FALSE);
            return this;
        }

        @NonNull
        public Editor setUpdateTimeMs(final long updateTimeMs) {
            mUserInfoUpdate.updateTimeMs.set(updateTimeMs);
            return this;
        }
    }

}
