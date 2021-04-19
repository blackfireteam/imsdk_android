package com.masonsoft.imsdk.sample.api;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.EnqueueCallbackAdapter;
import com.masonsoft.imsdk.OtherMessage;
import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.core.OtherMessageManager;
import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.core.observable.OtherMessageObservable;
import com.masonsoft.imsdk.sample.entity.Spark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.subjects.SingleSubject;

public class DefaultApi {

    private static final long TIMEOUT_MS = 20 * 1000L;

    private DefaultApi() {
    }

    public static List<Spark> getSparks() {
        final SingleSubject<List<Spark>> subject = SingleSubject.create();
        final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
        final long sign = SignGenerator.next();
        final FetchSparkMessagePacket messagePacket = FetchSparkMessagePacket.create(sign);
        final OtherMessage otherMessage = new OtherMessage(sessionUserId, messagePacket, new EnqueueCallbackAdapter<OtherMessage>() {
            @Override
            public void onEnqueueFail(@NonNull OtherMessage enqueueMessage, int errorCode, String errorMessage) {
                super.onEnqueueFail(enqueueMessage, errorCode, errorMessage);
                subject.onError(new IllegalArgumentException("errorCode:" + errorCode + ", errorMessage:" + errorMessage));
            }
        });
        final OtherMessageObservable.OtherMessageObserver otherMessageObserver = new OtherMessageObservable.OtherMessageObserver() {
            @Override
            public void onOtherMessageLoading(@NonNull OtherMessage otherMessage) {
            }

            @Override
            public void onOtherMessageSuccess(@NonNull OtherMessage otherMessage) {
                final List<Spark> sparkList = new ArrayList<>(messagePacket.getSparkList());
                subject.onSuccess(sparkList);
            }

            @Override
            public void onOtherMessageError(@NonNull OtherMessage otherMessage, long errorCode, String errorMessage) {
                subject.onError(new IllegalArgumentException("errorCode:" + errorCode + ", errorMessage:" + errorMessage));
            }
        };
        OtherMessageObservable.DEFAULT.registerObserver(otherMessageObserver);
        OtherMessageManager.getInstance().enqueueOtherMessage(sessionUserId, sign, otherMessage);
        return subject.timeout(TIMEOUT_MS, TimeUnit.MILLISECONDS).blockingGet();
    }

}
