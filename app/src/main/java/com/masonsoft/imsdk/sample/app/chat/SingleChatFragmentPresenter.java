package com.masonsoft.imsdk.sample.app.chat;

import android.app.Activity;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.idonans.core.thread.Threads;
import com.idonans.dynamic.page.PagePresenter;
import com.idonans.dynamic.page.PageView;
import com.idonans.dynamic.page.UnionTypeStatusPageView;
import com.idonans.lang.DisposableHolder;
import com.idonans.uniontype.UnionTypeItemObject;
import com.masonsoft.imsdk.IMConversation;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMConversationManager;
import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.core.observable.ConversationObservable;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageViewHolder;
import com.masonsoft.imsdk.util.Objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleSource;

public class SingleChatFragmentPresenter extends PagePresenter<UnionTypeItemObject, UnionTypeStatusPageView> {

    private static final boolean DEBUG = false;

    private final long mSessionUserId;
    private final int mConversationType = IMConstants.ConversationType.C2C;
    private final long mTargetUserId;
    private final long mTargetConversationId;
    private final int mPageSize = 20;
    private long mFirstMessageId = -1;
    private long mLastMessageId = -1;

    private final DisposableHolder mDefaultRequestHolder = new DisposableHolder();

    @UiThread
    public SingleChatFragmentPresenter(@NonNull SingleChatFragment.ViewImpl view) {
        super(view, true, true);
        mSessionUserId = IMSessionManager.getInstance().getSessionUserId();
        mTargetUserId = view.getTargetUserId();
        final IMConversation targetConversation = IMConversationManager.getInstance().getOrCreateConversationByTargetUserId(
                mSessionUserId,
                mConversationType,
                mTargetUserId);
        if (DEBUG) {
            SampleLog.v(Objects.defaultObjectTag(this) + " mSessionUserId:%s, mConversationType:%s, mTargetUserId:%s, mTargetConversation:%s",
                    mSessionUserId,
                    mConversationType,
                    mTargetUserId,
                    targetConversation);
        }
        mTargetConversationId = targetConversation.targetUserId.get();

        ConversationObservable.DEFAULT.registerObserver(mConversationObserver);
    }

    public long getTargetConversationId() {
        return mTargetConversationId;
    }

    void requestClearConversation() {
        clearRequestExcept();
        final ImConversation targetConversation = mTargetConversation;
        if (targetConversation == null) {
            Timber.e("ignore requestClearConversation targetConversation is null");
            return;
        }

        ImSingleFragment.ViewImpl view = getView();
        if (view == null) {
            Timber.e("view is null");
            return;
        }
        mFirstMessageId = -1;
        mLastMessageId = -1;
        view.hideInitLoading();
        view.hideNextPageLoading();
        view.hidePrePageLoading();
        onInitRequestResult(view, Collections.emptyList());

        mInitRequestHolder.set(
                Single.fromCallable(
                        () -> {
                            ImManager.getInstance().deleteAllChatMessage(targetConversation);
                            return targetConversation;
                        }).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(chatConversation -> {
                            // ignore
                        }, Timber::e));
    }

    @Nullable
    public SingleChatFragment.ViewImpl getView() {
        return (SingleChatFragment.ViewImpl) super.getView();
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final ConversationObservable.ConversationObserver mConversationObserver = new ConversationObservable.ConversationObserver() {
        @Override
        public void onConversationChanged(long sessionUserId, long conversationId) {
            Threads.postUi(() -> {
                onConversationChangedInternal(sessionUserId, conversationId);
            });
        }

        @Override
        public void onConversationCreated(long sessionUserId, long conversationId) {
            Threads.postUi(() -> {
                onConversationChangedInternal(sessionUserId, conversationId);
            });
        }

        private void onConversationChangedInternal(long sessionUserId, long conversationId) {
            if (mSessionUserId != sessionUserId || mTargetConversationId != conversationId) {
                return;
            }

            if (mLastMessageId > 0) {
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
            SampleLog.v("createInitRequest sessionUserId:%s, targetConversationId:%s, pageSize:%s", mSessionUserId, mTargetConversationId, mPageSize);
        }

        return Single.fromCallable(() -> ImManager.getInstance().getLatestMessages(conversationId, mPageSize))
                .map(page -> {
                    List<ImMessage> imMessages = page.data;
                    if (imMessages == null) {
                        imMessages = new ArrayList<>();
                    }
                    List<UnionTypeItemObject> target = new ArrayList<>();
                    for (ImMessage imMessage : imMessages) {
                        UnionTypeItemObject item = createDefault(imMessage);
                        if (item == null) {
                            if (DEBUG) {
                                Timber.e("createInitRequest ignore null UnionTypeItemObject");
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
        if (mTargetConversation != null) {
            // 设置新的会话焦点
            final long conversationId = mTargetConversation.id;
            final long sessionUserId = SessionManager.Session.getSessionUserId(mSession);
            SettingsManager.getInstance().getUserMemorySettings(sessionUserId).setFocusConversationId(conversationId);
            ImManager.getInstance().clearUnreadCount(conversationId);
        }

        // 记录上一页，下一页参数
        if (items.isEmpty()) {
            mFirstMessageId = -1;
            mLastMessageId = -1;
        } else {
            mFirstMessageId = ((ImMessage) ((DataObject) ((UnionTypeItemObject) ((List) items).get(0)).itemObject).object).id;
            mLastMessageId = ((ImMessage) ((DataObject) ((UnionTypeItemObject) ((List) items).get(items.size() - 1)).itemObject).object).id;
        }
        super.onInitRequestResult(view, items);

        dispatchGiftPlayer(items);
    }

    @Nullable
    @Override
    protected SingleSource<Collection<UnionTypeItemObject>> createPrePageRequest() throws Exception {
        Timber.v("createPrePageRequest");

        if (mTargetConversation == null) {
            Timber.e("createPrePageRequest mTargetConversation is null");
            return null;
        }

        final long conversationId = mTargetConversation.id;
        if (DEBUG) {
            Timber.v("createPrePageRequest for conversationId:%s, pageSize:%s", conversationId, mPageSize);
        }

        if (mFirstMessageId <= 0) {
            Timber.e("createPrePageRequest invalid mFirstMessageId:%s", mFirstMessageId);
            return null;
        }

        return Single.fromCallable(() -> ImManager.getInstance().getOlderMessages(conversationId, mFirstMessageId, mPageSize))
                .map(page -> {
                    List<ImMessage> imMessages = page.data;
                    if (imMessages == null) {
                        imMessages = new ArrayList<>();
                    }
                    List<UnionTypeItemObject> target = new ArrayList<>();
                    for (ImMessage imMessage : imMessages) {
                        UnionTypeItemObject item = createDefault(imMessage);
                        if (item == null) {
                            if (DEBUG) {
                                Timber.e("createPrePageRequest ignore null UnionTypeItemObject");
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
            mFirstMessageId = ((ImMessage) ((DataObject) ((UnionTypeItemObject) ((List) items).get(0)).itemObject).object).id;
        }
        super.onPrePageRequestResult(view, items);

        dispatchGiftPlayer(items);
    }

    @Nullable
    @Override
    protected SingleSource<Collection<UnionTypeItemObject>> createNextPageRequest() throws Exception {
        Timber.v("createNextPageRequest");

        if (mTargetConversation == null) {
            Timber.e("createNextPageRequest mTargetConversation is null");
            return null;
        }

        final long conversationId = mTargetConversation.id;
        if (DEBUG) {
            Timber.v("createNextPageRequest for conversationId:%s, pageSize:%s", conversationId, mPageSize);
        }

        if (mLastMessageId <= 0) {
            Timber.e("createNextPageRequest invalid mLastMessageId:%s", mLastMessageId);
            return null;
        }

        return Single.fromCallable(() -> ImManager.getInstance().getNewerMessages(conversationId, mLastMessageId, mPageSize))
                .map(page -> {
                    List<ImMessage> imMessages = page.data;
                    if (imMessages == null) {
                        imMessages = new ArrayList<>();
                    }
                    List<UnionTypeItemObject> target = new ArrayList<>();
                    for (ImMessage imMessage : imMessages) {
                        UnionTypeItemObject item = createDefault(imMessage);
                        if (item == null) {
                            if (DEBUG) {
                                Timber.e("createNextPageRequest ignore null UnionTypeItemObject");
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
            mLastMessageId = ((ImMessage) ((DataObject) ((UnionTypeItemObject) ((List) items).get(items.size() - 1)).itemObject).object).id;
        }
        super.onNextPageRequestResult(view, items);

        dispatchGiftPlayer(items);
    }

    public void requestAddToBlack() {
        mDefaultRequestHolder.clear();
        {
            ImSingleFragment.ViewImpl view = getView();
            if (view == null) {
                Timber.v("view is null");
                return;
            }
        }
        mDefaultRequestHolder.set(
                CommonHttpApi.addToBlack(mTargetUserId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .firstOrError()
                        .subscribe(object -> {
                            ImSingleFragment.ViewImpl view = getView();
                            if (view == null) {
                                Timber.v("view is null");
                                return;
                            }

                            CacheUserInfo cacheUserInfo = UserCacheManager.getInstance().getByUserId(mTargetUserId, false);
                            if (cacheUserInfo != null) {
                                EventBus.getDefault().post(new AddBlackListEvent(cacheUserInfo.toUserInfo()));
                            } else {
                                Timber.e("CacheUserInfo not found for user id: %s", mTargetUserId);
                            }

                            TipUtil.showSuccessText("已将对方拉黑");
                        }, e -> {
                            Timber.e(e);
                            ImSingleFragment.ViewImpl view = getView();
                            if (view == null) {
                                Timber.v("view is null");
                                return;
                            }
                            TipUtil.showNetworkOrServerError(e);
                        }));
    }

    @Override
    public void setAbort() {
        super.setAbort();
        mDefaultRequestHolder.clear();

        // 清除会话焦点
        final long sessionUserId = SessionManager.Session.getSessionUserId(mSession);
        SettingsManager.getInstance().getUserMemorySettings(sessionUserId).setFocusConversationId(-1);
    }

    public void submitGift(@NonNull Gift gift) {
        submitGiftInternal(gift, false);
    }

    private void submitGiftInternal(@NonNull Gift gift, boolean rmBlack) {
        final long targetUserId;
        final String targetUsername;

        {
            ImSingleFragment.ViewImpl view = getView();
            if (view == null) {
                Timber.e("view is null");
                return;
            }
            view.showLoading("加载中");
            targetUserId = view.getTargetUserId();

            CacheUserInfo cacheUserInfo = UserCacheManager.getInstance().getByUserId(targetUserId, false);
            if (cacheUserInfo != null) {
                targetUsername = cacheUserInfo.username;
            } else {
                targetUsername = null;
            }
        }

        mDefaultRequestHolder.set(
                CommonHttpApi.sendGift(gift.giftId, targetUserId, targetUsername, rmBlack)
                        .map(input -> {
                            ImSendMessageHelper.sendGiftMessage(
                                    targetUserId,
                                    input.msgId,
                                    gift.giftId,
                                    gift.giftName,
                                    gift.desc,
                                    gift.kprice,
                                    gift.thumb,
                                    gift.animationUrl
                            );
                            return input;
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(input -> {
                            ImSingleFragment.ViewImpl view = getView();
                            if (view == null) {
                                Timber.e("view is null");
                                return;
                            }
                            view.hideLoading();
                            view.hideAllSoftKeyboard();
                        }, e -> {
                            ImSingleFragment.ViewImpl view = getView();
                            if (view == null) {
                                Timber.e("view is null");
                                return;
                            }
                            view.hideLoading();
                            Activity innerActivity = view.getActivity();
                            if (innerActivity == null) {
                                Timber.e(Constants.Tip.ACTIVITY_NULL);
                                return;
                            }

                            ApiException apiException = TipUtil.findApiException(e);
                            if (apiException != null) {
                                if (apiException.getCode() == 2015) {
                                    // 提示黑名单
                                    new SimpleContentConfirmDialog(
                                            innerActivity,
                                            apiException.getMessage())
                                            .setOnBtnRightClickListener(() -> submitGiftInternal(gift, true))
                                            .show();
                                    return;
                                }

                                if (apiException.getCode() == 2014) {
                                    // 扩币不足
                                    ToastUtils.showToast("当前扩币不足");
                                    RechargeManager.getInstance().recharge(gift.kprice);
                                    return;
                                }

                                if (apiException.getCode() == 2017) {
                                    // 抱歉，你的"礼物"功能已被禁止使用
                                    new SimpleTitleContentNoticeDialog(
                                            innerActivity,
                                            innerActivity.findViewById(Window.ID_ANDROID_CONTENT),
                                            "无法送礼物",
                                            apiException.getMessage(),
                                            "我知道了")
                                            .show();
                                    return;
                                }

                                if (apiException.getCode() >= 0) {
                                    TipUtil.showNetworkOrServerError(e, "当前网络不佳，请稍后查看赠送结果");
                                    return;
                                }
                            }

                            TipUtil.show("当前网络不佳，请稍后查看赠送结果");
                        }));
    }

}
