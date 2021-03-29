package com.masonsoft.imsdk.core.db;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * @since 1.0
 */
public class TinyPage<T> {

    public List<T> items;
    public boolean hasMore;

    @NonNull
    public String toShortString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("TinyPage");
        if (items == null) {
            builder.append(" items:null");
        } else {
            builder.append(" items size:").append(items.size());
        }
        builder.append(" hasMore:").append(hasMore);
        return builder.toString();
    }

    @Override
    @NonNull
    public String toString() {
        return this.toShortString();
    }

}
