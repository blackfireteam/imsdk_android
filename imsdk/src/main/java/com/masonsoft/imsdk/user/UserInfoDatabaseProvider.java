package com.masonsoft.imsdk.user;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMProcessValidator;
import com.masonsoft.imsdk.core.RuntimeMode;
import com.masonsoft.imsdk.core.db.ColumnsSelector;

import java.util.ArrayList;
import java.util.List;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.util.IOUtil;

/**
 * @since 1.0
 */
public class UserInfoDatabaseProvider {

    public static final Singleton<UserInfoDatabaseProvider> INSTANCE = new Singleton<UserInfoDatabaseProvider>() {
        @Override
        protected UserInfoDatabaseProvider create() {
            return new UserInfoDatabaseProvider();
        }
    };

    public static UserInfoDatabaseProvider getInstance() {
        IMProcessValidator.validateProcess();

        return INSTANCE.get();
    }

    private final UserInfoDatabaseHelper mDBHelper;

    private UserInfoDatabaseProvider() {
        mDBHelper = UserInfoDatabaseHelper.getInstance();
    }

    @NonNull
    public List<UserInfo> getUserInfoByUserIdList(final List<Long> userIdList) {
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
                    UserInfoDatabaseHelper.TABLE_NAME_USER_INFO,
                    columnsSelector.queryColumns(),
                    UserInfoDatabaseHelper.ColumnsUserInfo.C_USER_ID + " in " + inBuilder,
                    null,
                    null,
                    null,
                    null
            );

            if (cursor.moveToNext()) {
                UserInfo item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                IMLog.v("getUserInfoByUserIdList found userInfo with user id:%s", item.uid);
                result.add(item);
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.fixme(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }
        return result;
    }

    /**
     * @param userId
     * @return 没有找到返回 null
     */
    @Nullable
    public UserInfo getUserInfoByUserId(final long userId) {
        final ColumnsSelector<UserInfo> columnsSelector = UserInfo.COLUMNS_SELECTOR_ALL;

        Cursor cursor = null;
        try {
            SQLiteDatabase db = mDBHelper.getDBHelper().getWritableDatabase();
            cursor = db.query(
                    UserInfoDatabaseHelper.TABLE_NAME_USER_INFO,
                    columnsSelector.queryColumns(),
                    UserInfoDatabaseHelper.ColumnsUserInfo.C_USER_ID + "=?",
                    new String[]{String.valueOf(userId)},
                    null,
                    null,
                    null,
                    "0,1"
            );

            if (cursor.moveToNext()) {
                UserInfo item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                IMLog.v("getUserInfoByUserId found userInfo with user id:%s", item.uid);
                return item;
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.fixme(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        // user not found
        IMLog.v("getUserInfoByUserId for user id:%s not found", userId);
        return null;
    }

    /**
     * 成功返回 true, 否则返回 false.
     */
    public boolean insertUserInfo(final UserInfo user) {
        if (user == null) {
            Throwable e = new IllegalArgumentException("insertUserInfo user is null");
            IMLog.e(e);
            RuntimeMode.fixme(e);
            return false;
        }

        if (user.uid.isUnset()) {
            final Throwable e = new IllegalArgumentException("insertUserInfo invalid user id, unset");
            IMLog.e(e);
            RuntimeMode.fixme(e);
            return false;
        }

        if (user.uid.get() == null || user.uid.get() <= 0) {
            final Throwable e = new IllegalArgumentException("insertUserInfo invalid user userId " + user.uid);
            IMLog.e(e);
            RuntimeMode.fixme(e);
            return false;
        }

        // 设置 lastModify
        user.localLastModifyMs.set(System.currentTimeMillis());

        try {
            IMLog.v("insertUserInfo %s", user);
            SQLiteDatabase db = mDBHelper.getDBHelper().getWritableDatabase();
            long rowId = db.insert(
                    UserInfoDatabaseHelper.TABLE_NAME_USER_INFO,
                    null,
                    user.toContentValues()
            );

            if (rowId <= 0) {
                Throwable e = new IllegalStateException("insertUserInfo for user id:" + user.uid.get() + " return rowId " + rowId);
                IMLog.e(e);
                RuntimeMode.fixme(e);
            } else {
                IMLog.v("insertUserInfo for user id:%s return rowId:%s", user.uid.get(), rowId);
            }
            return rowId > 0;
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.fixme(e);
        }
        return false;
    }

    /**
     * 如果记录存在，则忽略。否则插入一条新纪录。如果成功插入一条新记录返回 true, 否则返回 false.
     *
     * @param userId
     * @return
     */
    public boolean touchUserInfo(final long userId) {
        if (userId <= 0) {
            final Throwable e = new IllegalArgumentException("touchUserInfo invalid user id " + userId);
            IMLog.e(e);
            RuntimeMode.fixme(e);
            return false;
        }

        try {
            IMLog.v("touchUserInfo userId:%s", userId);
            SQLiteDatabase db = mDBHelper.getDBHelper().getWritableDatabase();

            final ContentValues contentValuesInsert = new ContentValues();
            contentValuesInsert.put(UserInfoDatabaseHelper.ColumnsUserInfo.C_USER_ID, userId);
            contentValuesInsert.put(UserInfoDatabaseHelper.ColumnsUserInfo.C_LOCAL_LAST_MODIFY_MS, System.currentTimeMillis());

            long rowId = db.insertWithOnConflict(
                    UserInfoDatabaseHelper.TABLE_NAME_USER_INFO,
                    null,
                    contentValuesInsert,
                    SQLiteDatabase.CONFLICT_IGNORE
            );
            IMLog.v("touchUserInfo for user id:%s rowId:%s", userId, rowId);
            return rowId > 0;
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.fixme(e);
        }
        return false;
    }

    /**
     * 更新成功返回 true, 否则返回 false.
     */
    public boolean updateUserInfo(final UserInfo user) {
        if (user == null) {
            Throwable e = new IllegalArgumentException("updateUserInfo user is null");
            IMLog.e(e);
            RuntimeMode.fixme(e);
            return false;
        }

        if (user.uid.isUnset()) {
            final Throwable e = new IllegalArgumentException("updateUserInfo invalid user id, unset");
            IMLog.e(e);
            RuntimeMode.fixme(e);
            return false;
        }

        if (user.uid.get() == null || user.uid.get() <= 0) {
            final Throwable e = new IllegalArgumentException("updateUserInfo invalid user userId " + user.uid);
            IMLog.e(e);
            RuntimeMode.fixme(e);
            return false;
        }

        // 设置 lastModify
        user.localLastModifyMs.set(System.currentTimeMillis());

        try {
            IMLog.v("updateUserInfo %s", user);
            SQLiteDatabase db = mDBHelper.getDBHelper().getWritableDatabase();
            long rowsAffected = db.update(
                    UserInfoDatabaseHelper.TABLE_NAME_USER_INFO,
                    user.toContentValues(),
                    UserInfoDatabaseHelper.ColumnsUserInfo.C_USER_ID + "=?",
                    new String[]{String.valueOf(user.uid.get())}
            );

            if (rowsAffected != 1) {
                Throwable e = new IllegalStateException("updateUserInfo for user id:" + user.uid.get() + " rowsAffected " + rowsAffected);
                IMLog.e(e);
                RuntimeMode.fixme(e);
                return false;
            }

            IMLog.v("updateUserInfo for user id:%s rowsAffected:%s", user.uid.get(), rowsAffected);
            return true;
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.fixme(e);
        }
        return false;
    }
}
