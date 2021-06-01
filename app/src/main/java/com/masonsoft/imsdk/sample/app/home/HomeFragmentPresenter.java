package com.masonsoft.imsdk.sample.app.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.sample.api.DefaultApi;
import com.masonsoft.imsdk.sample.entity.Spark;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeMapperImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.idonans.core.thread.Threads;
import io.github.idonans.dynamic.page.PagePresenter;
import io.github.idonans.dynamic.page.PageView;
import io.github.idonans.dynamic.page.UnionTypeStatusPageView;
import io.github.idonans.uniontype.UnionTypeItemObject;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleSource;

public class HomeFragmentPresenter extends PagePresenter<UnionTypeItemObject, UnionTypeStatusPageView> {

    public HomeFragmentPresenter(HomeFragment.ViewImpl view) {
        super(view, false, true);
    }

    @Nullable
    @Override
    public HomeFragment.ViewImpl getView() {
        return (HomeFragment.ViewImpl) super.getView();
    }

    @Nullable
    @Override
    protected SingleSource<Collection<UnionTypeItemObject>> createInitRequest() throws Exception {
        return Single.just("")
                .map(input -> DefaultApi.getSparks())
                .map(this::create)
                .delay(2, TimeUnit.SECONDS);
    }

    @Override
    protected void onInitRequestResult(@NonNull PageView<UnionTypeItemObject> view, @NonNull Collection<UnionTypeItemObject> items) {
        super.onInitRequestResult(view, items);

        if (items.isEmpty()) {
            setLastRetryListener(() -> requestInit(true));
        } else {
            setLastRetryListener(null);
        }
    }

    @Override
    protected void onInitRequestError(@NonNull PageView<UnionTypeItemObject> view, @NonNull Throwable e) {
        super.onInitRequestError(view, e);

        setLastRetryListener(() -> requestInit(true));
    }

    @Nullable
    @Override
    protected SingleSource<Collection<UnionTypeItemObject>> createPrePageRequest() throws Exception {
        // ignore
        return null;
    }

    @Nullable
    @Override
    protected SingleSource<Collection<UnionTypeItemObject>> createNextPageRequest() throws Exception {
        return Single.just("")
                .map(input -> DefaultApi.getSparks())
                .map(this::create);
    }

    @Override
    protected void onNextPageRequestResult(@NonNull PageView<UnionTypeItemObject> view, @NonNull Collection<UnionTypeItemObject> items) {
        super.onNextPageRequestResult(view, items);

        if (items.isEmpty()) {
            setLastRetryListener(() -> requestNextPage(true));
        } else {
            setLastRetryListener(null);
        }
    }

    @Override
    protected void onNextPageRequestError(@NonNull PageView<UnionTypeItemObject> view, @NonNull Throwable e) {
        super.onNextPageRequestError(view, e);

        setLastRetryListener(() -> requestNextPage(true));
    }

    private Collection<UnionTypeItemObject> create(Collection<Spark> input) {
        List<UnionTypeItemObject> result = new ArrayList<>();
        if (input != null) {
            for (Spark item : input) {
                if (item != null) {
                    result.add(create(item));
                }
            }
        }
        return result;
    }

    private UnionTypeItemObject create(@NonNull Spark input) {
        return UnionTypeItemObject.valueOf(UnionTypeMapperImpl.UNION_TYPE_IMPL_IM_HOME_SPARK, new DataObject<>(input));
    }

    private void setLastRetryListener(LastRetryListener listener) {
        Threads.runOnUi(() -> mLastRetryListener = listener);
    }

    private LastRetryListener mLastRetryListener;

    private interface LastRetryListener {
        void onRetry();
    }

    public void requestLastRetry() {
        if (mLastRetryListener != null) {
            mLastRetryListener.onRetry();
        }
    }

}
