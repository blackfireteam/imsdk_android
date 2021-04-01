package com.masonsoft.imsdk.user;

import com.idonans.core.Singleton;
import com.idonans.core.thread.TaskQueue;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMProcessValidator;
import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.core.message.packet.MessagePacket;
import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.lang.SafetyRunnable;

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

    public void enqueueSyncUserInfo(final long userId) {
        enqueueSyncUserInfo(userId, false);
    }

    public void enqueueSyncUserInfo(final long userId, boolean important) {
        mSyncQueue.enqueue(new SafetyRunnable(new UserInfoCacheSyncTask(userId, important)));
    }

    private class UserInfoCacheSyncTask implements Runnable {

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
            boolean requireSync = mImportant;
            if (!requireSync) {
                final UserInfo userInfo = UserInfoCacheManager.getInstance().getByUserId(mUserId);
                if (userInfo == null) {
                    requireSync = true;
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

            final MessagePacket
            sessionTcpClient.sendMessagePacketQuietly();
            // TODO
        }
    }

}
