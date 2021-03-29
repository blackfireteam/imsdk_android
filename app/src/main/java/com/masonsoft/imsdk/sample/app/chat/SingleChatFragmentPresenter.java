package com.masonsoft.imsdk.sample.app.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.idonans.core.thread.Threads;
import com.idonans.dynamic.page.PagePresenter;
import com.idonans.dynamic.page.PageView;
import com.idonans.dynamic.page.UnionTypeStatusPageView;
import com.idonans.lang.DisposableHolder;
import com.idonans.uniontype.UnionTypeItemObject;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMMessageManager;
import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.core.observable.ConversationObservable;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageViewHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleSource;

public class SingleChatFragmentPresenter extends PagePresenter<UnionTypeItemObject, UnionTypeStatusPageView> {

    private static final boolean DEBUG = false;

    private final long mSessionUserId;
    private final int mConversationType = IMConstants.ConversationType.C2C;
    private final long mTargetUserId;
    private final int mPageSize = 20;
    private long mFirstMessageSeq = -1;
    private long mLastMessageSeq = -1;

    private final DisposableHolder mDefaultRequestHolder = new DisposableHolder();

    @UiThread
    public SingleChatFragmentPresenter(@NonNull SingleChatFragment.ViewImpl view) {
        super(view, true, true);
        mSessionUserId = IMSessionManager.getInstance().getSessionUserId();
        mTargetUserId = view.getTargetUserId();
        ConversationObservable.DEFAULT.registerObserver(mConversationObserver);
    }

    @Nullable
    public SingleChatFragment.ViewImpl getView() {
        return (SingleChatFragment.ViewImpl) super.getView();
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final ConversationObservable.ConversationObserver mConversationObserver = new ConversationObservable.ConversationObserver() {
        @Override
        public void onConversationChanged(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
            Threads.postUi(() -> onConversationChangedInternal(sessionUserId, conversationId, conversationType, targetUserId));
        }

        @Override
        public void onConversationCreated(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
            Threads.postUi(() -> onConversationChangedInternal(sessionUserId, conversationId, conversationType, targetUserId));
        }

        private void onConversationChangedInternal(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
            if (mSessionUserId != sessionUserId || mConversationType != conversationType || mTargetUserId != targetUserId) {
                return;
            }

            if (mLastMessageSeq > 0) {
                requestNextPage(true);
            } else {
                requestInit(true);
            }
        }
    };

    @Nullable
    private UnionTypeItemObject createDefault(@Nullable IMMessage imMessage) {
        if (imMessage == null) {
            return null;
        }
        final DataObject<IMMessage> dataObject = new DataObject<>(imMessage);
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

        return Single.fromCallable(() -> IMMessageManager.getInstance().pageQueryMessage(
                mSessionUserId,
                0,
                mPageSize,
                mConversationType,
                mTargetUserId,
                true,
                null))
                .map(page -> {
                    List<IMMessage> imMessages = page.items;
                    if (imMessages == null) {
                        imMessages = new ArrayList<>();
                    }
                    List<UnionTypeItemObject> target = new ArrayList<>();
                    for (IMMessage imMessage : imMessages) {
                        UnionTypeItemObject item = createDefault(imMessage);
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
        /*
        // TODO
        if (mTargetConversation != null) {
            // 设置新的会话焦点
            final long conversationId = mTargetConversation.id;
            final long sessionUserId = SessionManager.Session.getSessionUserId(mSession);
            SettingsManager.getInstance().getUserMemorySettings(sessionUserId).setFocusConversationId(conversationId);
            ImManager.getInstance().clearUnreadCount(conversationId);
        }*/

        // 记录上一页，下一页参数
        if (items.isEmpty()) {
            mFirstMessageSeq = -1;
            mLastMessageSeq = -1;
        } else {
            mFirstMessageSeq = ((IMMessage) ((DataObject) ((UnionTypeItemObject) ((List) items).get(0)).itemObject).object).seq.get();
            mLastMessageSeq = ((IMMessage) ((DataObject) ((UnionTypeItemObject) ((List) items).get(items.size() - 1)).itemObject).object).seq.get();
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

        return Single.fromCallable(() -> IMMessageManager.getInstance().pageQueryMessage(
                mSessionUserId,
                mFirstMessageSeq,
                mPageSize,
                mConversationType,
                mTargetUserId,
                true,
                null))
                .map(page -> {
                    List<IMMessage> imMessages = page.items;
                    if (imMessages == null) {
                        imMessages = new ArrayList<>();
                    }
                    List<UnionTypeItemObject> target = new ArrayList<>();
                    for (IMMessage imMessage : imMessages) {
                        UnionTypeItemObject item = createDefault(imMessage);
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
        // 记录上一页，下一页参数
        if (!items.isEmpty()) {
            mFirstMessageSeq = ((IMMessage) ((DataObject) ((UnionTypeItemObject) ((List) items).get(0)).itemObject).object).seq.get();
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

        return Single.fromCallable(() -> IMMessageManager.getInstance().pageQueryMessage(
                mSessionUserId,
                mLastMessageSeq,
                mPageSize,
                mConversationType,
                mTargetUserId,
                false,
                null))
                .map(page -> {
                    List<IMMessage> imMessages = page.items;
                    if (imMessages == null) {
                        imMessages = new ArrayList<>();
                    }
                    List<UnionTypeItemObject> target = new ArrayList<>();
                    for (IMMessage imMessage : imMessages) {
                        UnionTypeItemObject item = createDefault(imMessage);
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
        // 记录上一页，下一页参数
        if (!items.isEmpty()) {
            mLastMessageSeq = ((IMMessage) ((DataObject) ((UnionTypeItemObject) ((List) items).get(items.size() - 1)).itemObject).object).seq.get();
        }
        super.onNextPageRequestResult(view, items);
    }

    @Override
    public void setAbort() {
        super.setAbort();
        mDefaultRequestHolder.clear();

        /* TODO
        // 清除会话焦点
        final long sessionUserId = SessionManager.Session.getSessionUserId(mSession);
        SettingsManager.getInstance().getUserMemorySettings(sessionUserId).setFocusConversationId(-1);
        */
    }

}
