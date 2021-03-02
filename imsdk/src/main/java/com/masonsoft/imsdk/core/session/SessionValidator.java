package com.masonsoft.imsdk.core.session;

import androidx.annotation.Nullable;

import com.masonsoft.imsdk.MSIMManager;

/**
 * 验证当前 Session 的有效性
 */
public class SessionValidator {

    private SessionValidator() {
    }

    /**
     * 校验目标 Session 是否有效，如果有效返回 true, 否则返回 false.
     */
    public static boolean isValid(@Nullable Session session) {
        if (session == null) {
            return false;
        }

        return MSIMManager.getInstance().getSessionManager().getSession() == session;
    }

}
