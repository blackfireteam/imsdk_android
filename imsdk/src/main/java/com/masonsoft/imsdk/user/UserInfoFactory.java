package com.masonsoft.imsdk.user;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMConstants;
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
        target.gold.set(input.getGold() ? IMConstants.TRUE : IMConstants.FALSE);
        target.verified.set(input.getVerified() ? IMConstants.TRUE : IMConstants.FALSE);
        return target;
    }

}
