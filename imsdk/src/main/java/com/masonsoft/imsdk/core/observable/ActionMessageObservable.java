package com.masonsoft.imsdk.core.observable;

import com.masonsoft.imsdk.util.WeakObservable;

/**
 * 指令消息
 */
public class ActionMessageObservable extends WeakObservable<ActionMessageObservable.ActionMessageObserver> {

    public static final ActionMessageObservable DEFAULT = new ActionMessageObservable();

    public interface ActionMessageObserver {
        void onActionMessageLoading(long sign);

        void onActionMessageSuccess(long sign);

        void onActionMessageError(long sign, long errorCode, String errorMessage);
    }

    public void notifyActionMessageLoading(long sign) {
        forEach(actionMessageObserver -> actionMessageObserver.onActionMessageLoading(sign));
    }

    public void notifyActionMessageSuccess(long sign) {
        forEach(actionMessageObserver -> actionMessageObserver.onActionMessageSuccess(sign));
    }

    public void notifyActionMessageError(long sign, long errorCode, String errorMessage) {
        forEach(actionMessageObserver -> actionMessageObserver.onActionMessageError(sign, errorCode, errorMessage));
    }

}
