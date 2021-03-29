package com.masonsoft.imsdk.sample.app.chat;

import android.app.Activity;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.idonans.dynamic.page.PagePresenter;
import com.idonans.dynamic.page.PageView;
import com.idonans.dynamic.page.UnionTypeStatusPageView;
import com.idonans.lang.DisposableHolder;
import com.idonans.lang.thread.Threads;
import com.idonans.lang.util.ViewUtil;
import com.idonans.uniontype.UnionTypeItemObject;
import com.xmqvip.xiaomaiquan.common.Constants;
import com.xmqvip.xiaomaiquan.common.SafetyRunnable;
import com.xmqvip.xiaomaiquan.common.api.CommonHttpApi;
import com.xmqvip.xiaomaiquan.common.entity.cache.CacheUserInfo;
import com.xmqvip.xiaomaiquan.common.entity.format.Gift;
import com.xmqvip.xiaomaiquan.common.eventbus.AddBlackListEvent;
import com.xmqvip.xiaomaiquan.common.eventbus.IMLocalEventConversationChanged;
import com.xmqvip.xiaomaiquan.common.eventbus.SessionChangedEvent;
import com.xmqvip.xiaomaiquan.common.eventpro.EventPro;
import com.xmqvip.xiaomaiquan.common.im.ImConstant;
import com.xmqvip.xiaomaiquan.common.im.ImManager;
import com.xmqvip.xiaomaiquan.common.im.ImSendMessageHelper;
import com.xmqvip.xiaomaiquan.common.im.entity.ImConversation;
import com.xmqvip.xiaomaiquan.common.im.entity.ImMessage;
import com.xmqvip.xiaomaiquan.common.manager.SVGAPlayerQueueManager;
import com.xmqvip.xiaomaiquan.common.manager.SessionManager;
import com.xmqvip.xiaomaiquan.common.manager.SettingsManager;
import com.xmqvip.xiaomaiquan.common.manager.UserCacheManager;
import com.xmqvip.xiaomaiquan.common.net.ApiException;
import com.xmqvip.xiaomaiquan.common.pay.RechargeManager;
import com.xmqvip.xiaomaiquan.common.simpledialog.SimpleContentConfirmDialog;
import com.xmqvip.xiaomaiquan.common.simpledialog.SimpleTitleContentNoticeDialog;
import com.xmqvip.xiaomaiquan.common.uniontype.DataObject;
import com.xmqvip.xiaomaiquan.common.uniontype.UnionTypeViewHolderListeners;
import com.xmqvip.xiaomaiquan.common.uniontypeappimpl.viewholder.ImMessageViewHolder;
import com.xmqvip.xiaomaiquan.common.utils.TipUtil;
import com.xmqvip.xiaomaiquan.common.utils.ToastUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class SingleChatFragmentPresenter extends PagePresenter<UnionTypeItemObject, UnionTypeStatusPageView> {

    private static final boolean DEBUG = false;

    private SessionManager.Session mSession;
    private final long mTargetUserId;
    @Nullable
    private ImConversation mTargetConversation;
    private final int mPageSize = 20;
    private long mFirstMessageId = -1;
    private long mLastMessageId = -1;
    private long mLatestMessageIdOverGiftPlayerQueue = -1;

    private final DisposableHolder mDefaultRequestHolder = new DisposableHolder();

    @UiThread
    public SingleChatFragmentPresenter(@NonNull SingleChatFragment.ViewImpl view) {
        super(view, true, true);
        mSession = SessionManager.getInstance().getSession();
        mTargetUserId = view.getTargetUserId();
        mTargetConversation = ImManager.getInstance().getChatConversationByTargetUserId(mTargetUserId, true);
        if (DEBUG) {
            Timber.v(":mSession:%s, mTargetUserId:%s, mTargetConversation:%s",
                    mSession, mTargetUserId, mTargetConversation);
        }
        if (mTargetConversation == null) {
            Throwable e = new IllegalArgumentException("mTargetConversation is null with mTargetUserId " + mTargetUserId);
            Timber.e(e);
        }

        if (mTargetConversation != null) {
            view.showDraftText(mTargetConversation.draftText);
        }

        if (mTargetConversation != null) {
            mLatestMessageIdOverGiftPlayerQueue = mTargetConversation.lastMsgId;
        }

        SessionChangedEvent.EVENT_PRO.addEventProListener(mSessionChangedEventUiEventProListener);
        IMLocalEventConversationChanged.EVENT_PRO.addEventProListener(mIMLocalEventConversationChangedUiEventProListener);
    }

    @Nullable
    public ImConversation getTargetConversation() {
        return mTargetConversation;
    }

    public long getTargetConversationId() {
        if (mTargetConversation != null) {
            return mTargetConversation.id;
        }
        return -1;
    }

    public long getLatestMessageIdOverGiftPlayerQueue() {
        return mLatestMessageIdOverGiftPlayerQueue;
    }

    public void setLatestMessageIdOverGiftPlayerQueue(long messageId) {
        mLatestMessageIdOverGiftPlayerQueue = messageId;
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
    public ImSingleFragment.ViewImpl getView() {
        return (ImSingleFragment.ViewImpl) super.getView();
    }

    private EventPro.UiEventProListener<SessionChangedEvent> mSessionChangedEventUiEventProListener = event -> {
        if (!SessionManager.Session.isSessionUserIdChanged(mSession, event.newSession)) {
            if (DEBUG) {
                Timber.v("onSessionChangedEvent ignore session user id not changed. mSession:%s, event.newSession:%s",
                        mSession, event.newSession);
            }
            return;
        }

        mSession = event.newSession;

        // 登录 id 变更，页面全部重新加载
        if (DEBUG) {
            Timber.v("onSessionChangedEvent sessionUserId:%s, token:%s",
                    event.newSession == null ? "null" : event.newSession.sessionUserId,
                    event.newSession == null ? "null" : event.newSession.token);
        }
        requestInit(true);
    };

    private EventPro.UiEventProListener<IMLocalEventConversationChanged> mIMLocalEventConversationChangedUiEventProListener = event -> {
        if (SessionManager.Session.isSessionUserIdChanged(mSession, event.sessionUserId)) {
            // 会话不属于当前用户，忽略。
            if (DEBUG) {
                Timber.v("onConversationChangedEvent ignore session user id changed. mSession:%s, event.sessionUserId:%s",
                        mSession, event.sessionUserId);
            }
            return;
        }

        if (mTargetConversation == null) {
            if (DEBUG) {
                Timber.v("onConversationChangedEvent ignore mTargetConversation is null");
            }
            return;
        }

        if (mTargetConversation.id != event.conversationId) {
            if (DEBUG) {
                Timber.v("onConversationChangedEvent ignore, conversationId not equal (%s, %s)", mTargetConversation.id, event.conversationId);
            }
            return;
        }
        Timber.v("onConversationChangedEvent sessionUserId:%s, conversationId:%s", event.sessionUserId, event.conversationId);

        if (mLastMessageId > 0) {
            requestNextPage(true);
        } else {
            requestInit(true);
        }
    };

    private final UnionTypeViewHolderListeners.OnItemClickListener mOnHolderItemClickListener = viewHolder -> {
        ImSingleFragment.ViewImpl view = getView();
        if (view != null) {
            ImMessageViewHolder.Helper.showPreview(viewHolder, view.getTargetUserId(), (holder, targetUserId, finder) -> {
                if (finder.imMessage.msgType == ImConstant.MessageType.MESSAGE_TYPE_GIFT) {
                    // 点击礼物类型的 holder，弹出礼物键盘
                    view.showCustomGiftInputBoard();
                    return true;
                }
                return false;
            });
        }
    };

    private final UnionTypeViewHolderListeners.OnItemLongClickListener mOnHolderItemLongClickListener = viewHolder -> {
        ImSingleFragment.ViewImpl view = getView();
        if (view != null) {
            if (ImMessageViewHolder.Helper.showMenu(viewHolder)) {
                ViewUtil.requestParentDisallowInterceptTouchEvent(viewHolder.itemView);
            }
        }
    };

    @Nullable
    private UnionTypeItemObject createDefault(@Nullable ImMessage imMessage) {
        if (imMessage == null) {
            return null;
        }
        final DataObject<ImMessage> dataObject = new DataObject<>(imMessage)
                .putExtHolderItemClick1(mOnHolderItemClickListener)
                .putExtHolderItemLongClick1(mOnHolderItemLongClickListener);
        return ImMessageViewHolder.Helper.createDefault(dataObject, SessionManager.Session.getSessionUserId(mSession));
    }

    @Nullable
    @Override
    protected SingleSource<Collection<UnionTypeItemObject>> createInitRequest() throws Exception {
        Timber.v("createInitRequest");

        if (mTargetConversation == null) {
            Timber.e("createInitRequest mTargetConversation is null");
            return null;
        }

        final long conversationId = mTargetConversation.id;
        if (DEBUG) {
            Timber.v("createInitRequest for conversationId:%s, pageSize:%s", conversationId, mPageSize);
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

    private void dispatchGiftPlayer(Collection<UnionTypeItemObject> items) {
        if (items == null) {
            return;
        }
        for (UnionTypeItemObject item : items) {
            if (item == null) {
                continue;
            }
            if (item.itemObject instanceof DataObject) {
                if (((DataObject) item.itemObject).object instanceof ImMessage) {
                    dispatchGiftPlayer(((ImMessage) ((DataObject) item.itemObject).object));
                }
            }
        }
    }

    private void dispatchGiftPlayer(final ImMessage imMessage) {
        Threads.postUi(new SafetyRunnable(() -> {
            ImSingleFragment.ViewImpl view = getView();
            if (view == null) {
                Timber.e("view is null");
                return;
            }

            Activity innerActivity = view.getActivity();
            if (innerActivity == null) {
                Timber.e(Constants.Tip.ACTIVITY_NULL);
                return;
            }

            if (imMessage.msgType != ImConstant.MessageType.MESSAGE_TYPE_GIFT) {
                return;
            }

            if (imMessage.id <= getLatestMessageIdOverGiftPlayerQueue()) {
                return;
            }

            setLatestMessageIdOverGiftPlayerQueue(imMessage.id);
            SVGAPlayerQueueManager.getInstance().getQueue(innerActivity).enqueue(imMessage.msgGiftAnim);
        }));
    }

}
