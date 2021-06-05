package com.masonsoft.imsdk.sample.app.conversation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.Lists;
import com.masonsoft.imsdk.MSIMConversation;
import com.masonsoft.imsdk.lang.GeneralResult;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.SystemInsetsFragment;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleConversationFragmentBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeMapperImpl;
import com.masonsoft.imsdk.sample.widget.DividerItemDecoration;
import com.masonsoft.imsdk.util.Objects;

import java.util.List;

import io.github.idonans.core.thread.Threads;
import io.github.idonans.core.util.DimenUtil;
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

        recyclerView.addItemDecoration(new DividerItemDecoration(
                DividerItemDecoration.VERTICAL,
                DividerItemDecoration.SHOW_DIVIDER_MIDDLE,
                0xFFe1e1e1,
                DimenUtil.dp2px(1),
                DimenUtil.dp2px(1)
        ));

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

    class ViewImpl extends UnionTypeStatusPageView<GeneralResult> {

        public ViewImpl(@NonNull UnionTypeAdapter adapter) {
            super(adapter);
            setAlwaysHideNoMoreData(true);
        }

        public void replaceConversation(@NonNull final UnionTypeItemObject unionTypeItemObject) {
            final String tag = Objects.defaultObjectTag(this);
            SampleLog.v(tag + " replaceConversation " + Objects.defaultObjectTag(unionTypeItemObject));
            getAdapter().getData().beginTransaction()
                    .add((transaction, groupArrayList) -> {
                        if (groupArrayList.getGroupItemsSize(getGroupContent()) == 0) {
                            // request init page
                            Threads.postUi(() -> {
                                if (mPresenter != null) {
                                    SampleLog.v(tag + " page content is empty, use requestInit instead of replace");
                                    if (!mPresenter.getInitRequestStatus().isLoading()) {
                                        mPresenter.requestInit(true);
                                    }
                                }
                            });
                            return;
                        }

                        // replace
                        final MSIMConversation updateConversation = (MSIMConversation) ((DataObject<?>) unionTypeItemObject.itemObject).object;
                        final boolean delete = updateConversation.isDelete();
                        final List<UnionTypeItemObject> groupDefaultList = groupArrayList.getGroupItems(getGroupContent());
                        int removedPosition = -1;
                        int insertPosition = -1;
                        if (groupDefaultList != null) {
                            final int size = groupDefaultList.size();
                            for (int i = 0; i < size; i++) {
                                final UnionTypeItemObject existsOne = groupDefaultList.get(i);
                                if (existsOne.isSameItem(unionTypeItemObject)) {
                                    removedPosition = i;
                                }

                                if (!delete) {
                                    final MSIMConversation existsConversation = (MSIMConversation) ((DataObject<?>) existsOne.itemObject).object;
                                    if (updateConversation.getSeq() > existsConversation.getSeq() && insertPosition == -1) {
                                        insertPosition = i;
                                    }

                                    if (removedPosition >= 0 && insertPosition >= 0) {
                                        if (removedPosition < insertPosition) {
                                            insertPosition--;
                                        }
                                        break;
                                    }
                                } else {
                                    if (removedPosition >= 0) {
                                        break;
                                    }
                                }
                            }
                        }
                        if (removedPosition >= 0 && removedPosition == insertPosition) {
                            SampleLog.v(Objects.defaultObjectTag(this) + " ignore. replaceConversation removedPosition:%s, insertPosition:%s %s", removedPosition, insertPosition, updateConversation);
                            return;
                        }

                        SampleLog.v(Objects.defaultObjectTag(this) + " replaceConversation removedPosition:%s, insertPosition:%s %s", removedPosition, insertPosition, updateConversation);

                        if (removedPosition >= 0) {
                            groupArrayList.removeGroupItem(getGroupContent(), removedPosition);
                        }

                        if (delete) {
                            return;
                        }
                        if (insertPosition >= 0) {
                            groupArrayList.insertGroupItems(getGroupContent(), insertPosition, Lists.newArrayList(unionTypeItemObject));
                        } else {
                            groupArrayList.appendGroupItems(getGroupContent(), Lists.newArrayList(unionTypeItemObject));
                        }
                    })
                    .commit();
        }
    }

}
