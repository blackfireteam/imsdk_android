package com.masonsoft.imsdk.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @since 1.0
 */
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

    public void remove(@Nullable T object) {
        mWriteLock.lock();
        try {
            mWeakMap.remove(object);
        } finally {
            mWriteLock.unlock();
        }
    }

    public void removeAll() {
        mWriteLock.lock();
        try {
            mWeakMap.clear();
        } finally {
            mWriteLock.unlock();
        }
    }

    @NonNull
    public List<T> getAll() {
        mReadLock.lock();
        try {
            mWeakMap.size();
            return new ArrayList<>(mWeakMap.keySet());
        } finally {
            mReadLock.unlock();
        }
    }

}
