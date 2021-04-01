package com.masonsoft.imsdk.core.observable;

import com.masonsoft.imsdk.user.UserInfoManager;
import com.masonsoft.imsdk.util.WeakObservable;

/**
 * 用户信息的变更
 *
 * @see UserInfoManager
 * @since 1.0
 */
public class UserInfoObservable extends WeakObservable<UserInfoObservable.UserInfoObserver> {

    public static final UserInfoObservable DEFAULT = new UserInfoObservable();

    public interface UserInfoObserver {
        void onUserInfoChanged(long userId);
    }

    public void notifyUserInfoChanged(long userId) {
        forEach(userInfoObserver -> userInfoObserver.onUserInfoChanged(userId));
    }

}