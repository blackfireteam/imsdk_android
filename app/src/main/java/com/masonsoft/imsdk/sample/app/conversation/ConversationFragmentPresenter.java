package com.masonsoft.imsdk.sample.app.conversation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.masonsoft.imsdk.IMConversation;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMConversationManager;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.core.observable.ConversationObservable;
import com.masonsoft.imsdk.core.observable.SessionObservable;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeMapperImpl;
import com.masonsoft.imsdk.util.Objects;

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

    private long mSessionUserId;
    private final int mConversationType = IMConstants.ConversationType.C2C;
    private final int mPageSize = 20;
    private long mFirstConversationSeq = -1;
    private long mLastConversationSeq = -1;

    private final DisposableHolder mDefaultRequestHolder = new DisposableHolder();

    @UiThread
    public ConversationFragmentPresenter(@NonNull ConversationFragment.ViewImpl view) {
        super(view, false, true);
        mSessionUserId = IMSessionManager.getInstance().getSessionUserId();
        SessionObservable.DEFAULT.registerObserver(mSessionObserver);
        ConversationObservable.DEFAULT.registerObserver(mConversationObserver);
    }

    private void reloadWithNewSessionUserId() {
        final long newSessionUserId = IMSessionManager.getInstance().getSessionUserId();
        if (mSessionUserId != newSessionUserId) {
            mSessionUserId = newSessionUserId;
            requestInit(true);
        }
    }

    @Nullable
    public ConversationFragment.ViewImpl getView() {
        return (ConversationFragment.ViewImpl) super.getView();
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final SessionObservable.SessionObserver mSessionObserver = new SessionObservable.SessionObserver() {
        @Override
        public void onSessionChanged() {
        }

        @Override
        public void onSessionUserIdChanged() {
            Threads.postUi(() -> reloadWithNewSessionUserId());
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ConversationObservable.ConversationObserver mConversationObserver = new ConversationObservable.ConversationObserver() {

        private boolean notMatch(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
            return (sessionUserId != IMConstants.ID_ANY && mSessionUserId != sessionUserId);
        }

        @Override
        public void onConversationChanged(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
            onConversationChangedInternal(sessionUserId, conversationId, conversationType, targetUserId);
        }

        @Override
        public void onConversationCreated(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
            onConversationChangedInternal(sessionUserId, conversationId, conversationType, targetUserId);
        }

        private void onConversationChangedInternal(long sessionUserId, long conversationId, int conversationType, long targetUserId) {
            if (notMatch(sessionUserId, conversationId, conversationType, targetUserId)) {
                return;
            }

            if (sessionUserId <= 0) {
                // ignore
                return;
            }

            IMConversation conversation = null;
            if (conversationId > 0) {
                conversation = IMConversationManager.getInstance().getConversation(sessionUserId, conversationId);
            } else if (conversationType >= 0 && targetUserId > 0) {
                conversation = IMConversationManager.getInstance().getConversationByTargetUserId(sessionUserId, conversationType, targetUserId);
            }
            if (conversation == null) {
                SampleLog.e(Objects.defaultObjectTag(this) + " conversation not found");
                return;
            }

            final UnionTypeItemObject unionTypeItemObject = createDefault(conversation);
            if (unionTypeItemObject == null) {
                return;
            }
            Threads.postUi(() -> {
                if (notMatch(sessionUserId, conversationId, conversationType, targetUserId)) {
                    return;
                }
                final ConversationFragment.ViewImpl view = getView();
                if (view == null) {
                    return;
                }
                view.replaceConversation(unionTypeItemObject);
            });
        }
    };

    @Nullable
    private UnionTypeItemObject createDefault(@Nullable IMConversation imConversation) {
        if (imConversation == null) {
            return null;
        }
        final DataObject<IMConversation> dataObject = new DeepDiffDataObject<>(imConversation);
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
                    mSessionUserId,
                    mConversationType,
                    mPageSize);
        }

        return Single.fromCallable(() -> IMConversationManager.getInstance().pageQueryConversation(
                mSessionUserId,
                0,
                mPageSize,
                mConversationType))
                .map(page -> {
                    List<IMConversation> imConversationList = page.items;
                    if (imConversationList == null) {
                        imConversationList = new ArrayList<>();
                    }
                    List<UnionTypeItemObject> target = new ArrayList<>();
                    for (IMConversation imConversation : imConversationList) {
                        UnionTypeItemObject item = createDefault(imConversation);
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
            mFirstConversationSeq = ((IMConversation) ((DataObject) ((UnionTypeItemObject) ((List) items).get(0)).itemObject).object).seq.get();
            mLastConversationSeq = ((IMConversation) ((DataObject) ((UnionTypeItemObject) ((List) items).get(items.size() - 1)).itemObject).object).seq.get();
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
                    mSessionUserId,
                    mConversationType,
                    mPageSize,
                    mLastConversationSeq);
        }

        if (mLastConversationSeq <= 0) {
            SampleLog.e(Objects.defaultObjectTag(this) + " createNextPageRequest invalid mLastConversationSeq:%s", mLastConversationSeq);
            return null;
        }

        return Single.fromCallable(() -> IMConversationManager.getInstance().pageQueryConversation(
                mSessionUserId,
                mLastConversationSeq,
                mPageSize,
                mConversationType))
                .map(page -> {
                    List<IMConversation> imConversationList = page.items;
                    if (imConversationList == null) {
                        imConversationList = new ArrayList<>();
                    }
                    List<UnionTypeItemObject> target = new ArrayList<>();
                    for (IMConversation imConversation : imConversationList) {
                        UnionTypeItemObject item = createDefault(imConversation);
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
        IMLog.v(Objects.defaultObjectTag(this) + " onNextPageRequestResult");
        if (DEBUG) {
            IMLog.v(Objects.defaultObjectTag(this) + " onNextPageRequestResult items size:%s", items.size());
        }

        // 记录上一页，下一页参数
        if (!items.isEmpty()) {
            mLastConversationSeq = ((IMConversation) ((DataObject) ((UnionTypeItemObject) ((List) items).get(items.size() - 1)).itemObject).object).seq.get();
        }
        super.onNextPageRequestResult(view, items);
    }

    @Override
    public void setAbort() {
        super.setAbort();
        mDefaultRequestHolder.clear();
    }

    private class DeepDiffDataObject<T> extends DataObject<T> implements DeepDiff {

        public DeepDiffDataObject(T object) {
            super(object);
        }

        @Override
        public boolean isSameItem(@Nullable Object other) {
            if (other instanceof DeepDiffDataObject) {
                final DeepDiffDataObject<?> otherDataObject = (DeepDiffDataObject<?>) other;
                if (this.object instanceof IMConversation && otherDataObject.object instanceof IMConversation) {
                    final IMConversation thisImConversation = (IMConversation) this.object;
                    final IMConversation otherImConversation = (IMConversation) otherDataObject.object;
                    final long thisImConversationId = thisImConversation.id.get();
                    return thisImConversationId == otherImConversation.id.get();
                }
            }
            return false;
        }

        @Override
        public boolean isSameContent(@Nullable Object other) {
            if (other instanceof DeepDiffDataObject) {
                final DeepDiffDataObject<?> otherDataObject = (DeepDiffDataObject<?>) other;
                if (this.object instanceof IMConversation && otherDataObject.object instanceof IMConversation) {
                    final IMConversation thisImConversation = (IMConversation) this.object;
                    final IMConversation otherImConversation = (IMConversation) otherDataObject.object;
                    final long thisImConversationId = thisImConversation.id.get();
                    return thisImConversationId == otherImConversation.id.get();
                }
            }
            return false;
        }
    }

}
