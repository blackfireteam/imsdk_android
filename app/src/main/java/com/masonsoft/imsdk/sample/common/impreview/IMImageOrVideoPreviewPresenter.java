package com.masonsoft.imsdk.sample.common.impreview;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.google.common.collect.Lists;
import com.masonsoft.imsdk.MSIMManager;
import com.masonsoft.imsdk.MSIMMessage;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeViewHolderListeners;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageViewHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.github.idonans.dynamic.page.PagePresenter;
import io.github.idonans.dynamic.page.PageView;
import io.github.idonans.dynamic.page.UnionTypeStatusPageView;
import io.github.idonans.uniontype.UnionTypeItemObject;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleSource;

public class IMImageOrVideoPreviewPresenter extends PagePresenter<UnionTypeItemObject, UnionTypeStatusPageView> {

    private static final boolean DEBUG = Constants.DEBUG_WIDGET;

    private final long mSessionUserId;
    private final int mConversationType;
    private final long mTargetUserId;
    private final int mPageSize = 20;
    private long mFirstMessageSeq = -1;
    private long mLastMessageSeq = -1;

    @UiThread
    public IMImageOrVideoPreviewPresenter(@NonNull IMImageOrVideoPreviewDialog.ViewImpl view, long targetUserId, long initMessageSeq) {
        super(view, initMessageSeq >= 0, initMessageSeq >= 0);
        mSessionUserId = IMSessionManager.getInstance().getSessionUserId();
        mConversationType = IMConstants.ConversationType.C2C;
        mTargetUserId = targetUserId;
        mFirstMessageSeq = initMessageSeq;
        mLastMessageSeq = initMessageSeq;
    }

    void showInitMessage(MSIMMessage initMessage) {
        UnionTypeStatusPageView view = getView();
        if (view == null) {
            return;
        }

        view.hideInitLoading();
        view.onInitDataLoad(Lists.newArrayList(create(initMessage, true)));
    }

    private final UnionTypeViewHolderListeners.OnItemClickListener mOnHolderItemClickListener = viewHolder -> {
        IMImageOrVideoPreviewDialog.ViewImpl view = (IMImageOrVideoPreviewDialog.ViewImpl) getView();
        if (view != null) {
            view.hide();
        }
    };

    @Nullable
    private UnionTypeItemObject create(MSIMMessage message) {
        return create(message, false);
    }

    @Nullable
    private UnionTypeItemObject create(MSIMMessage message, boolean autoPlay) {
        if (message == null) {
            return null;
        }

        return IMMessageViewHolder.Helper.createPreviewDefault(
                new DataObject<>(message)
                        .putExtObjectBoolean1(autoPlay)
                        .putExtHolderItemClick1(mOnHolderItemClickListener),
                mSessionUserId
        );
    }

    @Nullable
    @Override
    protected SingleSource<Collection<UnionTypeItemObject>> createInitRequest() throws Exception {
        return null;
    }

    @Nullable
    @Override
    protected SingleSource<Collection<UnionTypeItemObject>> createPrePageRequest() throws Exception {
        SampleLog.v("createPrePageRequest");
        if (DEBUG) {
            SampleLog.v("createPrePageRequest sessionUserId:%s, mConversationType:%s, targetUserId:%s, pageSize:%s, firstMessageSeq:%s",
                    mSessionUserId,
                    mConversationType,
                    mTargetUserId,
                    mPageSize,
                    mFirstMessageSeq);
        }

        if (mFirstMessageSeq <= 0) {
            SampleLog.e("createPrePageRequest invalid firstMessageSeq:%s", mFirstMessageSeq);
            return null;
        }

        return Single.just("")
                .map(input -> MSIMManager.getInstance().getMessageManager().pageQueryHistoryMessage(
                        mSessionUserId,
                        mFirstMessageSeq,
                        mPageSize,
                        mConversationType,
                        mTargetUserId))
                .map(page -> {
                    List<MSIMMessage> messageList = page.items;
                    if (messageList == null) {
                        messageList = new ArrayList<>();
                    }
                    Collections.reverse(messageList);
                    List<UnionTypeItemObject> target = new ArrayList<>();
                    for (MSIMMessage message : messageList) {
                        UnionTypeItemObject item = create(message);
                        if (item == null) {
                            if (DEBUG) {
                                SampleLog.e("createPrePageRequest ignore null UnionTypeItemObject");
                            }
                            continue;
                        }
                        target.add(item);
                    }

                    return target;
                });
    }

    @Override
    protected void onPrePageRequestResult(@NonNull PageView<UnionTypeItemObject> view, @NonNull Collection<UnionTypeItemObject> items) {
        IMLog.v("onPrePageRequestResult");
        if (DEBUG) {
            IMLog.v("onPrePageRequestResult items size:%s", items.size());
        }

        // 记录上一页，下一页参数
        if (!items.isEmpty()) {
            mFirstMessageSeq = ((MSIMMessage) ((DataObject) ((UnionTypeItemObject) ((List) items).get(0)).itemObject).object).getSeq();
        }
        super.onPrePageRequestResult(view, items);
    }

    @Nullable
    @Override
    protected SingleSource<Collection<UnionTypeItemObject>> createNextPageRequest() throws Exception {
        SampleLog.v("createNextPageRequest");
        if (DEBUG) {
            SampleLog.v("createNextPageRequest sessionUserId:%s, mConversationType:%s, targetUserId:%s, pageSize:%s, lastMessageSeq:%s",
                    mSessionUserId,
                    mConversationType,
                    mTargetUserId,
                    mPageSize,
                    mLastMessageSeq);
        }

        if (mLastMessageSeq <= 0) {
            SampleLog.e("createNextPageRequest invalid lastMessageSeq:%s", mLastMessageSeq);
            return null;
        }

        return Single.just("")
                .map(input -> MSIMManager.getInstance().getMessageManager().pageQueryNewMessage(
                        mSessionUserId,
                        mLastMessageSeq,
                        mPageSize,
                        mConversationType,
                        mTargetUserId))
                .map(page -> {
                    List<MSIMMessage> messageList = page.items;
                    if (messageList == null) {
                        messageList = new ArrayList<>();
                    }
                    List<UnionTypeItemObject> target = new ArrayList<>();
                    for (MSIMMessage message : messageList) {
                        UnionTypeItemObject item = create(message);
                        if (item == null) {
                            if (DEBUG) {
                                SampleLog.e("createNextPageRequest ignore null UnionTypeItemObject");
                            }
                            continue;
                        }
                        target.add(item);
                    }

                    return target;
                });
    }

    @Override
    protected void onNextPageRequestResult(@NonNull PageView<UnionTypeItemObject> view, @NonNull Collection<UnionTypeItemObject> items) {
        IMLog.v("onNextPageRequestResult");
        if (DEBUG) {
            IMLog.v("onNextPageRequestResult items size:%s", items.size());
        }

        // 记录上一页，下一页参数
        if (!items.isEmpty()) {
            mLastMessageSeq = ((MSIMMessage) ((DataObject) ((UnionTypeItemObject) ((List) items).get(items.size() - 1)).itemObject).object).getSeq();
        }
        super.onNextPageRequestResult(view, items);
    }

}
