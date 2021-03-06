package com.masonsoft.imsdk.core;

import androidx.annotation.Nullable;

import com.idonans.core.manager.StorageManager;

/**
 * 简单数据的 key-value 加密存储
 */
public class KeyValueStorage {

    private static final String NAMESPACE = "imsdk_key_value_storage_20210306";

    private KeyValueStorage() {
    }

    public static void set(@Nullable String key, @Nullable String value) {
        StorageManager.getInstance().set(NAMESPACE, key, value);
    }

    public static String get(@Nullable String key) {
        return StorageManager.getInstance().get(NAMESPACE, key);
    }

    public static String getOrSet(@Nullable String key, @Nullable String setValue) {
        return StorageManager.getInstance().getOrSetLock(NAMESPACE, key, setValue);
    }

}
