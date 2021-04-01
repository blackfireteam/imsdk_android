package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.user.UserInfo;
import com.masonsoft.imsdk.user.UserInfoManager;
import com.masonsoft.imsdk.user.UserInfoFactory;
import com.masonsoft.imsdk.util.Objects;

import java.util.List;

/**
 * 收到一组用户信息
 *
 * @since 1.0
 */
public class ReceivedMessageProtoTypeProfileListProcessor extends ReceivedMessageProtoTypeProcessor<ProtoMessage.ProfileList> {

    public ReceivedMessageProtoTypeProfileListProcessor() {
        super(ProtoMessage.ProfileList.class);
    }

    @Override
    protected boolean doNotNullProtoMessageObjectProcess(
            @NonNull SessionProtoByteMessageWrapper target,
            @NonNull ProtoMessage.ProfileList protoMessageObject) {

        final List<ProtoMessage.Profile> profileList = protoMessageObject.getProfilesList();
        if (profileList != null) {
            final int size = profileList.size();
            IMLog.v(Objects.defaultObjectTag(this) + " got %s profile", size);
            final long timeStart = System.currentTimeMillis();
            for (ProtoMessage.Profile profile : profileList) {
                if (profile == null) {
                    continue;
                }

                final UserInfo userInfo = UserInfoFactory.create(profile);
                UserInfoManager.getInstance().insertOrUpdateUser(userInfo);
            }
            final long timeInterval = System.currentTimeMillis() - timeStart;
            IMLog.v(Objects.defaultObjectTag(this) + " got %s profile, process use %s ms", size, timeInterval);
        }

        return true;
    }

}
