package com.masonsoft.imsdk.user;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import com.idonans.core.Singleton;
import com.idonans.core.util.IOUtil;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMProcessValidator;
import com.masonsoft.imsdk.core.RuntimeMode;
import com.masonsoft.imsdk.core.db.ColumnsSelector;

/**
 * @since 1.0
 */
public class UserInfoSyncDatabaseProvider {

    public static final Singleton<UserInfoSyncDatabaseProvider> INSTANCE = new Singleton<UserInfoSyncDatabaseProvider>() {
        @Override
        protected UserInfoSyncDatabaseProvider create() {
            return new UserInfoSyncDatabaseProvider();
        }
    };

    public static UserInfoSyncDatabaseProvider getInstance() {
        IMProcessValidator.validateProcess();

        return INSTANCE.get();
    }

    private final UserInfoDatabaseHelper mDBHelper;

    private UserInfoSyncDatabaseProvider() {
        mDBHelper = UserInfoDatabaseHelper.getInstance();
    }

    /**
     * @return 没有找到返回 null
     */
    @Nullable
    public UserInfoSync getUserInfoSyncByUserId(final long userId) {
        final ColumnsSelector<UserInfoSync> columnsSelector = UserInfoSync.COLUMNS_SELECTOR_ALL;

        Cursor cursor = null;
        try {
            SQLiteDatabase db = mDBHelper.getDBHelper().getWritableDatabase();
            cursor = db.query(
                    UserInfoDatabaseHelper.TABLE_NAME_USER_INFO_SYNC,
                    columnsSelector.queryColumns(),
                    UserInfoDatabaseHelper.ColumnsUserInfoSync.C_USER_ID + "=?",
                    new String[]{String.valueOf(userId)},
                    null,
                    null,
                    null,
                    "0,1"
            );

            if (cursor.moveToNext()) {
                UserInfoSync item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                IMLog.v("getUserInfoSyncByUserId found userInfoSync with user id:%s", item.uid);
                return item;
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        // user info sync not found
        IMLog.v("getUserInfoSyncByUserId for user id:%s not found", userId);
        return null;
    }

    /**
     * 成功返回 true, 否则返回 false.
     */
    public boolean insertUserInfoSync(final UserInfoSync userInfoSync) {
        if (userInfoSync == null) {
            Throwable e = new IllegalArgumentException("insertUserInfoSync userInfoSync is null");
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
            return false;
        }

        if (userInfoSync.uid.isUnset()) {
            final Throwable e = new IllegalArgumentException("insertUserInfoSync invalid user id, unset");
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
            return false;
        }

        if (userInfoSync.uid.get() == null || userInfoSync.uid.get() <= 0) {
            final Throwable e = new IllegalArgumentException("insertUserInfoSync invalid user userId " + userInfoSync.uid);
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
            return false;
        }

        try {
            IMLog.v("insertUserInfoSync %s", userInfoSync);
            SQLiteDatabase db = mDBHelper.getDBHelper().getWritableDatabase();
            long rowId = db.insert(
                    UserInfoDatabaseHelper.TABLE_NAME_USER_INFO_SYNC,
                    null,
                    userInfoSync.toContentValues()
            );

            if (rowId <= 0) {
                Throwable e = new IllegalAccessException("insertUserInfoSync for user id:" + userInfoSync.uid.get() + " return rowId " + rowId);
                IMLog.e(e);
                RuntimeMode.throwIfDebug(e);
            } else {
                IMLog.v("insertUserInfoSync for user id:%s return rowId:%s", userInfoSync.uid.get(), rowId);
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
    public boolean touchUserInfoSync(final long userId) {
        if (userId <= 0) {
            final Throwable e = new IllegalArgumentException("invalid user id " + userId);
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
            return false;
        }

        try {
            IMLog.v("touchUserInfoSync userId:%s", userId);
            SQLiteDatabase db = mDBHelper.getDBHelper().getWritableDatabase();

            final ContentValues contentValuesInsert = new ContentValues();
            contentValuesInsert.put(UserInfoDatabaseHelper.ColumnsUserInfoSync.C_USER_ID, userId);
            contentValuesInsert.put(UserInfoDatabaseHelper.ColumnsUserInfoSync.C_LOCAL_LAST_SYNC_TIME_MS, System.currentTimeMillis());

            long rowId = db.insertWithOnConflict(
                    UserInfoDatabaseHelper.TABLE_NAME_USER_INFO_SYNC,
                    null,
                    contentValuesInsert,
                    SQLiteDatabase.CONFLICT_IGNORE
            );
            IMLog.v("touchUserInfoSync for user id:%s rowId:%s", userId, rowId);
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
    public boolean updateUserInfoSync(final UserInfoSync userInfoSync) {
        if (userInfoSync == null) {
            Throwable e = new IllegalArgumentException("updateUserInfoSync userInfoSync is null");
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
            return false;
        }

        if (userInfoSync.uid.isUnset()) {
            final Throwable e = new IllegalArgumentException("updateUserInfoSync invalid user id, unset");
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
            return false;
        }

        if (userInfoSync.uid.get() == null || userInfoSync.uid.get() <= 0) {
            final Throwable e = new IllegalArgumentException("updateUserInfoSync invalid user userId " + userInfoSync.uid);
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
            return false;
        }

        try {
            IMLog.v("updateUserInfoSync %s", userInfoSync);
            SQLiteDatabase db = mDBHelper.getDBHelper().getWritableDatabase();
            long rowsAffected = db.update(
                    UserInfoDatabaseHelper.TABLE_NAME_USER_INFO_SYNC,
                    userInfoSync.toContentValues(),
                    UserInfoDatabaseHelper.ColumnsUserInfoSync.C_USER_ID + "=?",
                    new String[]{String.valueOf(userInfoSync.uid.get())}
            );

            if (rowsAffected != 1) {
                Throwable e = new IllegalAccessException("updateUserInfoSync for user id:" + userInfoSync.uid.get() + " rowsAffected " + rowsAffected);
                IMLog.e(e);
                RuntimeMode.throwIfDebug(e);
                return false;
            }

            IMLog.v("updateUserInfoSync for user id:%s rowsAffected:%s", userInfoSync.uid.get(), rowsAffected);
            return true;
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        }
        return false;
    }
}
