package com.masonsoft.imsdk.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @since 1.0
 */
public class TimeUtil {

    private TimeUtil() {
    }

    public static String msToHumanString(long timeMs) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.CHINA).format(new Date(timeMs));
    }

}
