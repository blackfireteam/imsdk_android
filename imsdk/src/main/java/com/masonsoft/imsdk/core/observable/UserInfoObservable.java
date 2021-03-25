package com.masonsoft.imsdk.core.observable;

import com.masonsoft.imsdk.util.WeakObservable;

/**
 * 用户信息的变更
 *
 * @see com.masonsoft.imsdk.user.UserCacheManager
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