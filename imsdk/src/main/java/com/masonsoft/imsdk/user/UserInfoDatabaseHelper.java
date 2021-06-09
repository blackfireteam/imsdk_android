package com.masonsoft.imsdk.user;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMProcessValidator;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.manager.ProcessManager;
import io.github.idonans.core.util.ContextUtil;

/**
 * @since 1.0
 */
public class UserInfoDatabaseHelper {

    public static final Singleton<UserInfoDatabaseHelper> INSTANCE = new Singleton<UserInfoDatabaseHelper>() {
        @Override
        protected UserInfoDatabaseHelper create() {
            return new UserInfoDatabaseHelper();
        }
    };

    public static UserInfoDatabaseHelper getInstance() {
        IMProcessValidator.validateProcess();

        return INSTANCE.get();
    }

    // 当前数据库最新版本号
    private static final int DB_VERSION = 1;
    // 用户表
    public static final String TABLE_NAME_USER_INFO = "t_user_info";
    // 用户同步信息表
    public static final String TABLE_NAME_USER_INFO_SYNC = "t_user_info_sync";

    private final SQLiteOpenHelper mDBHelper;

    /**
     * 用户表
     */
    public interface ColumnsUserInfo {

        /**
         * 用户 id, 全局唯一
         *
         * @since db version 1
         */
        String C_USER_ID = "c_user_id";

        /**
         * 本地最后修改时间, 毫秒
         */
        String C_LOCAL_LAST_MODIFY_MS = "c_local_last_modify_ms";

        /**
         * 用户数据的 json 序列化
         *
         * @since db version 1
         */
        String C_USER_JSON = "c_user_json";
    }

    /**
     * 用户同步信息表
     */
    public interface ColumnsUserInfoSync {
        /**
         * 用户 id, 全局唯一
         *
         * @since db version 1
         */
        String C_USER_ID = "c_user_id";
        /**
         * 上一次同步的时间
         */
        String C_LOCAL_LAST_SYNC_TIME_MS = "c_local_last_sync_time_ms";
    }

    private UserInfoDatabaseHelper() {
        final String dbName = IMConstants.GLOBAL_NAMESPACE + "_user_info_20210401_" + ProcessManager.getInstance().getProcessTag();
        mDBHelper = new SQLiteOpenHelper(ContextUtil.getContext(), dbName, null, DB_VERSION) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL(getSQLCreateTableUserInfo());
                for (String sqlIndex : getSQLIndexTableUserInfo()) {
                    db.execSQL(sqlIndex);
                }

                db.execSQL(getSQLCreateTableUserInfoSync());
                for (String sqlIndex : getSQLIndexTableUserInfoSync()) {
                    db.execSQL(sqlIndex);
                }
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                throw new IllegalStateException("need config database upgrade from " + oldVersion + " to " + newVersion);
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
    private String getSQLCreateTableUserInfo() {
        return "create table " + TABLE_NAME_USER_INFO + " (" +
                ColumnsUserInfo.C_USER_ID + " integer primary key," +
                ColumnsUserInfo.C_LOCAL_LAST_MODIFY_MS + " integer not null," +
                ColumnsUserInfo.C_USER_JSON + " text" +
                ")";
    }

    /**
     * 用户表创建索引语句(数据库最新版本)
     */
    @NonNull
    private String[] getSQLIndexTableUserInfo() {
        return new String[]{
                "create index " + TABLE_NAME_USER_INFO + "_index_local_last_modify_ms on " + TABLE_NAME_USER_INFO + "(" + ColumnsUserInfo.C_LOCAL_LAST_MODIFY_MS + ")",
        };
    }

    /**
     * 用户同步信息表创建语句(数据库最新版本)
     */
    @NonNull
    private String getSQLCreateTableUserInfoSync() {
        return "create table " + TABLE_NAME_USER_INFO_SYNC + " (" +
                ColumnsUserInfoSync.C_USER_ID + " integer primary key," +
                ColumnsUserInfoSync.C_LOCAL_LAST_SYNC_TIME_MS + " integer not null" +
                ")";
    }

    /**
     * 用户同步信息表创建索引语句(数据库最新版本)
     */
    @NonNull
    private String[] getSQLIndexTableUserInfoSync() {
        return new String[]{
                "create index " + TABLE_NAME_USER_INFO_SYNC + "_index_local_last_sync_time_ms on " + TABLE_NAME_USER_INFO_SYNC + "(" + ColumnsUserInfoSync.C_LOCAL_LAST_SYNC_TIME_MS + ")",
        };
    }

}
