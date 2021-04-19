package com.masonsoft.imsdk.core.observable;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.OtherMessage;
import com.masonsoft.imsdk.util.WeakObservable;

/**
 * 其它消息
 */
public class OtherMessageObservable extends WeakObservable<OtherMessageObservable.OtherMessageObserver> {

    public static final OtherMessageObservable DEFAULT = new OtherMessageObservable();

    public interface OtherMessageObserver {
        void onOtherMessageLoading(long sign, @NonNull OtherMessage otherMessage);

        void onOtherMessageSuccess(long sign, @NonNull OtherMessage otherMessage);

        void onOtherMessageError(long sign, @NonNull OtherMessage otherMessage, long errorCode, String errorMessage);
    }

    public void notifyOtherMessageLoading(long sign, @NonNull OtherMessage otherMessage) {
        forEach(otherMessageObserver -> otherMessageObserver.onOtherMessageLoading(sign, otherMessage));
    }

    public void notifyOtherMessageSuccess(long sign, @NonNull OtherMessage otherMessage) {
        forEach(otherMessageObserver -> otherMessageObserver.onOtherMessageSuccess(sign, otherMessage));
    }

    public void notifyOtherMessageError(long sign, @NonNull OtherMessage otherMessage, long errorCode, String errorMessage) {
        forEach(otherMessageObserver -> otherMessageObserver.onOtherMessageError(sign, otherMessage, errorCode, errorMessage));
    }

}
