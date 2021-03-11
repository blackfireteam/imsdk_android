package com.masonsoft.imsdk.db;

import android.database.Cursor;

import androidx.annotation.NonNull;

public interface ColumnsSelector<T> {

    /**
     * 指定需要查询的列
     */
    @NonNull
    String[] queryColumns();

    /**
     * 将指定查询的列转换为对象
     */
    @NonNull
    T cursorToObjectWithQueryColumns(@NonNull Cursor cursor);

}
