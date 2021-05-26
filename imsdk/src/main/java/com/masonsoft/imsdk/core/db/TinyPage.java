package com.masonsoft.imsdk.core.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.core.util.Predicate;

import com.masonsoft.imsdk.lang.GeneralResult;
import com.masonsoft.imsdk.util.Objects;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 1.0
 */
public class TinyPage<T> {

    public List<T> items;
    public boolean hasMore;

    @Nullable
    public GeneralResult generalResult;

    @NonNull
    public String toShortString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(Objects.defaultObjectTag(this));
        if (items == null) {
            builder.append(" items:null");
        } else {
            builder.append(" items size:").append(items.size());
        }
        builder.append(" hasMore:").append(hasMore);
        if (this.generalResult == null) {
            builder.append(" generalResult:null");
        } else {
            builder.append(" generalResult:").append(this.generalResult.toShortString());
        }
        return builder.toString();
    }

    @Override
    @NonNull
    public String toString() {
        return this.toShortString();
    }

    @NonNull
    public <P> TinyPage<P> transform(Function<T, P> f) {
        final TinyPage<P> target = new TinyPage<>();
        target.items = new ArrayList<>();
        target.hasMore = this.hasMore;
        target.generalResult = this.generalResult;

        if (this.items != null) {
            for (T item : this.items) {
                target.items.add(f.apply(item));
            }
        }

        return target;
    }

    @NonNull
    public TinyPage<T> filter(Predicate<T> p) {
        final TinyPage<T> target = new TinyPage<>();
        target.items = new ArrayList<>();
        target.hasMore = this.hasMore;
        target.generalResult = this.generalResult;

        if (this.items != null) {
            for (T item : this.items) {
                if (p.test(item)) {
                    target.items.add(item);
                }
            }
        }

        return target;
    }

}
