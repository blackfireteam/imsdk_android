package com.masonsoft.imsdk.sample.api;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.masonsoft.imsdk.EnqueueCallbackAdapter;
import com.masonsoft.imsdk.OtherMessage;
import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.core.OtherMessageManager;
import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.core.observable.OtherMessageObservable;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.LocalSettingsManager;
import com.masonsoft.imsdk.sample.entity.ApiResponse;
import com.masonsoft.imsdk.sample.entity.Init;
import com.masonsoft.imsdk.sample.entity.Spark;
import com.masonsoft.imsdk.sample.im.FetchSparkMessagePacket;
import com.masonsoft.imsdk.sample.util.OkHttpClientUtil;
import com.masonsoft.imsdk.sample.util.RequestSignUtil;
import com.masonsoft.imsdk.user.UserInfoManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.subjects.SingleSubject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class DefaultApi {

    private static final long TIMEOUT_MS = 20 * 1000L;

    private DefaultApi() {
    }

    private static OkHttpClient createDefaultApiOkHttpClient() {
        final HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        return OkHttpClientUtil.createDefaultOkHttpClient()
                .newBuilder()
                .addInterceptor(logging)
                .build();
    }

    public static Init getImToken(String phone) {
        final String appSecret = Constants.APP_SECRET;
        final int nonce = (int) (Math.random() * 1000000000);
        final long timestamp = System.currentTimeMillis() / 1000;
        final String sign = RequestSignUtil.calSign(appSecret, nonce, timestamp);

        final Map<String, Object> requestArgs = new HashMap<>();
        requestArgs.put("uid", phone);
        final String requestArgsAsJson = new Gson().toJson(requestArgs);
        final RequestBody requestBody = RequestBody.create(requestArgsAsJson, MediaType.parse("application/json;charset=utf-8"));

        final LocalSettingsManager.Settings settings = LocalSettingsManager.getInstance().getSettings();
        final String url = settings.apiServer + "/user/iminit";
        final Request request = new Request.Builder()
                .addHeader("nonce", String.valueOf(nonce))
                .addHeader("timestamp", String.valueOf(timestamp))
                .addHeader("sig", sign)
                .url(url)
                .post(requestBody)
                .build();

        final OkHttpClient okHttpClient = createDefaultApiOkHttpClient();
        try {
            final Response response = okHttpClient.newCall(request).execute();
            final String json = response.body().string();
            final ApiResponse<Init> apiResponse = new Gson().fromJson(json, new TypeToken<ApiResponse<Init>>() {
            }.getType());

            if (apiResponse.code != 0) {
                throw new ApiResponseException(apiResponse.code, apiResponse.message);
            }

            return apiResponse.data;
        } catch (Throwable e) {
            if (e instanceof ApiResponseException) {
                throw (ApiResponseException) e;
            }
            throw new RuntimeException(e);
        }
    }

    public static List<Spark> getSparks() {
        final SingleSubject<List<Spark>> subject = SingleSubject.create();
        final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
        final long originSign = SignGenerator.next();
        final FetchSparkMessagePacket messagePacket = FetchSparkMessagePacket.create(originSign);
        final OtherMessage otherMessage = new OtherMessage(sessionUserId, messagePacket, new EnqueueCallbackAdapter<OtherMessage>() {
            @Override
            public void onEnqueueFail(@NonNull OtherMessage enqueueMessage, int errorCode, String errorMessage) {
                super.onEnqueueFail(enqueueMessage, errorCode, errorMessage);
                subject.onError(new IllegalArgumentException("errorCode:" + errorCode + ", errorMessage:" + errorMessage));
            }
        });
        final OtherMessageObservable.OtherMessageObserver otherMessageObserver = new OtherMessageObservable.OtherMessageObserver() {
            @Override
            public void onOtherMessageLoading(long sign, @NonNull OtherMessage otherMessage) {
            }

            @Override
            public void onOtherMessageSuccess(long sign, @NonNull OtherMessage otherMessage) {
                if (originSign != sign) {
                    return;
                }

                final List<Spark> sparkList = new ArrayList<>(messagePacket.getSparkList());

                // 存储用户头像与昵称
                for (Spark spark : sparkList) {
                    UserInfoManager.getInstance().updateAvatarAndNickname(spark.userId, spark.avatar, spark.nickname);
                }

                subject.onSuccess(sparkList);
            }

            @Override
            public void onOtherMessageError(long sign, @NonNull OtherMessage otherMessage, long errorCode, String errorMessage) {
                if (originSign != sign) {
                    return;
                }

                subject.onError(new IllegalArgumentException("errorCode:" + errorCode + ", errorMessage:" + errorMessage));
            }
        };
        OtherMessageObservable.DEFAULT.registerObserver(otherMessageObserver);
        OtherMessageManager.getInstance().enqueueOtherMessage(sessionUserId, originSign, otherMessage);
        return subject.timeout(TIMEOUT_MS, TimeUnit.MILLISECONDS).blockingGet();
    }

}
