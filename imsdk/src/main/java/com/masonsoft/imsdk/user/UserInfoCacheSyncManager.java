package com.masonsoft.imsdk.user;

import com.idonans.core.Singleton;
import com.idonans.core.thread.TaskQueue;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMProcessValidator;
import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.core.ProtoByteMessage;
import com.masonsoft.imsdk.core.message.packet.MessagePacket;
import com.masonsoft.imsdk.core.message.packet.ResultIgnoreMessagePacket;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.lang.SafetyRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户缓存信息的同步管理
 *
 * @since 1.0
 */
public class UserInfoCacheSyncManager {

    private static final Singleton<UserInfoCacheSyncManager> INSTANCE = new Singleton<UserInfoCacheSyncManager>() {
        @Override
        protected UserInfoCacheSyncManager create() {
            return new UserInfoCacheSyncManager();
        }
    };

    public static UserInfoCacheSyncManager getInstance() {
        IMProcessValidator.validateProcess();

        return INSTANCE.get();
    }

    private final TaskQueue mSyncQueue = new TaskQueue(1);

    private UserInfoCacheSyncManager() {
    }

    public void enqueueSyncUserInfoList(final long... userId) {
        mSyncQueue.enqueue(new SafetyRunnable(new MultiUserInfoCacheSyncTask(userId)));
    }

    public void enqueueSyncUserInfo(final long userId) {
        enqueueSyncUserInfo(userId, false);
    }

    public void enqueueSyncUserInfo(final long userId, boolean important) {
        mSyncQueue.enqueue(new SafetyRunnable(new UserInfoCacheSyncTask(userId, important)));
    }

    private static class UserInfoCacheSyncTask implements Runnable {

        /**
         * 超过此时间间隔时总是请求更新
         */
        private final long MAX_INTERVAL_MS = TimeUnit.DAYS.toMillis(1);

        private final long mUserId;
        private final boolean mImportant;
        private long mUserUpdateTimeMs;

        private UserInfoCacheSyncTask(long userId, boolean important) {
            mUserId = userId;
            mImportant = important;
        }

        @Override
        public void run() {
            if (mUserId <= 0) {
                IMLog.v("ignore. invalid user id: %s", mUserId);
                return;
            }

            boolean requireSync = mImportant;
            if (!requireSync) {
                final UserInfo userInfo = UserInfoCacheManager.getInstance().getByUserId(mUserId);
                if (userInfo == null) {
                    requireSync = true;
                    // 插入一条新纪录
                    UserInfoCacheManager.getInstance().touch(mUserId);
                } else {
                    mUserUpdateTimeMs = userInfo.updateTimeMs.get();
                    final long localLastModifyMs = userInfo.localLastModifyMs.get();
                    requireSync = (System.currentTimeMillis() - localLastModifyMs) >= MAX_INTERVAL_MS;
                }
            }

            if (!requireSync) {
                return;
            }

            final IMSessionManager.SessionTcpClientProxy proxy = IMSessionManager.getInstance().getSessionTcpClientProxy();
            if (proxy == null) {
                IMLog.v("SessionTcpClientProxy is null, abort sync user info. userId:%s", mUserId);
                return;
            }
            if (!proxy.isOnline()) {
                IMLog.v("SessionTcpClientProxy is not online, abort sync user info. userId:%s", mUserId);
                return;
            }
            final SessionTcpClient sessionTcpClient = proxy.getSessionTcpClient();
            if (sessionTcpClient == null) {
                IMLog.v("SessionTcpClient is null, abort sync user info. userId:%s", mUserId);
                return;
            }

            final MessagePacket messagePacket = new ResultIgnoreMessagePacket(
                    ProtoByteMessage.Type.encode(ProtoMessage.GetProfile.newBuilder()
                            .setSign(ResultIgnoreMessagePacket.SIGN_IGNORE)
                            .setUid(mUserId)
                            .setUpdateTime(mUserUpdateTimeMs / 1000) // 秒
                            .build())
            );
            sessionTcpClient.sendMessagePacketQuietly(messagePacket);
            if (messagePacket.getState() == MessagePacket.STATE_WAIT_RESULT) {
                // unsafe
                // 更新 localLastModifyMs
                UserInfoCacheManager.getInstance().touch(mUserId);
            } else {
                final Throwable e = new IllegalArgumentException("GetProfile message packet state error " + messagePacket);
                IMLog.e(e);
            }
        }
    }

    private static class MultiUserInfoCacheSyncTask implements Runnable {

        private static final int MAX_LENGTH = 100;
        private final long[] mUserIdList;

        private MultiUserInfoCacheSyncTask(long[] userIdList) {
            mUserIdList = userIdList;
        }

        @Override
        public void run() {
            if (mUserIdList == null || mUserIdList.length <= 0) {
                IMLog.e("ignore. invalid user id list");
                return;
            }

            if (mUserIdList.length > MAX_LENGTH) {
                final Throwable e = new IllegalArgumentException("length:" + mUserIdList.length + " too long, allow max:" + MAX_LENGTH);
                IMLog.e(e);
                return;
            }

            final Map<Long, Long> updateTimeMsMap = new HashMap<>();
            final List<UserInfo> userInfoList = UserInfoCacheManager.getInstance().getByUserIdList(mUserIdList);
            for (UserInfo userInfo : userInfoList) {
                updateTimeMsMap.put(userInfo.uid.get(), userInfo.updateTimeMs.get());
            }

            final List<ProtoMessage.GetProfile> getProfileList = new ArrayList<>();
            for (long userId : mUserIdList) {
                Long updateTimeMs = updateTimeMsMap.get(userId);
                if (updateTimeMs == null) {
                    updateTimeMs = 0L;
                }
                getProfileList.add(ProtoMessage.GetProfile.newBuilder()
                        .setSign(ResultIgnoreMessagePacket.SIGN_IGNORE)
                        .setUid(userId)
                        .setUpdateTime(updateTimeMs / 1000) // 秒
                        .build());
            }

            final IMSessionManager.SessionTcpClientProxy proxy = IMSessionManager.getInstance().getSessionTcpClientProxy();
            if (proxy == null) {
                IMLog.v("SessionTcpClientProxy is null, abort sync user id list");
                return;
            }
            if (!proxy.isOnline()) {
                IMLog.v("SessionTcpClientProxy is not online, abort sync user id list");
                return;
            }
            final SessionTcpClient sessionTcpClient = proxy.getSessionTcpClient();
            if (sessionTcpClient == null) {
                IMLog.v("SessionTcpClient is null, abort sync user id list");
                return;
            }

            final MessagePacket messagePacket = new ResultIgnoreMessagePacket(
                    ProtoByteMessage.Type.encode(ProtoMessage.GetProfiles.newBuilder()
                            .setSign(ResultIgnoreMessagePacket.SIGN_IGNORE)
                            .addAllGetProfiles(getProfileList)
                            .build())
            );
            sessionTcpClient.sendMessagePacketQuietly(messagePacket);
            if (messagePacket.getState() != MessagePacket.STATE_WAIT_RESULT) {
                final Throwable e = new IllegalArgumentException("GetProfiles message packet state error " + messagePacket);
                IMLog.e(e);
            }
        }

    }

}
