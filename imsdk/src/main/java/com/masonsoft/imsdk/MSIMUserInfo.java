package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    public int getGender() {
        return getGender(0);
    }

    public int getGender(int defaultValue) {
        return mUserInfo.gender.getOrDefault(defaultValue);
    }

    public String getCustom() {
        return getCustom(null);
    }

    public String getCustom(String defaultValue) {
        return mUserInfo.custom.getOrDefault(defaultValue);
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
        public Editor setGender(final int gender) {
            mUserInfoUpdate.gender.set(gender);
            return this;
        }

        @NonNull
        public Editor setCustom(final String custom) {
            mUserInfoUpdate.custom.set(custom);
            return this;
        }

        @NonNull
        public Editor setUpdateTimeMs(final long updateTimeMs) {
            mUserInfoUpdate.updateTimeMs.set(updateTimeMs);
            return this;
        }
    }

}
