package com.masonsoft.imsdk.sample.app.conversation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.masonsoft.imsdk.MSIMConversation;
import com.masonsoft.imsdk.lang.GeneralResult;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.SystemInsetsFragment;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleConversationFragmentBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeMapperImpl;
import com.masonsoft.imsdk.sample.widget.DividerItemDecoration;
import com.masonsoft.imsdk.util.Objects;
import com.masonsoft.imsdk.util.TimeDiffDebugHelper;

import java.util.Collections;
import java.util.List;

import io.github.idonans.core.thread.Threads;
import io.github.idonans.core.util.DimenUtil;
import io.github.idonans.core.util.Preconditions;
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

        @WorkerThread
        void mergeSortedConversationList(@NonNull final List<UnionTypeItemObject> unionTypeItemObjectList) {
            final String tag = Objects.defaultObjectTag(this) + "[mergeSortedConversationList][" + System.currentTimeMillis() + "][size:]" + unionTypeItemObjectList.size();
            SampleLog.v(tag);
            getAdapter().getData().beginTransaction()
                    .add((transaction, groupArrayList) -> {
                        if (groupArrayList.getGroupItemsSize(getGroupContent()) == 0) {
                            // request init page
                            Threads.postUi(() -> {
                                if (mPresenter != null) {
                                    SampleLog.v(tag + " use requestInit instead of merge");
                                    if (!mPresenter.getInitRequestStatus().isLoading()) {
                                        mPresenter.requestInit(true);
                                    }
                                }
                            });
                            return;
                        }

                        final TimeDiffDebugHelper innerMergeTimeDiffDebugHelper = new TimeDiffDebugHelper("innerMergeTimeDiffDebugHelper[" + tag + "]");

                        final List<UnionTypeItemObject> currentList = groupArrayList.getGroupItems(getGroupContent());
                        Preconditions.checkNotNull(currentList);

                        // merge
                        // 第一步，从原来的列表中删除重复元素
                        {
                            innerMergeTimeDiffDebugHelper.mark();
                            for (UnionTypeItemObject unionTypeItemObject : unionTypeItemObjectList) {
                                for (int i = 0; i < currentList.size(); i++) {
                                    if (currentList.get(i).isSameItem(unionTypeItemObject)) {
                                        currentList.remove(i);
                                        break;
                                    }
                                }
                            }
                            innerMergeTimeDiffDebugHelper.print("rm duplicate");
                        }

                        // 第二步，去除 unionTypeItemObjectList 中已删除的元素
                        innerMergeTimeDiffDebugHelper.mark();
                        for (int i = unionTypeItemObjectList.size() - 1; i >= 0; i--) {
                            final MSIMConversation updateConversation = (MSIMConversation) ((DataObject<?>) unionTypeItemObjectList.get(i).itemObject).object;
                            if (updateConversation.isDelete()) {
                                unionTypeItemObjectList.remove(i);
                            }
                        }
                        innerMergeTimeDiffDebugHelper.print("rm delete");

                        // 第三步，将 unionTypeItemObjectList 与 currentList 合并(这两个都是有序列表，且其中不包含重复元素)
                        innerMergeTimeDiffDebugHelper.mark();
                        currentList.addAll(0, unionTypeItemObjectList);
                        innerMergeTimeDiffDebugHelper.mark();
                        innerMergeTimeDiffDebugHelper.print("add all");
                        Collections.sort(currentList, (o1, o2) -> {
                            final MSIMConversation o1Object = (MSIMConversation) ((DataObject<?>) o1.itemObject).object;
                            final MSIMConversation o2Object = (MSIMConversation) ((DataObject<?>) o2.itemObject).object;
                            final long o1ObjectSeq = o1Object.getSeq();
                            final long o2ObjectSeq = o2Object.getSeq();
                            final long diff = o1ObjectSeq - o2ObjectSeq;
                            return diff == 0 ? 0 : (diff < 0 ? 1 : -1);
                        });
                        innerMergeTimeDiffDebugHelper.mark();
                        innerMergeTimeDiffDebugHelper.print("sort");

                        groupArrayList.removeGroup(getGroupHeader());
                    })
                    .commit();
        }
    }

}
