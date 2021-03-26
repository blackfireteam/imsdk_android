package com.masonsoft.imsdk.user;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.RuntimeMode;
import com.masonsoft.imsdk.core.db.ColumnsSelector;
import com.masonsoft.imsdk.lang.StateProp;

import org.json.JSONObject;

public class UserInfo {

    /**
     * 用户 id, 全局唯一
     */
    @NonNull
    public final StateProp<Long> uid = new StateProp<>();

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

    @NonNull
    public final StateProp<Integer> gold = new StateProp<>();

    @NonNull
    public final StateProp<Integer> verified = new StateProp<>();

    @NonNull
    public String toShortString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("UserInfo");
        if (this.uid.isUnset()) {
            builder.append(" uid:unset");
        } else {
            builder.append(" uid:").append(this.uid.get());
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
        this.updateTimeMs.apply(input.updateTimeMs);
        this.nickname.apply(input.nickname);
        this.avatar.apply(input.avatar);
        this.gold.apply(input.gold);
        this.verified.apply(input.verified);
    }

    @NonNull
    public ContentValues toContentValues() {
        final ContentValues target = new ContentValues();
        if (!this.uid.isUnset()) {
            target.put(UserCacheManager.DatabaseProvider.DatabaseHelper.ColumnsUser.C_USER_ID, this.uid.get());
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
            if (!this.gold.isUnset()) {
                jsonObject.put("gold", this.gold.get());
            }
            if (!this.verified.isUnset()) {
                jsonObject.put("verified", this.verified.get());
            }
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.throwIfDebug(e);
        }
        target.put(UserCacheManager.DatabaseProvider.DatabaseHelper.ColumnsUser.C_USER_JSON, jsonObject.toString());
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
                    UserCacheManager.DatabaseProvider.DatabaseHelper.ColumnsUser.C_USER_ID,
                    UserCacheManager.DatabaseProvider.DatabaseHelper.ColumnsUser.C_USER_JSON,
            };
        }

        @NonNull
        @Override
        public UserInfo cursorToObjectWithQueryColumns(@NonNull Cursor cursor) {
            final UserInfo target = new UserInfo();
            int index = -1;
            target.uid.set(cursor.getLong(++index));
            final String userJson = cursor.getString(++index);
            if (!TextUtils.isEmpty(userJson)) {
                try {
                    final JSONObject jsonObject = new JSONObject(userJson);
                    target.updateTimeMs.set(jsonObject.optLong("updateTimeMs"));
                    target.nickname.set(jsonObject.optString("nickname"));
                    target.avatar.set(jsonObject.optString("avatar"));
                    target.gold.set(jsonObject.optInt("gold"));
                    target.verified.set(jsonObject.optInt("verified"));
                } catch (Throwable e) {
                    IMLog.e(e);
                    RuntimeMode.throwIfDebug(e);
                }
            }
            return target;
        }
    };

}
