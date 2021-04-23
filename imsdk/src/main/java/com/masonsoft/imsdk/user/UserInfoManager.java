package com.masonsoft.imsdk.user;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMProcessValidator;
import com.masonsoft.imsdk.core.observable.UserInfoObservable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.github.idonans.core.Singleton;

/**
 * 用户信息管理。会缓存一部分用户信息到内存中。
 *
 * @since 1.0
 */
public class UserInfoManager {

    private static final Singleton<UserInfoManager> INSTANCE = new Singleton<UserInfoManager>() {
        @Override
        protected UserInfoManager create() {
            return new UserInfoManager();
        }
    };

    public static UserInfoManager getInstance() {
        IMProcessValidator.validateProcess();

        return INSTANCE.get();
    }

    private static class MemoryFullCache {

        private static final MemoryFullCache DEFAULT = new MemoryFullCache();

        private static final int MEMORY_CACHE_SIZE = 100;
        @NonNull
        private final LruCache<Long, UserInfo> mFullCaches = new LruCache<>(MEMORY_CACHE_SIZE);

        private void addFullCache(@NonNull UserInfo userInfo) {
            if (userInfo.uid.isUnset()) {
                IMLog.e("uid is unset %s", userInfo);
                return;
            }
            mFullCaches.put(userInfo.uid.get(), userInfo);
        }

        private void removeFullCache(long userId) {
            mFullCaches.remove(userId);
        }

        @Nullable
        private UserInfo getFullCache(long userId) {
            return mFullCaches.get(userId);
        }
    }

    private UserInfoManager() {
    }

    @Nullable
    public UserInfo getByUserId(long userId) {
        final UserInfo cache = MemoryFullCache.DEFAULT.getFullCache(userId);
        if (cache != null) {
            IMLog.v("getByUserId cache hit userId:%s", userId);
            return cache;
        }

        IMLog.v("getByUserId cache miss, try read from db, userId:%s", userId);
        final UserInfo user = UserInfoDatabaseProvider.getInstance().getUserInfoByUserId(userId);
        if (user != null) {
            MemoryFullCache.DEFAULT.addFullCache(user);
        }

        return user;
    }

    /**
     * 注意：此方法不会走缓存。
     */
    @NonNull
    public List<UserInfo> getByUserIdList(final List<Long> userIdList) {
        if (userIdList == null) {
            return new ArrayList<>();
        }
        if (userIdList.size() <= 0) {
            return new ArrayList<>();
        }

        return UserInfoDatabaseProvider.getInstance().getUserInfoByUserIdList(userIdList);
    }

    /**
     * 插入或更新
     */
    public void insertOrUpdateUser(UserInfo userInfo) {
        if (userInfo == null) {
            return;
        }

        final long userId = userInfo.uid.getOrDefault(-1L);
        if (userId <= 0) {
            IMLog.e("insertOrUpdateUser ignore. invalid user id %s.", userId);
            return;
        }

        UserInfo cacheUserInfo = getByUserId(userId);
        if (cacheUserInfo != null) {
            if (!cacheUserInfo.updateTimeMs.isUnset()
                    && !userInfo.updateTimeMs.isUnset()) {
                if (cacheUserInfo.updateTimeMs.get() >= userInfo.updateTimeMs.get()) {
                    IMLog.v("ignore insertOrUpdateUser cacheUserInfo is newer");
                    return;
                }
            }
        }

        try {
            if (cacheUserInfo != null) {
                UserInfoDatabaseProvider.getInstance().updateUserInfo(userInfo);
            } else {
                UserInfoDatabaseProvider.getInstance().insertUserInfo(userInfo);
            }
        } catch (Throwable e) {
            // ignore
        }

        MemoryFullCache.DEFAULT.removeFullCache(userId);
        UserInfoObservable.DEFAULT.notifyUserInfoChanged(userId);
    }

    /**
     * 如果记录存在，则忽略。否则插入一条新纪录。如果成功插入一条新记录返回 true, 否则返回 false.
     *
     * @param userId
     * @return
     */
    public boolean touchUserInfo(final long userId) {
        if (userId <= 0) {
            IMLog.e("touchUserInfo ignore. invalid user id %s", userId);
            return false;
        }

        if (MemoryFullCache.DEFAULT.getFullCache(userId) != null) {
            // 命中缓存，说明记录已经存在
            return false;
        }
        if (UserInfoDatabaseProvider.getInstance().touchUserInfo(userId)) {
            MemoryFullCache.DEFAULT.removeFullCache(userId);
            UserInfoObservable.DEFAULT.notifyUserInfoChanged(userId);
            return true;
        }
        return false;
    }

    public boolean exists(long userId) {
        return getByUserId(userId) != null;
    }

    /**
     * 如果头像发生了变更，则更新
     */
    public void updateAvatar(final long userId, @Nullable final String avatar) {
        final UserInfo userInfoUpdate = new UserInfo();
        userInfoUpdate.avatar.set(avatar);
        this.updateManual(userId, userInfoUpdate);
    }

    /**
     * 如果昵称发生了变更，则更新
     */
    public void updateNickname(final long userId, @Nullable final String nickname) {
        final UserInfo userInfoUpdate = new UserInfo();
        userInfoUpdate.nickname.set(nickname);
        this.updateManual(userId, userInfoUpdate);
    }

    /**
     * 如果头像与昵称发生了变化，则更新
     */
    public void updateAvatarAndNickname(final long userId, @Nullable final String avatar, @Nullable final String nickname) {
        final UserInfo userInfoUpdate = new UserInfo();
        userInfoUpdate.avatar.set(avatar);
        userInfoUpdate.nickname.set(nickname);
        this.updateManual(userId, userInfoUpdate);
    }

    /**
     * 如果 gold 发生了变更，则更新
     */
    public void updateGold(final long userId, final boolean gold) {
        final UserInfo userInfoUpdate = new UserInfo();
        userInfoUpdate.gold.set(gold ? IMConstants.TRUE : IMConstants.FALSE);
        this.updateManual(userId, userInfoUpdate);
    }

    /**
     * 如果 verified 发生了变更，则更新
     */
    public void updateVerified(final long userId, final boolean verified) {
        final UserInfo userInfoUpdate = new UserInfo();
        userInfoUpdate.verified.set(verified ? IMConstants.TRUE : IMConstants.FALSE);
        this.updateManual(userId, userInfoUpdate);
    }

    /**
     * 如果指定条目发生了变更，则更新
     */
    public void updateManual(final long userId, @Nullable UserInfo userInfoUpdate) {
        boolean update = false;
        final UserInfo userInfo = getByUserId(userId);
        if (userInfo != null) {
            if (userInfoUpdate != null) {
                if (!userInfoUpdate.avatar.isUnset()) {
                    final String oldAvatar = userInfo.avatar.getOrDefault(null);
                    if (!Objects.equals(oldAvatar, userInfoUpdate.avatar.get())) {
                        update = true;
                    }
                }

                if (!update) {
                    if (!userInfoUpdate.nickname.isUnset()) {
                        final String oldNickname = userInfo.nickname.getOrDefault(null);
                        if (!Objects.equals(oldNickname, userInfoUpdate.nickname.get())) {
                            update = true;
                        }
                    }
                }

                if (!update) {
                    if (!userInfoUpdate.gold.isUnset()) {
                        final Integer oldGold = userInfo.gold.getOrDefault(null);
                        if (!Objects.equals(oldGold, userInfoUpdate.gold.get())) {
                            update = true;
                        }
                    }
                }

                if (!update) {
                    if (!userInfoUpdate.verified.isUnset()) {
                        final Integer oldVerified = userInfo.verified.getOrDefault(null);
                        if (!Objects.equals(oldVerified, userInfoUpdate.verified.get())) {
                            update = true;
                        }
                    }
                }
            }
        } else {
            update = true;
        }
        if (!update) {
            return;
        }

        final UserInfo mergeUserInfo;
        if (userInfo == null) {
            mergeUserInfo = UserInfoFactory.create(userId);
        } else {
            mergeUserInfo = UserInfoFactory.copy(userInfo);
        }

        if (!userInfoUpdate.avatar.isUnset()) {
            mergeUserInfo.avatar.apply(userInfoUpdate.avatar);
        }
        if (!userInfoUpdate.nickname.isUnset()) {
            mergeUserInfo.nickname.apply(userInfoUpdate.nickname);
        }
        if (!userInfoUpdate.gold.isUnset()) {
            mergeUserInfo.gold.apply(userInfoUpdate.gold);
        }
        if (!userInfoUpdate.verified.isUnset()) {
            mergeUserInfo.verified.apply(userInfoUpdate.verified);
        }

        mergeUserInfo.updateTimeMs.set(System.currentTimeMillis());

        insertOrUpdateUser(mergeUserInfo);
    }

}
