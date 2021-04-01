package com.masonsoft.imsdk.user;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.core.Singleton;
import com.idonans.core.util.IOUtil;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMProcessValidator;
import com.masonsoft.imsdk.core.RuntimeMode;
import com.masonsoft.imsdk.core.db.ColumnsSelector;

import java.util.ArrayList;
import java.util.List;

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
                    UserInfoDatabaseHelper.TABLE_NAME_USER_INFO,
                    columnsSelector.queryColumns(),
                    UserInfoDatabaseHelper.ColumnsUserInfo.C_USER_ID + "=?",
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
                    UserInfoDatabaseHelper.TABLE_NAME_USER_INFO,
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
            contentValuesInsert.put(UserInfoDatabaseHelper.ColumnsUserInfo.C_USER_ID, userId);
            contentValuesInsert.put(UserInfoDatabaseHelper.ColumnsUserInfo.C_LOCAL_LAST_MODIFY_MS, System.currentTimeMillis());

            long rowId = db.insertWithOnConflict(
                    UserInfoDatabaseHelper.TABLE_NAME_USER_INFO,
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
                    UserInfoDatabaseHelper.TABLE_NAME_USER_INFO,
                    user.toContentValues(),
                    UserInfoDatabaseHelper.ColumnsUserInfo.C_USER_ID + "=?",
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
