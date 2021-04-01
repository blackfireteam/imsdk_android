package com.masonsoft.imsdk.user;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

import com.idonans.core.Singleton;
import com.idonans.core.manager.ProcessManager;
import com.idonans.core.util.ContextUtil;
import com.idonans.core.util.IOUtil;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMProcessValidator;
import com.masonsoft.imsdk.core.RuntimeMode;
import com.masonsoft.imsdk.core.db.ColumnsSelector;
import com.masonsoft.imsdk.core.observable.UserInfoObservable;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户信息缓存管理。会缓存一部分用户信息到内存中。
 *
 * @since 1.0
 */
public class UserInfoCacheManager {

    private static final Singleton<UserInfoCacheManager> INSTANCE = new Singleton<UserInfoCacheManager>() {
        @Override
        protected UserInfoCacheManager create() {
            return new UserInfoCacheManager();
        }
    };

    public static UserInfoCacheManager getInstance() {
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

    private UserInfoCacheManager() {
    }

    @Nullable
    public UserInfo getByUserId(long userId) {
        final UserInfo cache = MemoryFullCache.DEFAULT.getFullCache(userId);
        if (cache != null) {
            IMLog.v("getByUserId cache hint userId:%s", userId);
            return cache;
        }

        IMLog.v("getByUserId cache miss, try read from db, userId:%s", userId);
        final UserInfo user = DatabaseProvider.getInstance().getTargetUser(userId);
        if (user != null) {
            MemoryFullCache.DEFAULT.addFullCache(user);
        }

        return user;
    }

    /**
     * 注意：此方法不会走缓存。
     */
    @NonNull
    public List<UserInfo> getByUserIdList(final long[] userIdList) {
        if (userIdList == null) {
            return new ArrayList<>();
        }
        if (userIdList.length <= 0) {
            return new ArrayList<>();
        }

        return DatabaseProvider.getInstance().getByUserIdList(userIdList);
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
                DatabaseProvider.getInstance().updateUser(userInfo);
            } else {
                DatabaseProvider.getInstance().insertUser(userInfo);
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
    public boolean touch(final long userId) {
        if (userId <= 0) {
            IMLog.e("touch ignore. invalid user id %s", userId);
            return false;
        }

        if (MemoryFullCache.DEFAULT.getFullCache(userId) != null) {
            // 命中缓存，说明记录已经存在
            return false;
        }
        if (DatabaseProvider.getInstance().touch(userId)) {
            MemoryFullCache.DEFAULT.removeFullCache(userId);
            UserInfoObservable.DEFAULT.notifyUserInfoChanged(userId);
            return true;
        }
        return false;
    }

    public boolean exists(long userId) {
        return getByUserId(userId) != null;
    }


}
