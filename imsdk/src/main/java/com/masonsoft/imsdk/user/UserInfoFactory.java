package com.masonsoft.imsdk.user;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.proto.ProtoMessage;

/**
 * @since 1.0
 */
public class UserInfoFactory {

    private UserInfoFactory() {
    }

    @NonNull
    public static UserInfo create(@NonNull ProtoMessage.Profile input) {
        final UserInfo target = new UserInfo();
        target.uid.set(input.getUid());

        // 将服务器返回的秒转换为毫秒
        target.updateTimeMs.set(input.getUpdateTime() * 1000);

        target.nickname.set(input.getNickName());
        target.avatar.set(input.getAvatar());
        target.gender.set((int) input.getGender());
        target.custom.set(input.getCustom());
        return target;
    }

    @NonNull
    public static UserInfo create(final long userId) {
        final UserInfo target = new UserInfo();
        target.uid.set(userId);

        // 设置一个较小的时间作为更新时间
        target.updateTimeMs.set(0L);

        return target;
    }

    @NonNull
    public static UserInfo copy(@NonNull UserInfo input) {
        final UserInfo target = new UserInfo();
        target.apply(input);
        return target;
    }

}
