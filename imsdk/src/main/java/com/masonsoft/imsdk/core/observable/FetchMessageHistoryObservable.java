package com.masonsoft.imsdk.core.observable;

import com.masonsoft.imsdk.util.WeakObservable;

/**
 * 获取历史消息
 */
public class FetchMessageHistoryObservable extends WeakObservable<FetchMessageHistoryObservable.FetchMessageHistoryObserver> {

    public static final FetchMessageHistoryObservable DEFAULT = new FetchMessageHistoryObservable();

    public interface FetchMessageHistoryObserver {
        void onMessageHistoryFetched(long sign);

        void onMessageHistoryFetchedError(long sign, long errorCode, String errorMessage);

        void onMessageHistoryFetchedTimeout(long sign);
    }

    public void notifyMessageHistoryFetched(long sign) {
        forEach(fetchMessageHistoryObserver -> fetchMessageHistoryObserver.onMessageHistoryFetched(sign));
    }

    public void notifyMessageHistoryFetchedError(long sign, long errorCode, String errorMessage) {
        forEach(fetchMessageHistoryObserver -> fetchMessageHistoryObserver.onMessageHistoryFetchedError(sign, errorCode, errorMessage));
    }

    public void notifyMessageHistoryFetchedTimeout(long sign) {
        forEach(fetchMessageHistoryObserver -> fetchMessageHistoryObserver.onMessageHistoryFetchedTimeout(sign));
    }

}
