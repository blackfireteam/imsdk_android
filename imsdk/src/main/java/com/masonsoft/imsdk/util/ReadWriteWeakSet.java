package com.masonsoft.imsdk.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteWeakSet<T> {

    private final WeakHashMap<T, Boolean> mWeakMap = new WeakHashMap<>();
    private final ReentrantReadWriteLock mLock = new ReentrantReadWriteLock();
    private final Lock mReadLock = mLock.readLock();
    private final Lock mWriteLock = mLock.writeLock();

    public void put(@Nullable T object) {
        mWriteLock.lock();
        try {
            mWeakMap.put(object, Boolean.TRUE);
        } finally {
            mWriteLock.unlock();
        }
    }

    @NonNull
    public List<T> get() {
        mReadLock.lock();
        try {
            mWeakMap.size();
            return new ArrayList<>(mWeakMap.keySet());
        } finally {
            mReadLock.unlock();
        }
    }

}
