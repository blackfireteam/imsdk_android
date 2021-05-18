package com.masonsoft.imsdk.core.observable;

import com.masonsoft.imsdk.util.WeakObservable;

/**
 * 获取会话列表
 */
public class FetchConversationListObservable extends WeakObservable<FetchConversationListObservable.FetchConversationListObserver> {

    public static final FetchConversationListObservable DEFAULT = new FetchConversationListObservable();

    public interface FetchConversationListObserver {
        void onConversationListFetchedLoading();

        void onConversationListFetchedSuccess();

        void onConversationListFetchedError(int errorCode, String errorMessage);
    }

    public void notifyConversationListFetchedLoading() {
        forEach(FetchConversationListObserver::onConversationListFetchedLoading);
    }

    public void notifyConversationListFetchedSuccess() {
        forEach(FetchConversationListObserver::onConversationListFetchedSuccess);
    }

    public void notifyConversationListFetchedError(int errorCode, String errorMessage) {
        forEach(fetchConversationListObserver -> fetchConversationListObserver.onConversationListFetchedError(errorCode, errorMessage));
    }

}
