package com.masonsoft.imsdk.sample.im;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMMessageQueueManager;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.processor.ReceivedProtoMessageNotNullProcessor;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.observable.DiscoverUserObservable;
import com.masonsoft.imsdk.user.UserInfo;
import com.masonsoft.imsdk.user.UserInfoManager;
import com.masonsoft.imsdk.util.Objects;

import java.util.ArrayList;
import java.util.List;

import io.github.idonans.core.Singleton;

public class DiscoverUserManager {

    private static final Singleton<DiscoverUserManager> INSTANCE = new Singleton<DiscoverUserManager>() {
        @Override
        protected DiscoverUserManager create() {
            return new DiscoverUserManager();
        }
    };

    public static DiscoverUserManager getInstance() {
        return INSTANCE.get();
    }

    @NonNull
    private final List<Long> mOnlineUserList = new ArrayList<>();

    private DiscoverUserManager() {
        IMMessageQueueManager.getInstance()
                .getReceivedMessageProcessor()
                .addFirstProcessor(new ProfileOnlineOfflineProcessor());
    }

    public void attach() {
        SampleLog.v(Objects.defaultObjectTag(this) + "attach");
    }

    @NonNull
    public List<Long> getOnlineUserList() {
        synchronized (mOnlineUserList) {
            return new ArrayList<>(mOnlineUserList);
        }
    }

    private void addOnline(Long userId) {
        synchronized (mOnlineUserList) {
            if (!mOnlineUserList.contains(userId)) {
                mOnlineUserList.add(userId);
            }
        }

        DiscoverUserObservable.DEFAULT.notifyDiscoverUserOnline(userId);
    }

    private void removeOnline(Long userId) {
        synchronized (mOnlineUserList) {
            mOnlineUserList.remove(userId);
        }

        DiscoverUserObservable.DEFAULT.notifyDiscoverUserOffline(userId);
    }

    private class ProfileOnlineOfflineProcessor extends ReceivedProtoMessageNotNullProcessor {

        @Override
        protected boolean doNotNullProcess(@NonNull SessionProtoByteMessageWrapper target) {
            final Object protoMessageObject = target.getProtoByteMessageWrapper().getProtoMessageObject();
            if (protoMessageObject == null) {
                return false;
            }

            if (protoMessageObject instanceof ProtoMessage.ProfileOnline) {
                final UserInfo userInfo = createUserInfo((ProtoMessage.ProfileOnline) protoMessageObject);
                UserInfoManager.getInstance().insertOrUpdateUser(userInfo);
                addOnline(userInfo.uid.get());
                return true;
            }

            if (protoMessageObject instanceof ProtoMessage.UsrOffline) {
                removeOnline(((ProtoMessage.UsrOffline) protoMessageObject).getUid());
                return true;
            }

            return false;
        }

    }

    @NonNull
    private static UserInfo createUserInfo(@NonNull ProtoMessage.ProfileOnline input) {
        final UserInfo target = new UserInfo();
        target.uid.set(input.getUid());

        // 将服务器返回的秒转换为毫秒
        target.updateTimeMs.set(input.getUpdateTime() * 1000L);

        target.nickname.set(input.getNickName());
        target.avatar.set(input.getAvatar());
        target.gold.set(input.getGold() ? IMConstants.TRUE : IMConstants.FALSE);
        target.verified.set(input.getVerified() ? IMConstants.TRUE : IMConstants.FALSE);
        return target;
    }

}
