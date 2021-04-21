package com.masonsoft.imsdk.sample.app.signin;

import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.sample.LocalSettingsManager;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.api.ApiResponseException;
import com.masonsoft.imsdk.sample.api.DefaultApi;

import io.github.idonans.dynamic.DynamicPresenter;
import io.github.idonans.lang.DisposableHolder;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SignInFragmentPresenter extends DynamicPresenter<SignInFragment.ViewImpl> {

    private final DisposableHolder mRequestHolder = new DisposableHolder();

    public SignInFragmentPresenter(SignInFragment.ViewImpl view) {
        super(view);
    }

    public void requestToken(String phone) {
        mRequestHolder.set(Single.just("")
                .map(input -> DefaultApi.getImToken(phone))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(init -> {
                    final SignInFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }
                    view.onFetchTokenSuccess(init.token);
                }, e -> {
                    SampleLog.e(e);
                    final SignInFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }

                    if (e instanceof ApiResponseException) {
                        final int errorCode = ((ApiResponseException) e).code;
                        final String errorMessage = ((ApiResponseException) e).message;
                        view.onFetchTokenFail(phone, errorCode, errorMessage);
                        return;
                    }

                    view.onFetchTokenFail(e, phone);
                }));

    }


    public void requestTcpSignIn(String token) {
        mRequestHolder.set(Single.just("")
                .map(input -> {
                    final LocalSettingsManager.Settings settings = LocalSettingsManager.getInstance().getSettings();
                    settings.imToken = token;
                    LocalSettingsManager.getInstance().setSettings(settings);
                    IMSessionManager.getInstance().setSession(settings.createSession());
                    return input;
                })
                .map(input -> IMSessionManager.getInstance().getSessionUserIdWithBlockOrTimeout())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    final SignInFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }
                    final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
                    if (sessionUserId > 0) {
                        view.onTcpSignInSuccess();
                    } else {
                        view.onTcpSignInFail(result);
                    }
                }, e -> {
                    SampleLog.e(e);
                    final SignInFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }

                    view.onTcpSignInFail(e);
                }));

    }

}
