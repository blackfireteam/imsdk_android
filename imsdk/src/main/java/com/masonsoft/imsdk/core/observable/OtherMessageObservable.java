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
        void onOtherMessageLoading(@NonNull OtherMessage otherMessage);

        void onOtherMessageSuccess(@NonNull OtherMessage otherMessage);

        void onOtherMessageError(@NonNull OtherMessage otherMessage, long errorCode, String errorMessage);
    }

    public void notifyOtherMessageLoading(@NonNull OtherMessage otherMessage) {
        forEach(otherMessageObserver -> otherMessageObserver.onOtherMessageLoading(otherMessage));
    }

    public void notifyOtherMessageSuccess(@NonNull OtherMessage otherMessage) {
        forEach(otherMessageObserver -> otherMessageObserver.onOtherMessageSuccess(otherMessage));
    }

    public void notifyOtherMessageError(@NonNull OtherMessage otherMessage, long errorCode, String errorMessage) {
        forEach(otherMessageObserver -> otherMessageObserver.onOtherMessageError(otherMessage, errorCode, errorMessage));
    }

}
