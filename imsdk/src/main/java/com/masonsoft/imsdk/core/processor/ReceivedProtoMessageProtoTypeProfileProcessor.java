package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.user.UserInfo;
import com.masonsoft.imsdk.user.UserInfoManager;
import com.masonsoft.imsdk.user.UserInfoFactory;

/**
 * 收到一个用户信息
 *
 * @since 1.0
 */
public class ReceivedProtoMessageProtoTypeProfileProcessor extends ReceivedProtoMessageProtoTypeProcessor<ProtoMessage.Profile> {

    public ReceivedProtoMessageProtoTypeProfileProcessor() {
        super(ProtoMessage.Profile.class);
    }

    @Override
    protected boolean doNotNullProtoMessageObjectProcess(
            @NonNull SessionProtoByteMessageWrapper target,
            @NonNull ProtoMessage.Profile protoMessageObject) {
        final UserInfo userInfo = UserInfoFactory.create(protoMessageObject);
        UserInfoManager.getInstance().insertOrUpdateUser(userInfo);
        return true;
    }

}
