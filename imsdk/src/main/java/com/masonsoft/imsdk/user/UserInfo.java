package com.masonsoft.imsdk.user;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.RuntimeMode;
import com.masonsoft.imsdk.core.db.ColumnsSelector;
import com.masonsoft.imsdk.lang.StateProp;
import com.masonsoft.imsdk.util.CursorUtil;

import org.json.JSONObject;

/**
 * 用户信息
 *
 * @since 1.0
 */
public class UserInfo {

    /**
     * 用户 id, 全局唯一
     */
    @NonNull
    public final StateProp<Long> uid = new StateProp<>();

    /**
     * 本地记录的 lastModify, 毫秒
     */
    public final StateProp<Long> localLastModifyMs = new StateProp<>();

    /**
     * 用户信息的最后更新时间, 毫秒。
     */
    @NonNull
    public final StateProp<Long> updateTimeMs = new StateProp<>();

    /**
     * 昵称
     */
    @NonNull
    public final StateProp<String> nickname = new StateProp<>();

    /**
     * 头像
     */
    @NonNull
    public final StateProp<String> avatar = new StateProp<>();

    /**
     * 性别
     */
    @NonNull
    public final StateProp<Integer> gender = new StateProp<>();

    /**
     * 第三方自定义数据
     */
    @NonNull
    public final StateProp<String> custom = new StateProp<>();

    @NonNull
    public String toShortString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("UserInfo");
        if (this.uid.isUnset()) {
            builder.append(" uid:unset");
        } else {
            builder.append(" uid:").append(this.uid.get());
        }
        if (this.localLastModifyMs.isUnset()) {
            builder.append(" localLastModifyMs:unset");
        } else {
            builder.append(" localLastModifyMs:").append(this.localLastModifyMs.get());
        }
        if (this.updateTimeMs.isUnset()) {
            builder.append(" updateTimeMs:unset");
        } else {
            builder.append(" updateTimeMs:").append(this.updateTimeMs.get());
        }
        return builder.toString();
    }

    @Override
    @NonNull
    public String toString() {
        return toShortString();
    }

    public void apply(@NonNull UserInfo input) {
        this.uid.apply(input.uid);
        this.localLastModifyMs.apply(input.localLastModifyMs);
        this.updateTimeMs.apply(input.updateTimeMs);
        this.nickname.apply(input.nickname);
        this.avatar.apply(input.avatar);
        this.gender.apply(input.gender);
        this.custom.apply(input.custom);
    }

    @NonNull
    public ContentValues toContentValues() {
        final ContentValues target = new ContentValues();
        if (!this.uid.isUnset()) {
            target.put(UserInfoDatabaseHelper.ColumnsUserInfo.C_USER_ID, this.uid.get());
        }
        if (!this.localLastModifyMs.isUnset()) {
            target.put(UserInfoDatabaseHelper.ColumnsUserInfo.C_LOCAL_LAST_MODIFY_MS, this.localLastModifyMs.get());
        }
        final JSONObject jsonObject = new JSONObject();
        try {
            if (!this.updateTimeMs.isUnset()) {
                jsonObject.put("updateTimeMs", this.updateTimeMs.get());
            }
            if (!this.nickname.isUnset()) {
                jsonObject.put("nickname", this.nickname.get());
            }
            if (!this.avatar.isUnset()) {
                jsonObject.put("avatar", this.avatar.get());
            }
            if (!this.gender.isUnset()) {
                jsonObject.put("gender", this.gender.get());
            }
            if (!this.custom.isUnset()) {
                jsonObject.put("custom", this.custom.get());
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.fixme(e);
        }
        target.put(UserInfoDatabaseHelper.ColumnsUserInfo.C_USER_JSON, jsonObject.toString());
        return target;
    }

    /**
     * 查询所有字段
     */
    public static final ColumnsSelector<UserInfo> COLUMNS_SELECTOR_ALL = new ColumnsSelector<UserInfo>() {

        @NonNull
        @Override
        public String[] queryColumns() {
            return new String[]{
                    UserInfoDatabaseHelper.ColumnsUserInfo.C_USER_ID,
                    UserInfoDatabaseHelper.ColumnsUserInfo.C_LOCAL_LAST_MODIFY_MS,
                    UserInfoDatabaseHelper.ColumnsUserInfo.C_USER_JSON,
            };
        }

        @NonNull
        @Override
        public UserInfo cursorToObjectWithQueryColumns(@NonNull Cursor cursor) {
            final UserInfo target = new UserInfo();
            int index = -1;
            target.uid.set(CursorUtil.getLong(cursor, ++index));
            target.localLastModifyMs.set(CursorUtil.getLong(cursor, ++index));
            final String userJson = CursorUtil.getString(cursor, ++index);
            if (!TextUtils.isEmpty(userJson)) {
                try {
                    final JSONObject jsonObject = new JSONObject(userJson);
                    if (jsonObject.has("updateTimeMs")) {
                        target.updateTimeMs.set(jsonObject.getLong("updateTimeMs"));
                    }
                    if (jsonObject.has("nickname")) {
                        target.nickname.set(jsonObject.getString("nickname"));
                    }
                    if (jsonObject.has("avatar")) {
                        target.avatar.set(jsonObject.getString("avatar"));
                    }
                    if (jsonObject.has("gender")) {
                        target.gender.set(jsonObject.getInt("gender"));
                    }
                    if (jsonObject.has("custom")) {
                        target.custom.set(jsonObject.getString("custom"));
                    }
                } catch (Throwable e) {
                    IMLog.e(e);
                    RuntimeMode.fixme(e);
                }
            }
            return target;
        }
    };

}
