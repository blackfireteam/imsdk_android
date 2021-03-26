package com.masonsoft.imsdk.user;

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

/**
 * 用户信息缓存管理。会缓存一部分用户信息到内存中。
 */
public class UserCacheManager {

    private static final Singleton<UserCacheManager> INSTANCE = new Singleton<UserCacheManager>() {
        @Override
        protected UserCacheManager create() {
            return new UserCacheManager();
        }
    };

    public static UserCacheManager getInstance() {
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

    private UserCacheManager() {
    }

    @Nullable
    public UserInfo getByUserId(long userId) {
        final UserInfo cache = MemoryFullCache.DEFAULT.getFullCache(userId);
        if (cache != null) {
            IMLog.v("getByUserId cache hint userId:%s", userId);
            return cache;
        }

        IMLog.v("getByUserId cache miss, try read from db, userId:%s", userId);
        final UserInfo user = DatabaseProvider.getInstance().getTargetUser(userId, null);
        if (user != null) {
            MemoryFullCache.DEFAULT.addFullCache(user);
        }

        return user;
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

    public boolean exists(long userId) {
        return getByUserId(userId) != null;
    }

    public static final class DatabaseProvider {
        private static Singleton<DatabaseProvider> sInstance = new Singleton<DatabaseProvider>() {
            @Override
            protected DatabaseProvider create() {
                return new DatabaseProvider();
            }
        };

        private static DatabaseProvider getInstance() {
            return sInstance.get();
        }

        private DatabaseHelper mDBHelper;

        private DatabaseProvider() {
            mDBHelper = new DatabaseHelper();
        }

        /**
         * @param targetUserId
         * @return 没有找到返回 null
         */
        @Nullable
        public UserInfo getTargetUser(final long targetUserId, @Nullable ColumnsSelector<UserInfo> columnsSelector) {
            if (columnsSelector == null) {
                columnsSelector = UserInfo.COLUMNS_SELECTOR_ALL;
            }
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
                } else {
                    IMLog.v("update user for target user id:%s rowsAffected:%s", user.uid.get(), rowsAffected);
                }
                return true;
            } catch (Throwable e) {
                IMLog.e(e);
                RuntimeMode.throwIfDebug(e);
            }
            return false;
        }

        public static final class DatabaseHelper {

            // 当前数据库最新版本号
            private static final int DB_VERSION = 1;
            // 用户表
            public static final String TABLE_NAME_USER = "t_user_cache";

            private final SQLiteOpenHelper mDBHelper;

            /**
             * 用户表
             */
            public interface ColumnsUser {

                /**
                 * 用户 id, 全局唯一
                 *
                 * @since db version 1
                 */
                String C_USER_ID = "c_user_id";

                /**
                 * 用户数据的 json 序列化
                 *
                 * @since db version 1
                 */
                String C_USER_JSON = "c_user_json";
            }

            private DatabaseHelper() {
                final String dbName = IMConstants.GLOBAL_NAMESPACE + "_user_cache_manager_20210325_" + ProcessManager.getInstance().getProcessTag();
                mDBHelper = new SQLiteOpenHelper(ContextUtil.getContext(), dbName, null, DB_VERSION) {
                    @Override
                    public void onCreate(SQLiteDatabase db) {
                        try {
                            db.beginTransaction();

                            db.execSQL(getSQLCreateTableUser());
                            for (String sqlIndex : getSQLIndexTableUser()) {
                                db.execSQL(sqlIndex);
                            }

                            db.setTransactionSuccessful();
                        } catch (Throwable e) {
                            IMLog.e(e);
                        } finally {
                            db.endTransaction();
                        }
                    }

                    @Override
                    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                        throw new IllegalAccessError("need config database upgrade from " + oldVersion + " to " + newVersion);
                    }
                };
            }

            public SQLiteOpenHelper getDBHelper() {
                return mDBHelper;
            }

            /**
             * 用户表创建语句(数据库最新版本)
             */
            @NonNull
            private String getSQLCreateTableUser() {
                return "create table " + TABLE_NAME_USER + " (" +
                        ColumnsUser.C_USER_ID + " integer primary key," +
                        ColumnsUser.C_USER_JSON + " text" +
                        ")";
            }

            /**
             * 用户表创建索引语句(数据库最新版本)
             */
            @NonNull
            private String[] getSQLIndexTableUser() {
                return new String[]{
                };
            }

        }

    }

}
