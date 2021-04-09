package com.masonsoft.imsdk.user;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

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
import com.masonsoft.imsdk.util.Objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用户信息的同步管理
 *
 * @since 1.0
 */
public class UserInfoSyncManager {

    private static final Singleton<UserInfoSyncManager> INSTANCE = new Singleton<UserInfoSyncManager>() {
        @Override
        protected UserInfoSyncManager create() {
            return new UserInfoSyncManager();
        }
    };

    public static UserInfoSyncManager getInstance() {
        IMProcessValidator.validateProcess();

        return INSTANCE.get();
    }

    private static class MemoryFullCache {

        private static final MemoryFullCache DEFAULT = new MemoryFullCache();

        private static final int MEMORY_CACHE_SIZE = 100;
        @NonNull
        private final LruCache<Long, UserInfoSync> mFullCaches = new LruCache<>(MEMORY_CACHE_SIZE);

        private void addFullCache(@NonNull UserInfoSync userInfoSync) {
            if (userInfoSync.uid.isUnset()) {
                IMLog.e(Objects.defaultObjectTag(this) + " uid is unset %s", userInfoSync);
                return;
            }
            mFullCaches.put(userInfoSync.uid.get(), userInfoSync);
        }

        private void removeFullCache(long userId) {
            mFullCaches.remove(userId);
        }

        @Nullable
        private UserInfoSync getFullCache(long userId) {
            return mFullCaches.get(userId);
        }
    }

    private final TaskQueue mSyncQueue = new TaskQueue(1);

    private UserInfoSyncManager() {
    }

    @Nullable
    public UserInfoSync getUserInfoSyncByUserId(long userId) {
        final UserInfoSync cache = MemoryFullCache.DEFAULT.getFullCache(userId);
        if (cache != null) {
            IMLog.v(Objects.defaultObjectTag(this) + " getUserInfoSyncByUserId cache hit userId:%s", userId);
            return cache;
        }

        IMLog.v(Objects.defaultObjectTag(this) + " getUserInfoSyncByUserId cache miss, try read from db, userId:%s", userId);
        final UserInfoSync userInfoSync = UserInfoSyncDatabaseProvider.getInstance().getUserInfoSyncByUserId(userId);
        if (userInfoSync != null) {
            MemoryFullCache.DEFAULT.addFullCache(userInfoSync);
        }

        return userInfoSync;
    }

    /**
     * 插入或更新
     */
    public void insertOrUpdateUserInfoSync(UserInfoSync userInfoSync) {
        if (userInfoSync == null) {
            return;
        }

        final long userId = userInfoSync.uid.getOrDefault(-1L);
        if (userId <= 0) {
            IMLog.e(Objects.defaultObjectTag(this) + " insertOrUpdateUserInfoSync ignore. invalid user id %s.", userId);
            return;
        }

        UserInfoSync cacheUserInfoSync = getUserInfoSyncByUserId(userId);
        if (cacheUserInfoSync != null) {
            if (!cacheUserInfoSync.localLastSyncTimeMs.isUnset()
                    && !userInfoSync.localLastSyncTimeMs.isUnset()) {
                if (cacheUserInfoSync.localLastSyncTimeMs.get() >= userInfoSync.localLastSyncTimeMs.get()) {
                    IMLog.v(Objects.defaultObjectTag(this) + " ignore insertOrUpdateUserInfoSync cacheUserInfoSync is newer");
                    return;
                }
            }
        }

        try {
            if (cacheUserInfoSync != null) {
                UserInfoSyncDatabaseProvider.getInstance().updateUserInfoSync(userInfoSync);
            } else {
                UserInfoSyncDatabaseProvider.getInstance().insertUserInfoSync(userInfoSync);
            }
        } catch (Throwable e) {
            // ignore
        }

        MemoryFullCache.DEFAULT.removeFullCache(userId);
    }

    /**
     * 如果记录存在，则忽略。否则插入一条新纪录。如果成功插入一条新记录返回 true, 否则返回 false.
     *
     * @param userId
     * @return
     */
    public boolean touchUserInfoSync(final long userId) {
        if (userId <= 0) {
            IMLog.e(Objects.defaultObjectTag(this) + " touchUserInfoSync ignore. invalid user id %s", userId);
            return false;
        }

        if (MemoryFullCache.DEFAULT.getFullCache(userId) != null) {
            // 命中缓存，说明记录已经存在
            return false;
        }
        if (UserInfoSyncDatabaseProvider.getInstance().touchUserInfoSync(userId)) {
            MemoryFullCache.DEFAULT.removeFullCache(userId);
            return true;
        }
        return false;
    }

    public void enqueueSyncUserInfoList(final List<Long> userIdList) {
        mSyncQueue.enqueue(new SafetyRunnable(new MultiUserInfoSyncTask(userIdList)));
    }

    public void enqueueSyncUserInfo(final long userId) {
        enqueueSyncUserInfo(userId, false);
    }

    public void enqueueSyncUserInfo(final long userId, boolean important) {
        enqueueSyncUserInfo(userId, 0L, important);
    }

    public void enqueueSyncUserInfo(final long userId, long serverUpdateTimeMs) {
        enqueueSyncUserInfo(userId, serverUpdateTimeMs, false);
    }

    public void enqueueSyncUserInfo(final long userId, long serverUpdateTimeMs, boolean important) {
        mSyncQueue.enqueue(new SafetyRunnable(new UserInfoSyncTask(userId, serverUpdateTimeMs, important)));
    }

    private static class UserInfoSyncTask implements Runnable {

        /**
         * 超过此时间间隔时总是请求更新
         */
        private final long MAX_INTERVAL_MS = TimeUnit.DAYS.toMillis(1);
        /**
         * 当本地完全没有目标用户的信息时(一次都没有同步成功过), 发起重复请求的最少间隔时间
         */
        private final long REQUIRE_AT_LEAST_ONE_USER_INFO_MIN_INTERVAL_MS = TimeUnit.MINUTES.toMillis(2);

        private final boolean mForce;

        private final long mUserId;
        private final long mServerUpdateTimeMs;
        private long mUserUpdateTimeMs;

        private UserInfoSyncTask(long userId, long serverUpdateTimeMs, boolean force) {
            mUserId = userId;
            mServerUpdateTimeMs = serverUpdateTimeMs;
            mForce = force;
        }

        @Override
        public void run() {
            if (mUserId <= 0) {
                IMLog.v(Objects.defaultObjectTag(this) + " ignore. invalid user id: %s", mUserId);
                return;
            }

            boolean requireSync = mForce;
            if (!requireSync) {
                long localLastSyncTimeMs = 0L;

                final UserInfoSync userInfoSync = UserInfoSyncManager.getInstance().getUserInfoSyncByUserId(mUserId);
                if (userInfoSync == null) {
                    requireSync = true;
                    UserInfoSyncManager.getInstance().touchUserInfoSync(mUserId);
                } else {
                    localLastSyncTimeMs = userInfoSync.localLastSyncTimeMs.get();
                    if ((System.currentTimeMillis() - localLastSyncTimeMs) >= MAX_INTERVAL_MS) {
                        requireSync = true;
                    }
                }

                final UserInfo userInfo = UserInfoManager.getInstance().getByUserId(mUserId);
                if (userInfo == null) {
                    requireSync = true;
                    UserInfoManager.getInstance().touchUserInfo(mUserId);
                } else {
                    mUserUpdateTimeMs = userInfo.updateTimeMs.getOrDefault(0L);
                    if (mServerUpdateTimeMs > 0 && mServerUpdateTimeMs > mUserUpdateTimeMs) {
                        requireSync = true;
                    }
                }

                if (!requireSync) {
                    if (mUserUpdateTimeMs == 0L) {
                        // 本地还没有至少一次完整的用户信息
                        if (System.currentTimeMillis() - localLastSyncTimeMs >= REQUIRE_AT_LEAST_ONE_USER_INFO_MIN_INTERVAL_MS) {
                            final UserInfoSync userInfoSyncUpdate = new UserInfoSync();
                            userInfoSyncUpdate.uid.set(mUserId);
                            userInfoSyncUpdate.localLastSyncTimeMs.set(System.currentTimeMillis());
                            UserInfoSyncManager.getInstance().insertOrUpdateUserInfoSync(userInfoSyncUpdate);
                            requireSync = true;
                        }
                    }
                }
            }

            if (!requireSync) {
                return;
            }

            final IMSessionManager.SessionTcpClientProxy proxy = IMSessionManager.getInstance().getSessionTcpClientProxy();
            if (proxy == null) {
                IMLog.v(Objects.defaultObjectTag(this) + " SessionTcpClientProxy is null, abort sync user info. userId:%s", mUserId);
                return;
            }
            if (!proxy.isOnline()) {
                IMLog.v(Objects.defaultObjectTag(this) + " SessionTcpClientProxy is not online, abort sync user info. userId:%s", mUserId);
                return;
            }
            final SessionTcpClient sessionTcpClient = proxy.getSessionTcpClient();
            if (sessionTcpClient == null) {
                IMLog.v(Objects.defaultObjectTag(this) + " SessionTcpClient is null, abort sync user info. userId:%s", mUserId);
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
            if (messagePacket.getState() != MessagePacket.STATE_WAIT_RESULT) {
                final Throwable e = new IllegalArgumentException("GetProfile message packet state error " + messagePacket);
                IMLog.e(e);
            } else {
                IMLog.v(Objects.defaultObjectTag(this) + " send sync user info for user id:%s", mUserId);
                final UserInfoSync userInfoSyncUpdate = new UserInfoSync();
                userInfoSyncUpdate.uid.set(mUserId);
                userInfoSyncUpdate.localLastSyncTimeMs.set(System.currentTimeMillis());
                UserInfoSyncManager.getInstance().insertOrUpdateUserInfoSync(userInfoSyncUpdate);
            }
        }
    }

    private static class MultiUserInfoSyncTask implements Runnable {

        /**
         * 超过此时间间隔时总是请求更新
         */
        private final long MAX_INTERVAL_MS = TimeUnit.DAYS.toMillis(1);
        /**
         * 当本地完全没有目标用户的信息时(一次都没有同步成功过), 发起重复请求的最少间隔时间
         */
        private final long REQUIRE_AT_LEAST_ONE_USER_INFO_MIN_INTERVAL_MS = TimeUnit.MINUTES.toMillis(2);

        private static final int MAX_LENGTH = 100;
        private final List<Long> mOriginUserIdList;

        private MultiUserInfoSyncTask(final List<Long> userIdList) {
            mOriginUserIdList = userIdList;
        }

        @NonNull
        private List<Long> filterUserIdList() {
            List<Long> filterUserIdList = new ArrayList<>();
            if (mOriginUserIdList != null) {
                for (long userId : mOriginUserIdList) {
                    if (accept(userId)) {
                        filterUserIdList.add(userId);
                    }
                }
            }

            if (filterUserIdList.size() > MAX_LENGTH) {
                final List<Long> overflow = filterUserIdList.subList(MAX_LENGTH, filterUserIdList.size());
                UserInfoSyncManager.getInstance().enqueueSyncUserInfoList(overflow);
                filterUserIdList = filterUserIdList.subList(0, MAX_LENGTH);
            }
            return filterUserIdList;
        }

        private boolean accept(final long userId) {
            boolean requireSync = false;
            long localLastSyncTimeMs = 0L;
            long userUpdateTimeMs = 0L;

            final UserInfoSync userInfoSync = UserInfoSyncManager.getInstance().getUserInfoSyncByUserId(userId);
            if (userInfoSync == null) {
                requireSync = true;
                UserInfoSyncManager.getInstance().touchUserInfoSync(userId);
            } else {
                localLastSyncTimeMs = userInfoSync.localLastSyncTimeMs.get();
                if ((System.currentTimeMillis() - localLastSyncTimeMs) >= MAX_INTERVAL_MS) {
                    requireSync = true;
                }
            }

            final UserInfo userInfo = UserInfoManager.getInstance().getByUserId(userId);
            if (userInfo == null) {
                requireSync = true;
                UserInfoManager.getInstance().touchUserInfo(userId);
            } else {
                userUpdateTimeMs = userInfo.updateTimeMs.getOrDefault(0L);
            }

            if (!requireSync) {
                if (userUpdateTimeMs == 0L) {
                    // 本地还没有至少一次完整的用户信息
                    if (System.currentTimeMillis() - localLastSyncTimeMs >= REQUIRE_AT_LEAST_ONE_USER_INFO_MIN_INTERVAL_MS) {
                        final UserInfoSync userInfoSyncUpdate = new UserInfoSync();
                        userInfoSyncUpdate.uid.set(userId);
                        userInfoSyncUpdate.localLastSyncTimeMs.set(System.currentTimeMillis());
                        UserInfoSyncManager.getInstance().insertOrUpdateUserInfoSync(userInfoSyncUpdate);
                        requireSync = true;
                    }
                }
            }

            return requireSync;
        }

        @Override
        public void run() {
            final List<Long> filterUserIdList = filterUserIdList();
            IMLog.v(Objects.defaultObjectTag(this) + " run mOriginUserIdList.size:%s -> filterUserIdList.size:%s",
                    mOriginUserIdList.size(), filterUserIdList.size());
            if (filterUserIdList.size() <= 0) {
                IMLog.e("ignore. filterUserIdList is empty");
                return;
            }

            final Map<Long, Long> updateTimeMsMap = new HashMap<>();
            final List<UserInfo> userInfoList = UserInfoManager.getInstance().getByUserIdList(filterUserIdList);
            for (UserInfo userInfo : userInfoList) {
                updateTimeMsMap.put(userInfo.uid.get(), userInfo.updateTimeMs.getOrDefault(0L));
            }

            final List<ProtoMessage.GetProfile> getProfileList = new ArrayList<>();
            for (long userId : filterUserIdList) {
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
                IMLog.v(Objects.defaultObjectTag(this) + " SessionTcpClientProxy is null, abort sync user id list");
                return;
            }
            if (!proxy.isOnline()) {
                IMLog.v(Objects.defaultObjectTag(this) + " SessionTcpClientProxy is not online, abort sync user id list");
                return;
            }
            final SessionTcpClient sessionTcpClient = proxy.getSessionTcpClient();
            if (sessionTcpClient == null) {
                IMLog.v(Objects.defaultObjectTag(this) + " SessionTcpClient is null, abort sync user id list");
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
            } else {
                final long syncTimeMs = System.currentTimeMillis();
                final int size = filterUserIdList.size();
                int index = 0;
                for (long userId : filterUserIdList) {
                    IMLog.v(Objects.defaultObjectTag(this) + " multi [%s/%s] send sync user info for user id:%s", ++index, size, userId);
                    final UserInfoSync userInfoSyncUpdate = new UserInfoSync();
                    userInfoSyncUpdate.uid.set(userId);
                    userInfoSyncUpdate.localLastSyncTimeMs.set(syncTimeMs);
                    UserInfoSyncManager.getInstance().insertOrUpdateUserInfoSync(userInfoSyncUpdate);
                }
            }
        }
    }
}
