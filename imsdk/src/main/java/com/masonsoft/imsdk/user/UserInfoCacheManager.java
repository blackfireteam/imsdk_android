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

    public static final class DatabaseProvider {
        private static final Singleton<DatabaseProvider> INSTANCE = new Singleton<DatabaseProvider>() {
            @Override
            protected DatabaseProvider create() {
                return new DatabaseProvider();
            }
        };

        private static DatabaseProvider getInstance() {
            return INSTANCE.get();
        }

        private final DatabaseHelper mDBHelper;

        private DatabaseProvider() {
            mDBHelper = DatabaseHelper.DEFAULT;
        }

        @NonNull
        public List<UserInfo> getByUserIdList(final long[] userIdList) {
            final ColumnsSelector<UserInfo> columnsSelector = UserInfo.COLUMNS_SELECTOR_ALL;
            final List<UserInfo> result = new ArrayList<>();

            final StringBuilder inBuilder = new StringBuilder();
            inBuilder.append("(");
            boolean first = true;
            for (long userId : userIdList) {
                if (!first) {
                    inBuilder.append(",");
                }
                first = false;
                inBuilder.append(userId);
            }
            inBuilder.append(")");

            Cursor cursor = null;
            try {
                SQLiteDatabase db = mDBHelper.getDBHelper().getWritableDatabase();
                cursor = db.query(
                        DatabaseHelper.TABLE_NAME_USER,
                        columnsSelector.queryColumns(),
                        DatabaseHelper.ColumnsUser.C_USER_ID + " in " + inBuilder,
                        null,
                        null,
                        null,
                        null
                );

                if (cursor.moveToNext()) {
                    UserInfo item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                    IMLog.v("found user with user id:%s", item.uid);
                    result.add(item);
                }
            } catch (Throwable e) {
                IMLog.e(e);
                RuntimeMode.throwIfDebug(e);
            } finally {
                IOUtil.closeQuietly(cursor);
            }
            return result;
        }

        /**
         * @param targetUserId
         * @return 没有找到返回 null
         */
        @Nullable
        public UserInfo getTargetUser(final long targetUserId) {
            final ColumnsSelector<UserInfo> columnsSelector = UserInfo.COLUMNS_SELECTOR_ALL;

            Cursor cursor = null;
            try {
                SQLiteDatabase db = mDBHelper.getDBHelper().getWritableDatabase();
                cursor = db.query(
                        DatabaseHelper.TABLE_NAME_USER,
                        columnsSelector.queryColumns(),
                        DatabaseHelper.ColumnsUser.C_USER_ID + "=?",
                        new String[]{String.valueOf(targetUserId)},
                        null,
                        null,
                        null,
                        "0,1"
                );

                if (cursor.moveToNext()) {
                    UserInfo item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                    IMLog.v("found user with user id:%s", item.uid);
                    return item;
                }
            } catch (Throwable e) {
                IMLog.e(e);
                RuntimeMode.throwIfDebug(e);
            } finally {
                IOUtil.closeQuietly(cursor);
            }

            // user not found
            IMLog.v("user for target user id:%s not found", targetUserId);
            return null;
        }

        /**
         * 成功返回 true, 否则返回 false.
         */
        public boolean insertUser(final UserInfo user) {
            if (user == null) {
                Throwable e = new IllegalArgumentException("user is null");
                IMLog.e(e);
                RuntimeMode.throwIfDebug(e);
                return false;
            }

            if (user.uid.isUnset()) {
                final Throwable e = new IllegalArgumentException("invalid user id, unset");
                IMLog.e(e);
                RuntimeMode.throwIfDebug(e);
                return false;
            }

            if (user.uid.get() == null || user.uid.get() <= 0) {
                final Throwable e = new IllegalArgumentException("invalid user userId " + user.uid);
                IMLog.e(e);
                RuntimeMode.throwIfDebug(e);
                return false;
            }

            // 设置 lastModify
            user.localLastModifyMs.set(System.currentTimeMillis());

            try {
                IMLog.v("insertUser %s", user);
                SQLiteDatabase db = mDBHelper.getDBHelper().getWritableDatabase();
                long rowId = db.insert(
                        DatabaseHelper.TABLE_NAME_USER,
                        null,
                        user.toContentValues()
                );

                if (rowId <= 0) {
                    Throwable e = new IllegalAccessException("insert user for target user id:" + user.uid.get() + " return rowId " + rowId);
                    IMLog.e(e);
                    RuntimeMode.throwIfDebug(e);
                } else {
                    IMLog.v("insert user for target user id:%s return rowId:%s", user.uid.get(), rowId);
                }
                return rowId > 0;
            } catch (Throwable e) {
                IMLog.e(e);
                RuntimeMode.throwIfDebug(e);
            }
            return false;
        }

        /**
         * 如果记录存在，则忽略。否则插入一条新纪录。如果成功插入一条新记录返回 true, 否则返回 false.
         *
         * @param userId
         * @return
         */
        public boolean touch(final long userId) {
            if (userId <= 0) {
                final Throwable e = new IllegalArgumentException("invalid user id " + userId);
                IMLog.e(e);
                RuntimeMode.throwIfDebug(e);
                return false;
            }

            try {
                IMLog.v("touch userId:%s", userId);
                SQLiteDatabase db = mDBHelper.getDBHelper().getWritableDatabase();

                final ContentValues contentValuesInsert = new ContentValues();
                contentValuesInsert.put(DatabaseHelper.ColumnsUser.C_USER_ID, userId);
                contentValuesInsert.put(DatabaseHelper.ColumnsUser.C_LOCAL_LAST_MODIFY_MS, System.currentTimeMillis());

                long rowId = db.insertWithOnConflict(
                        DatabaseHelper.TABLE_NAME_USER,
                        null,
                        contentValuesInsert,
                        SQLiteDatabase.CONFLICT_IGNORE
                );
                IMLog.v("touch user for target user id:%s rowId:%s", userId, rowId);
                return rowId > 0;
            } catch (Throwable e) {
                IMLog.e(e);
                RuntimeMode.throwIfDebug(e);
            }
            return false;
        }

        /**
         * 更新成功返回 true, 否则返回 false.
         */
        public boolean updateUser(final UserInfo user) {
            if (user == null) {
                Throwable e = new IllegalArgumentException("user is null");
                IMLog.e(e);
                RuntimeMode.throwIfDebug(e);
                return false;
            }

            if (user.uid.isUnset()) {
                final Throwable e = new IllegalArgumentException("invalid user id, unset");
                IMLog.e(e);
                RuntimeMode.throwIfDebug(e);
                return false;
            }

            if (user.uid.get() == null || user.uid.get() <= 0) {
                final Throwable e = new IllegalArgumentException("invalid user userId " + user.uid);
                IMLog.e(e);
                RuntimeMode.throwIfDebug(e);
                return false;
            }

            // 设置 lastModify
            user.localLastModifyMs.set(System.currentTimeMillis());

            try {
                IMLog.v("updateUser %s", user);
                SQLiteDatabase db = mDBHelper.getDBHelper().getWritableDatabase();
                long rowsAffected = db.update(
                        DatabaseHelper.TABLE_NAME_USER,
                        user.toContentValues(),
                        DatabaseHelper.ColumnsUser.C_USER_ID + "=?",
                        new String[]{String.valueOf(user.uid.get())}
                );

                if (rowsAffected != 1) {
                    Throwable e = new IllegalAccessException("update user for target user id:" + user.uid.get() + " rowsAffected " + rowsAffected);
                    IMLog.e(e);
                    RuntimeMode.throwIfDebug(e);
                    return false;
                }

                IMLog.v("update user for target user id:%s rowsAffected:%s", user.uid.get(), rowsAffected);
                return true;
            } catch (Throwable e) {
                IMLog.e(e);
                RuntimeMode.throwIfDebug(e);
            }
            return false;
        }


    }

}
