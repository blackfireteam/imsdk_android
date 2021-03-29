package com.masonsoft.imsdk.util;

import android.database.Cursor;

import androidx.annotation.NonNull;

public class CursorUtil {

    private CursorUtil() {
    }

    public static String getString(@NonNull Cursor cursor, int index) {
        return getString(cursor, index, "");
    }

    public static String getString(@NonNull Cursor cursor, int index, String defaultValue) {
        if (cursor.isNull(index)) {
            return defaultValue;
        }
        return cursor.getString(index);
    }

    public static int getInt(@NonNull Cursor cursor, int index) {
        return getInt(cursor, index, 0);
    }

    public static int getInt(@NonNull Cursor cursor, int index, int defaultValue) {
        if (cursor.isNull(index)) {
            return defaultValue;
        }
        return cursor.getInt(index);
    }

    public static long getLong(@NonNull Cursor cursor, int index) {
        return getLong(cursor, index, 0L);
    }

    public static long getLong(@NonNull Cursor cursor, int index, long defaultValue) {
        if (cursor.isNull(index)) {
            return defaultValue;
        }
        return cursor.getLong(index);
    }

    public static float getFloat(@NonNull Cursor cursor, int index) {
        return getFloat(cursor, index, 0f);
    }

    public static float getFloat(@NonNull Cursor cursor, int index, float defaultValue) {
        if (cursor.isNull(index)) {
            return defaultValue;
        }
        return cursor.getFloat(index);
    }

    public static double getDouble(@NonNull Cursor cursor, int index) {
        return getDouble(cursor, index, 0d);
    }

    public static double getDouble(@NonNull Cursor cursor, int index, double defaultValue) {
        if (cursor.isNull(index)) {
            return defaultValue;
        }
        return cursor.getDouble(index);
    }

}
