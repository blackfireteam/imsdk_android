package com.masonsoft.imsdk.sample.app.discover;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.sample.im.DiscoverUserManager;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeMapperImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.github.idonans.dynamic.page.PagePresenter;
import io.github.idonans.dynamic.page.UnionTypeStatusPageView;
import io.github.idonans.uniontype.UnionTypeItemObject;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleSource;

public class DiscoverFragmentPresenter extends PagePresenter<UnionTypeItemObject, UnionTypeStatusPageView> {

    public DiscoverFragmentPresenter(DiscoverFragment.ViewImpl view) {
        super(view, false, false);
    }

    @Nullable
    @Override
    public DiscoverFragment.ViewImpl getView() {
        return (DiscoverFragment.ViewImpl) super.getView();
    }

    @Nullable
    @Override
    protected SingleSource<Collection<UnionTypeItemObject>> createInitRequest() throws Exception {
        final List<Long> userIdList = DiscoverUserManager.getInstance().getOnlineUserList();
        return Single.just(create(userIdList));
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
        // ignore
        return null;
    }

    private Collection<UnionTypeItemObject> create(Collection<Long> input) {
        List<UnionTypeItemObject> result = new ArrayList<>();
        if (input != null) {
            for (Long item : input) {
                if (item != null) {
                    result.add(create(item));
                }
            }
        }
        return result;
    }

    private UnionTypeItemObject create(@NonNull Long userId) {
        return UnionTypeItemObject.valueOf(UnionTypeMapperImpl.UNION_TYPE_IMPL_IM_DISCOVER_USER, new DataObject<>(userId));
    }

}
