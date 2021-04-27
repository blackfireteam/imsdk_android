package com.masonsoft.imsdk.core.observable;

import com.masonsoft.imsdk.util.WeakObservable;

/**
 * 获取历史消息
 */
public class FetchMessageHistoryObservable extends WeakObservable<FetchMessageHistoryObservable.FetchMessageHistoryObserver> {

    public static final FetchMessageHistoryObservable DEFAULT = new FetchMessageHistoryObservable();

    public interface FetchMessageHistoryObserver {
        void onMessageHistoryFetchedLoading(long sign);

        void onMessageHistoryFetchedSuccess(long sign);

        void onMessageHistoryFetchedError(long sign, int errorCode, String errorMessage);
    }

    public void notifyMessageHistoryFetchedLoading(long sign) {
        forEach(fetchMessageHistoryObserver -> fetchMessageHistoryObserver.onMessageHistoryFetchedLoading(sign));
    }

    public void notifyMessageHistoryFetchedSuccess(long sign) {
        forEach(fetchMessageHistoryObserver -> fetchMessageHistoryObserver.onMessageHistoryFetchedSuccess(sign));
    }

    public void notifyMessageHistoryFetchedError(long sign, int errorCode, String errorMessage) {
        forEach(fetchMessageHistoryObserver -> fetchMessageHistoryObserver.onMessageHistoryFetchedError(sign, errorCode, errorMessage));
    }

}
