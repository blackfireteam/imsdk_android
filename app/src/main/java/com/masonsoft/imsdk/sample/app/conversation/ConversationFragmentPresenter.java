package com.masonsoft.imsdk.sample.app.conversation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.masonsoft.imsdk.MSIMConstants;
import com.masonsoft.imsdk.MSIMConversation;
import com.masonsoft.imsdk.MSIMConversationListener;
import com.masonsoft.imsdk.MSIMConversationListenerProxy;
import com.masonsoft.imsdk.MSIMManager;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeMapperImpl;
import com.masonsoft.imsdk.sample.widget.SessionUserIdChangedViewHelper;
import com.masonsoft.imsdk.util.Objects;
import com.masonsoft.imsdk.util.TimeDiffDebugHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.github.idonans.core.thread.Threads;
import io.github.idonans.dynamic.page.PagePresenter;
import io.github.idonans.dynamic.page.PageView;
import io.github.idonans.dynamic.page.UnionTypeStatusPageView;
import io.github.idonans.lang.DisposableHolder;
import io.github.idonans.uniontype.DeepDiff;
import io.github.idonans.uniontype.UnionTypeItemObject;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleSource;

public class ConversationFragmentPresenter extends PagePresenter<UnionTypeItemObject, UnionTypeStatusPageView> {

    private static final boolean DEBUG = true;

    private final SessionUserIdChangedViewHelper mSessionUserIdChangedViewHelper;
    @SuppressWarnings("FieldCanBeLocal")
    private final MSIMConversationListener mConversationListener;
    private final int mConversationType = MSIMConstants.ConversationType.C2C;
    private final int mPageSize = 20;
    private long mFirstConversationSeq = -1;
    private long mLastConversationSeq = -1;

    private final DisposableHolder mDefaultRequestHolder = new DisposableHolder();

    @UiThread
    public ConversationFragmentPresenter(@NonNull ConversationFragment.ViewImpl view) {
        super(view, false, true);
        mSessionUserIdChangedViewHelper = new SessionUserIdChangedViewHelper() {
            @Override
            protected void onSessionUserIdChanged(long sessionUserId) {
                reloadWithNewSessionUserId();
            }
        };
        mConversationListener = new MSIMConversationListenerProxy(new MSIMConversationListener() {
            @Override
            public void onConversationChanged(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
                addOrUpdateConversation(sessionUserId, conversationId);
            }

            @Override
            public void onConversationCreated(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
                addOrUpdateConversation(sessionUserId, conversationId);
            }
        }) {
            @Nullable
            @Override
            protected Object getOnConversationCreatedTag(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
                // merge created, changed callback
                return super.getOnConversationChangedTag(sessionUserId, conversationId, conversationType, targetUserId);
            }
        };
        MSIMManager.getInstance().getConversationManager().addConversationListener(mConversationListener);
    }

    private long getSessionUserId() {
        return mSessionUserIdChangedViewHelper.getSessionUserId();
    }

    private void reloadWithNewSessionUserId() {
        requestInit(true);
    }

    private boolean isAbort(long sessionUserId) {
        if (super.isAbort()) {
            return true;
        }
        if (getSessionUserId() != sessionUserId) {
            return true;
        }
        return getView() == null;
    }

    private void addOrUpdateConversation(long sessionUserId, long conversationId) {
        if (isAbort(sessionUserId)) {
            return;
        }

        Threads.postBackground(() -> {
            if (isAbort(sessionUserId)) {
                return;
            }
            final MSIMConversation conversation = MSIMManager.getInstance().getConversationManager().getConversation(sessionUserId, conversationId);
            if (isAbort(sessionUserId)) {
                return;
            }
            if (conversation != null) {
                Threads.postUi(() -> {
                    if (isAbort(sessionUserId)) {
                        return;
                    }
                    addOrUpdateConversation(conversation);
                });
            }
        });
    }

    private void addOrUpdateConversation(@Nullable MSIMConversation conversation) {
        if (conversation == null) {
            return;
        }

        final UnionTypeItemObject unionTypeItemObject = createDefault(conversation);
        if (unionTypeItemObject == null) {
            return;
        }
        final ConversationFragment.ViewImpl view = getView();
        if (view == null) {
            return;
        }
        final TimeDiffDebugHelper timeDiffDebugHelper = new TimeDiffDebugHelper(Objects.defaultObjectTag(this));
        timeDiffDebugHelper.mark();
        view.replaceConversation(unionTypeItemObject);
        timeDiffDebugHelper.mark();
        timeDiffDebugHelper.print("addOrUpdateConversation targetUserId:" + conversation.getTargetUserId() + ", sessionUserId:" + conversation.getSessionUserId());
    }

    @Nullable
    public ConversationFragment.ViewImpl getView() {
        return (ConversationFragment.ViewImpl) super.getView();
    }

    @Nullable
    private UnionTypeItemObject createDefault(@Nullable MSIMConversation conversation) {
        if (conversation == null) {
            return null;
        }
        final DataObject<MSIMConversation> dataObject = new DeepDiffDataObject(conversation);
        return new UnionTypeItemObject(
                UnionTypeMapperImpl.UNION_TYPE_IMPL_IM_CONVERSATION,
                dataObject
        );
    }

    @Nullable
    @Override
    protected SingleSource<Collection<UnionTypeItemObject>> createInitRequest() throws Exception {
        SampleLog.v(Objects.defaultObjectTag(this) + " createInitRequest");
        if (DEBUG) {
            SampleLog.v(Objects.defaultObjectTag(this) + " createInitRequest sessionUserId:%s, mConversationType:%s, pageSize:%s",
                    getSessionUserId(),
                    mConversationType,
                    mPageSize);
        }

        return Single.just("")
                .map(input -> MSIMManager.getInstance().getConversationManager().pageQueryConversation(
                        getSessionUserId(),
                        0,
                        mPageSize,
                        mConversationType))
                .map(page -> {
                    List<MSIMConversation> conversationList = page.items;
                    if (conversationList == null) {
                        conversationList = new ArrayList<>();
                    }
                    List<UnionTypeItemObject> target = new ArrayList<>();
                    for (MSIMConversation conversation : conversationList) {
                        UnionTypeItemObject item = createDefault(conversation);
                        if (item == null) {
                            if (DEBUG) {
                                SampleLog.e(Objects.defaultObjectTag(this) + " createInitRequest ignore null UnionTypeItemObject");
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
        SampleLog.v(Objects.defaultObjectTag(this) + " onInitRequestResult");
        if (DEBUG) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onInitRequestResult items size:%s", items.size());
        }
        // 记录上一页，下一页参数
        if (items.isEmpty()) {
            mFirstConversationSeq = -1;
            mLastConversationSeq = -1;
        } else {
            mFirstConversationSeq = ((MSIMConversation) ((DataObject) ((UnionTypeItemObject) ((List) items).get(0)).itemObject).object).getSeq();
            mLastConversationSeq = ((MSIMConversation) ((DataObject) ((UnionTypeItemObject) ((List) items).get(items.size() - 1)).itemObject).object).getSeq();
        }
        super.onInitRequestResult(view, items);
    }

    @Nullable
    @Override
    protected SingleSource<Collection<UnionTypeItemObject>> createPrePageRequest() throws Exception {
        final Throwable e = new IllegalAccessError("not support");
        SampleLog.e(e);
        return null;
    }

    @Nullable
    @Override
    protected SingleSource<Collection<UnionTypeItemObject>> createNextPageRequest() throws Exception {
        SampleLog.v(Objects.defaultObjectTag(this) + " createNextPageRequest");
        if (DEBUG) {
            SampleLog.v(Objects.defaultObjectTag(this) + " createNextPageRequest sessionUserId:%s, mConversationType:%s, pageSize:%s, mLastConversationSeq:%s",
                    getSessionUserId(),
                    mConversationType,
                    mPageSize,
                    mLastConversationSeq);
        }

        if (mLastConversationSeq <= 0) {
            SampleLog.e(Objects.defaultObjectTag(this) + " createNextPageRequest invalid mLastConversationSeq:%s", mLastConversationSeq);
            return null;
        }

        return Single.just("")
                .map(input -> MSIMManager.getInstance().getConversationManager().pageQueryConversation(
                        getSessionUserId(),
                        mLastConversationSeq,
                        mPageSize,
                        mConversationType))
                .map(page -> {
                    List<MSIMConversation> conversationList = page.items;
                    if (conversationList == null) {
                        conversationList = new ArrayList<>();
                    }
                    List<UnionTypeItemObject> target = new ArrayList<>();
                    for (MSIMConversation conversation : conversationList) {
                        UnionTypeItemObject item = createDefault(conversation);
                        if (item == null) {
                            if (DEBUG) {
                                SampleLog.e(Objects.defaultObjectTag(this) + " createNextPageRequest ignore null UnionTypeItemObject");
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
        SampleLog.v(Objects.defaultObjectTag(this) + " onNextPageRequestResult");
        if (DEBUG) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onNextPageRequestResult items size:%s", items.size());
        }

        // 记录上一页，下一页参数
        if (!items.isEmpty()) {
            mLastConversationSeq = ((MSIMConversation) ((DataObject) ((UnionTypeItemObject) ((List) items).get(items.size() - 1)).itemObject).object).getSeq();
        }
        super.onNextPageRequestResult(view, items);
    }

    @Override
    public void setAbort() {
        super.setAbort();
        mDefaultRequestHolder.clear();
    }

    private static class DeepDiffDataObject extends DataObject<MSIMConversation> implements DeepDiff {

        public DeepDiffDataObject(@NonNull MSIMConversation object) {
            super(object);
        }

        @Override
        public boolean isSameItem(@Nullable Object other) {
            if (other instanceof DeepDiffDataObject) {
                final DeepDiffDataObject otherDataObject = (DeepDiffDataObject) other;
                return this.object.getConversationId() == otherDataObject.object.getConversationId();
            }
            return false;
        }

        @Override
        public boolean isSameContent(@Nullable Object other) {
            if (other instanceof DeepDiffDataObject) {
                final DeepDiffDataObject otherDataObject = (DeepDiffDataObject) other;
                return this.object.getConversationId() == otherDataObject.object.getConversationId();
            }
            return false;
        }
    }

}
