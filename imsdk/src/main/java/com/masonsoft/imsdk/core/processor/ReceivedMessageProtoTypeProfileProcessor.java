package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.user.UserInfo;
import com.masonsoft.imsdk.user.UserInfoCacheManager;
import com.masonsoft.imsdk.user.UserInfoFactory;

/**
 * 收到一个用户信息
 *
 * @since 1.0
 */
public class ReceivedMessageProtoTypeProfileProcessor extends ReceivedMessageProtoTypeProcessor<ProtoMessage.Profile> {

    public ReceivedMessageProtoTypeProfileProcessor() {
        super(ProtoMessage.Profile.class);
    }

    @Override
    protected boolean doNotNullProtoMessageObjectProcess(
            @NonNull SessionProtoByteMessageWrapper target,
            @NonNull ProtoMessage.Profile protoMessageObject) {
        final UserInfo userInfo = UserInfoFactory.create(protoMessageObject);
        UserInfoCacheManager.getInstance().insertOrUpdateUser(userInfo);
        return true;
    }

}
