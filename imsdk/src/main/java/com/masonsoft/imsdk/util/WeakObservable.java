package com.masonsoft.imsdk.util;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import java.util.List;

/**
 * @since 1.0
 */
public class WeakObservable<T> {

    private final ReadWriteWeakSet<T> mObservers = new ReadWriteWeakSet<>();

    public void registerObserver(T observer) {
        mObservers.put(observer);
    }

    public void unregisterObserver(T observer) {
        mObservers.remove(observer);
    }

    public void unregisterAll() {
        mObservers.removeAll();
    }

    protected void forEach(@NonNull Consumer<T> consumer) {
        final List<T> observers = mObservers.getAll();
        for (T observer : observers) {
            if (observer == null) {
                continue;
            }
            consumer.accept(observer);
        }
    }

}
