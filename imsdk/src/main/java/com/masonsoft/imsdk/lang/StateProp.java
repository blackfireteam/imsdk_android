package com.masonsoft.imsdk.lang;

import androidx.annotation.NonNull;

/**
 * 带状态的对象属性定义。指定判断属性是否已经设置值。
 *
 * @since 1.0
 */
public class StateProp<T> {

    private static final Object UNSET = new Object();
    private Object mProp = UNSET;

    public boolean isUnset() {
        return mProp == UNSET;
    }

    /**
     * 获取值，如果值没有设置过，将抛出异常。
     *
     * @see #isUnset()
     */
    public T get() {
        if (mProp == UNSET) {
            throw new IllegalArgumentException("current prop is unset");
        }

        //noinspection unchecked
        return (T) mProp;
    }

    /**
     * 获取值，如果值没有设置过，返回默认值
     *
     * @param defaultValue 当值未设置时，返回该默认值
     * @see #isUnset()
     */
    public T getOrDefault(T defaultValue) {
        if (mProp == UNSET) {
            return defaultValue;
        }
        //noinspection unchecked
        return (T) mProp;
    }

    public void set(T value) {
        mProp = value;
    }

    /**
     * 将值设置为 UNSET
     *
     * @see #isUnset()
     */
    public void clear() {
        mProp = UNSET;
    }

    /**
     * 复制指定 StateProp 的值
     */
    public void apply(@NonNull StateProp<T> origin) {
        this.mProp = origin.mProp;
    }

    @NonNull
    public String toShortString() {
        if (mProp == UNSET) {
            return "StateProp:unset";
        }
        return "StateProp:" + mProp;
    }

    @Override
    @NonNull
    public String toString() {
        return toShortString();
    }

}
