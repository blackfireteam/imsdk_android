package com.masonsoft.imsdk.user;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.db.ColumnsSelector;
import com.masonsoft.imsdk.lang.StateProp;
import com.masonsoft.imsdk.util.CursorUtil;
import com.masonsoft.imsdk.util.Objects;

/**
 * 用户同步信息
 *
 * @since 1.0
 */
public class UserInfoSync {

    /**
     * 用户 id, 全局唯一
     */
    @NonNull
    public final StateProp<Long> uid = new StateProp<>();

    /**
     * 上一次同步的发送时间, 毫秒
     */
    public final StateProp<Long> localLastSyncTimeMs = new StateProp<>();

    @NonNull
    public String toShortString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(Objects.defaultObjectTag(this));
        if (this.uid.isUnset()) {
            builder.append(" uid:unset");
        } else {
            builder.append(" uid:").append(this.uid.get());
        }
        if (this.localLastSyncTimeMs.isUnset()) {
            builder.append(" localLastSyncTimeMs:unset");
        } else {
            builder.append(" localLastSyncTimeMs:").append(this.localLastSyncTimeMs.get());
        }
        return builder.toString();
    }

    @Override
    @NonNull
    public String toString() {
        return toShortString();
    }

    public void apply(@NonNull UserInfoSync input) {
        this.uid.apply(input.uid);
        this.localLastSyncTimeMs.apply(input.localLastSyncTimeMs);
    }

    @NonNull
    public ContentValues toContentValues() {
        final ContentValues target = new ContentValues();
        if (!this.uid.isUnset()) {
            target.put(UserInfoDatabaseHelper.ColumnsUserInfoSync.C_USER_ID, this.uid.get());
        }
        if (!this.localLastSyncTimeMs.isUnset()) {
            target.put(UserInfoDatabaseHelper.ColumnsUserInfoSync.C_LOCAL_LAST_SYNC_TIME_MS, this.localLastSyncTimeMs.get());
        }
        return target;
    }

    /**
     * 查询所有字段
     */
    public static final ColumnsSelector<UserInfoSync> COLUMNS_SELECTOR_ALL = new ColumnsSelector<UserInfoSync>() {

        @NonNull
        @Override
        public String[] queryColumns() {
            return new String[]{
                    UserInfoDatabaseHelper.ColumnsUserInfoSync.C_USER_ID,
                    UserInfoDatabaseHelper.ColumnsUserInfoSync.C_LOCAL_LAST_SYNC_TIME_MS,
            };
        }

        @NonNull
        @Override
        public UserInfoSync cursorToObjectWithQueryColumns(@NonNull Cursor cursor) {
            final UserInfoSync target = new UserInfoSync();
            int index = -1;
            target.uid.set(CursorUtil.getLong(cursor, ++index));
            target.localLastSyncTimeMs.set(CursorUtil.getLong(cursor, ++index));
            return target;
        }
    };

}
