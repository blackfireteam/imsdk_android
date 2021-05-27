package com.masonsoft.imsdk.sample.app.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.masonsoft.imsdk.MSIMConstants;
import com.masonsoft.imsdk.MSIMConversation;
import com.masonsoft.imsdk.MSIMManager;
import com.masonsoft.imsdk.MSIMMessage;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeViewHolderListeners;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageViewHolder;
import com.masonsoft.imsdk.sample.widget.MSIMConversationChangedViewHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.github.idonans.dynamic.page.PagePresenter;
import io.github.idonans.dynamic.page.PageView;
import io.github.idonans.dynamic.page.UnionTypeStatusPageView;
import io.github.idonans.lang.DisposableHolder;
import io.github.idonans.uniontype.UnionTypeItemObject;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleSource;

public class SingleChatFragmentPresenter extends PagePresenter<UnionTypeItemObject, UnionTypeStatusPageView> {

    private static final boolean DEBUG = true;

    private final long mSessionUserId;
    private final int mConversationType = MSIMConstants.ConversationType.C2C;
    private final long mTargetUserId;
    private final int mPageSize = 20;
    private long mFirstMessageSeq = -1;
    private long mLastMessageSeq = -1;

    @SuppressWarnings("FieldCanBeLocal")
    private final MSIMConversationChangedViewHelper mConversationChangedViewHelper;

    private final DisposableHolder mDefaultRequestHolder = new DisposableHolder();

    @UiThread
    public SingleChatFragmentPresenter(@NonNull SingleChatFragment.ViewImpl view) {
        super(view, true, true);
        mSessionUserId = IMSessionManager.getInstance().getSessionUserId();
        mTargetUserId = view.getTargetUserId();

        mConversationChangedViewHelper = new MSIMConversationChangedViewHelper() {
            @Override
            protected void onConversationChanged(@Nullable MSIMConversation conversation, @Nullable Object customObject) {
                if (conversation == null) {
                    reloadOrRequestMoreMessage();
                    return;
                }

                final long sessionUserId = conversation.getSessionUserId();
                final int conversationType = conversation.getConversationType();
                final long targetUserId = conversation.getTargetUserId();
                if (mSessionUserId == sessionUserId
                        && mConversationType == conversationType
                        && mTargetUserId == targetUserId) {
                    reloadOrRequestMoreMessage();
                }
            }
        };
    }

    @Nullable
    public SingleChatFragment.ViewImpl getView() {
        return (SingleChatFragment.ViewImpl) super.getView();
    }

    private void reloadOrRequestMoreMessage() {
        if (mLastMessageSeq > 0) {
            if (!getNextPageRequestStatus().isLoading()) {
                requestNextPage(true);
            }
        } else {
            if (!getInitRequestStatus().isLoading()) {
                requestInit(true);
            }
        }
    }

    private final UnionTypeViewHolderListeners.OnItemClickListener mOnHolderItemClickListener = viewHolder -> {
        SingleChatFragment.ViewImpl view = getView();
        if (view != null) {
            IMMessageViewHolder.Helper.showPreview(viewHolder, view.getTargetUserId());
        }
    };

    private final UnionTypeViewHolderListeners.OnItemLongClickListener mOnHolderItemLongClickListener = viewHolder -> {
        SingleChatFragment.ViewImpl view = getView();
        if (view != null) {
            IMMessageViewHolder.Helper.showMenu(viewHolder);
        }
    };

    @Nullable
    private UnionTypeItemObject createDefault(@Nullable MSIMMessage message) {
        if (message == null) {
            return null;
        }
        final DataObject<MSIMMessage> dataObject = new DataObject<>(message)
                .putExtHolderItemClick1(mOnHolderItemClickListener)
                .putExtHolderItemLongClick1(mOnHolderItemLongClickListener);
        return IMMessageViewHolder.Helper.createDefault(dataObject, mSessionUserId);
    }

    @Nullable
    @Override
    protected SingleSource<Collection<UnionTypeItemObject>> createInitRequest() throws Exception {
        SampleLog.v("createInitRequest");
        if (DEBUG) {
            SampleLog.v("createInitRequest sessionUserId:%s, mConversationType:%s, targetUserId:%s, pageSize:%s",
                    mSessionUserId,
                    mConversationType,
                    mTargetUserId,
                    mPageSize);
        }

        return Single.just("")
                .map(input -> MSIMManager.getInstance().getMessageManager().pageQueryHistoryMessage(
                        mSessionUserId,
                        0,
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
                        UnionTypeItemObject item = createDefault(message);
                        if (item == null) {
                            if (DEBUG) {
                                SampleLog.e("createInitRequest ignore null UnionTypeItemObject");
                            }
                            continue;
                        }
                        target.add(item);
                    }

                    return target;
                });
    }

    @Override
    protected void onInitRequestResult(@NonNull PageView<UnionTypeItemObject> view, @NonNull Collection<UnionTypeItemObject> items) {
        IMLog.v("onInitRequestResult");
        if (DEBUG) {
            IMLog.v("onInitRequestResult items size:%s", items.size());
        }

        // 记录上一页，下一页参数
        if (items.isEmpty()) {
            mFirstMessageSeq = -1;
            mLastMessageSeq = -1;
        } else {
            mFirstMessageSeq = ((MSIMMessage) ((DataObject) ((UnionTypeItemObject) ((List) items).get(0)).itemObject).object).getSeq();
            mLastMessageSeq = ((MSIMMessage) ((DataObject) ((UnionTypeItemObject) ((List) items).get(items.size() - 1)).itemObject).object).getSeq();
        }
        super.onInitRequestResult(view, items);
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
                        UnionTypeItemObject item = createDefault(message);
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
                        UnionTypeItemObject item = createDefault(message);
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

    @Override
    public void setAbort() {
        super.setAbort();
        mDefaultRequestHolder.clear();
    }

}
