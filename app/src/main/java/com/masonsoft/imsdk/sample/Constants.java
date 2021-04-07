package com.masonsoft.imsdk.sample;

import com.idonans.core.util.HumanUtil;

import java.util.concurrent.TimeUnit;

public class Constants {

    public interface ExtrasKey {
        String TARGET_USER_ID = "extra:targetUserId";
    }

    public interface ErrorLog {
        String FRAGMENT_MANAGER_STATE_SAVED = "fragment manager is state saved";
        String ACTIVITY_NOT_FOUND_IN_FRAGMENT = "activity not found in fragment";
        String ACTIVITY_IS_FINISHING = "activity is finishing";
        String BINDING_IS_NULL = "binding is null";
        String ACTIVITY_IS_NULL = "activity is null";
        String FRAGMENT_IS_NULL = "fragment is null";
        String EDITABLE_IS_NULL = "editable is null";
        String SOFT_KEYBOARD_HELPER_IS_NULL = "soft keyboard helper is null";
        String ACTIVITY_IS_NOT_APP_COMPAT_ACTIVITY = "activity is not AppCompatActivity";
        String PERMISSION_REQUIRED = "permission required";
    }

    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    /**
     * 图片选择中允许的最大图片尺寸(width*height*4)
     */
    public static final long SELECTOR_MAX_IMAGE_SIZE = 50 * HumanUtil.MB;
    /**
     * 图片选择中允许的最大图片文件大小
     */
    public static final long SELECTOR_MAX_IMAGE_FILE_SIZE = 10 * HumanUtil.MB;
    /**
     * 视频选择中允许的最大视频文件大小(file.length)
     */
    public static final long SELECTOR_MAX_VIDEO_SIZE = 200 * HumanUtil.MB;
    /**
     * 视频选择中允许的最长时长 ms (video.duration)
     */
    public static final long SELECTOR_MAX_VIDEO_DURATION = TimeUnit.SECONDS.toMillis(30);
    /**
     * 视频选择中允许的最短时长 ms (video.duration)
     */
    public static final long SELECTOR_MIN_VIDEO_DURATION = TimeUnit.SECONDS.toMillis(1);
    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
}
