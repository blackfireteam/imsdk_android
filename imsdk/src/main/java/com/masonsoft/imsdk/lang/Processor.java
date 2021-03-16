package com.masonsoft.imsdk.lang;

import androidx.annotation.Nullable;

/**
 * @since 1.0
 */
public interface Processor<T> {

    /**
     * 处理 target 内容，如果已经消费了 target 内容，返回 true, 否则返回 false.
     */
    boolean doProcess(@Nullable T target);

}
