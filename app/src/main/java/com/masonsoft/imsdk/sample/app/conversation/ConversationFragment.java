package com.masonsoft.imsdk.sample.app.conversation;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.Lists;
import com.masonsoft.imsdk.IMConversation;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.SystemInsetsFragment;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleConversationFragmentBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeMapperImpl;
import com.masonsoft.imsdk.util.Objects;

import java.util.Collection;
import java.util.List;

import io.github.idonans.dynamic.page.UnionTypeStatusPageView;
import io.github.idonans.uniontype.Host;
import io.github.idonans.uniontype.UnionTypeAdapter;
import io.github.idonans.uniontype.UnionTypeItemObject;

/**
 * 会话
 */
public class ConversationFragment extends SystemInsetsFragment {

    public static ConversationFragment newInstance() {
        Bundle args = new Bundle();
        ConversationFragment fragment = new ConversationFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    private ImsdkSampleConversationFragmentBinding mBinding;

    private UnionTypeAdapter mDataAdapter;
    private ConversationFragmentPresenter mPresenter;
    private ViewImpl mViewImpl;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        SampleLog.v("onCreateView %s", getClass());
        mBinding = ImsdkSampleConversationFragmentBinding.inflate(inflater, container, false);

        final RecyclerView recyclerView = mBinding.recyclerView;
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                recyclerView.getContext(),
                RecyclerView.VERTICAL,
                false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);
        recyclerView.setHasFixedSize(true);

        UnionTypeAdapter adapter = new UnionTypeAdapter();
        adapter.setHost(Host.Factory.create(this, recyclerView, adapter));
        adapter.setUnionTypeMapper(new UnionTypeMapperImpl());
        mDataAdapter = adapter;
        mViewImpl = new ViewImpl(adapter);
        clearPresenter();
        mPresenter = new ConversationFragmentPresenter(mViewImpl);
        mViewImpl.setPresenter(mPresenter);
        recyclerView.setAdapter(adapter);

        mPresenter.requestInit();

        return mBinding.getRoot();
    }

    private void clearPresenter() {
        if (mPresenter != null) {
            mPresenter.setAbort();
            mPresenter = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        clearPresenter();
        mBinding = null;
        mViewImpl = null;
    }

    class ViewImpl extends UnionTypeStatusPageView {

        public ViewImpl(@NonNull UnionTypeAdapter adapter) {
            super(adapter);
            setAlwaysHideNoMoreData(true);
        }

        @Override
        public void onInitDataEmpty() {
            SampleLog.v(Objects.defaultObjectTag(this) + " onInitDataEmpty");
            super.onInitDataEmpty();
        }

        @Override
        public void onInitDataLoad(@NonNull Collection<UnionTypeItemObject> items) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onInitDataLoad items size:" + items.size());
            super.onInitDataLoad(items);
        }

        @Override
        public void onPrePageDataLoad(@NonNull Collection<UnionTypeItemObject> items) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onPrePageDataLoad items size:" + items.size());
            super.onPrePageDataLoad(items);
        }

        @Override
        public void onNextPageDataLoad(@NonNull Collection<UnionTypeItemObject> items) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onNextPageDataLoad items size:" + items.size());
            super.onNextPageDataLoad(items);
        }

        // TODO
        public Activity getActivity() {
            return ConversationFragment.this.getActivity();
        }

        public void replaceConversation(@NonNull final UnionTypeItemObject unionTypeItemObject) {
            if (!hasPageContent()) {
                SampleLog.v(Objects.defaultObjectTag(this) + " page content is empty, use requestInit instead of replace");
                mPresenter.requestInit();
                return;
            }

            final IMConversation updateConversation = (IMConversation) ((DataObject<?>) unionTypeItemObject.itemObject).object;
            final List<UnionTypeItemObject> groupDefaultList = getAdapter().getData().getGroupItems(GROUP_DEFAULT);
            int removedPosition = -1;
            int insertPosition = -1;
            if (groupDefaultList != null) {
                final int size = groupDefaultList.size();
                for (int i = 0; i < size; i++) {
                    final UnionTypeItemObject existsOne = groupDefaultList.get(i);
                    if (existsOne.isSameItem(unionTypeItemObject)) {
                        removedPosition = i;
                    }
                    final IMConversation existsConversation = (IMConversation) ((DataObject<?>) existsOne.itemObject).object;
                    if (updateConversation.seq.get() > existsConversation.seq.get() && insertPosition == -1) {
                        insertPosition = i;
                    }

                    if (removedPosition >= 0 && insertPosition >= 0) {
                        break;
                    }
                }
            }
            if (removedPosition >= 0 && removedPosition == insertPosition) {
                SampleLog.v(Objects.defaultObjectTag(this) + " ignore. replaceConversation removedPosition:%s, insertPosition:%s", removedPosition, insertPosition);
                return;
            }

            SampleLog.v(Objects.defaultObjectTag(this) + " replaceConversation removedPosition:%s, insertPosition:%s", removedPosition, insertPosition);

            if (removedPosition >= 0) {
                if (!getAdapter().removeGroupItem(GROUP_DEFAULT, removedPosition)) {
                    final Throwable e = new IllegalAccessError("fail to removeGroupItem GROUP_DEFAULT removedPosition:" + removedPosition);
                    SampleLog.e(e);
                }
            }
            if (insertPosition == -1) {
                insertPosition = 0;
            }
            if (!getAdapter().insertGroupItems(GROUP_DEFAULT, insertPosition, Lists.newArrayList(unionTypeItemObject))) {
                final Throwable e = new IllegalAccessError("fail to insertGroupItems GROUP_DEFAULT insertPosition:" + insertPosition);
                SampleLog.e(e);
            }
        }
    }

}
