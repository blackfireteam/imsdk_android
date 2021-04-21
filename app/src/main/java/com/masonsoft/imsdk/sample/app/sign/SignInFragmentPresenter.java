package com.masonsoft.imsdk.sample.app.sign;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.core.message.packet.SignInMessagePacket;
import com.masonsoft.imsdk.core.message.packet.SignOutMessagePacket;
import com.masonsoft.imsdk.core.observable.SessionTcpClientObservable;
import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.sample.LocalSettingsManager;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.api.DefaultApi;

import io.github.idonans.core.thread.Threads;
import io.github.idonans.dynamic.DynamicPresenter;
import io.github.idonans.lang.DisposableHolder;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SignInFragmentPresenter extends DynamicPresenter<SignInFragment.ViewImpl> {

    private final DisposableHolder mRequestHolder = new DisposableHolder();

    public SignInFragmentPresenter(SignInFragment.ViewImpl view) {
        super(view);
        SessionTcpClientObservable.DEFAULT.registerObserver(mSessionTcpClientObserver);
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final SessionTcpClientObservable.SessionTcpClientObserver mSessionTcpClientObserver = new SessionTcpClientObservable.SessionTcpClientObserver() {

        private boolean isSameSessionTcpClient(@NonNull SessionTcpClient sessionTcpClient) {
            final IMSessionManager.SessionTcpClientProxy proxy = IMSessionManager.getInstance().getSessionTcpClientProxy();
            return proxy != null && proxy.getSessionTcpClient() == sessionTcpClient;
        }

        @Override
        public void onConnectionStateChanged(@NonNull SessionTcpClient sessionTcpClient) {
            if (!isSameSessionTcpClient(sessionTcpClient)) {
                return;
            }

            if (sessionTcpClient.isConnectFail()) {
                // 长连接失败
                Threads.postUi(() -> {
                    final SignInFragment.ViewImpl view = getView();
                    if (view != null) {
                        view.onConnectionFail();
                    }
                });
            }
        }

        @Override
        public void onSignInStateChanged(@NonNull SessionTcpClient sessionTcpClient, @NonNull SignInMessagePacket messagePacket) {
            if (messagePacket.isSignIn()) {
                // 登录成功
                Threads.postUi(() -> {
                    final SignInFragment.ViewImpl view = getView();
                    if (view != null) {
                        view.onSignInSuccess();
                    }
                });
                return;
            }

            if (messagePacket.isEnd()) {
                // 登录失败
                Threads.postUi(() -> {
                    final SignInFragment.ViewImpl view = getView();
                    if (view != null) {
                        view.onSignInFail((int) messagePacket.getErrorCode(), messagePacket.getErrorMessage());
                    }
                });
            }
        }

        @Override
        public void onSignOutStateChanged(@NonNull SessionTcpClient sessionTcpClient, @NonNull SignOutMessagePacket messagePacket) {
        }
    };

    public void requestSignIn(String phone) {
        mRequestHolder.set(Single.just("")
                .map(input -> DefaultApi.getImToken(phone))
                .map(init -> {
                    final LocalSettingsManager.Settings settings = LocalSettingsManager.getInstance().getSettings();
                    settings.imToken = init.token;
                    LocalSettingsManager.getInstance().setSettings(settings);
                    IMSessionManager.getInstance().setSession(settings.createSession());
                    return init;
                })
                .map(token -> IMSessionManager.getInstance().getSessionUserIdWithBlockOrTimeout())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ignore -> {
                    final SignInFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }
                    final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
                    if (sessionUserId > 0) {
                        view.onSignInSuccess();
                    } else {
                        // TODO
                        // view.onSignInFail();
                    }
                }, e -> {
                    SampleLog.e(e);
                    final SignInFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }
                    // TODO
                    // view.onSignInFail();
                }));

    }

}
