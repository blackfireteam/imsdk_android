package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.masonsoft.imsdk.MSIMManager;
import com.masonsoft.imsdk.MSIMSdkListener;
import com.masonsoft.imsdk.MSIMSessionListener;
import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.lang.GeneralResult;
import com.masonsoft.imsdk.sample.IMTokenOfflineManager;

import io.github.idonans.core.thread.BatchQueue;

public class ConnectionStateDebugView extends AppCompatTextView {

    public ConnectionStateDebugView(@NonNull Context context) {
        this(context, null);
    }

    public ConnectionStateDebugView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConnectionStateDebugView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ConnectionStateDebugView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private final BatchQueue<Boolean> mSessionStateChangedQueue = new BatchQueue<>(true);

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mSessionStateChangedQueue.setConsumer(payloadList -> onSessionStateChanged());
        MSIMManager.getInstance().addSessionListener(mSessionListener);
        MSIMManager.getInstance().addSdkListener(mSdkListener);

        onSessionStateChanged();
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final MSIMSessionListener mSessionListener = new MSIMSessionListener() {
        @Override
        public void onSessionChanged() {
            mSessionStateChangedQueue.add(true);
        }

        @Override
        public void onSessionUserIdChanged() {
            mSessionStateChangedQueue.add(true);
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final MSIMSdkListener mSdkListener = new MSIMSdkListener() {
        @Override
        public void onConnecting() {
            mSessionStateChangedQueue.add(true);
        }

        @Override
        public void onConnectSuccess() {
            mSessionStateChangedQueue.add(true);
        }

        @Override
        public void onConnectClosed() {
            mSessionStateChangedQueue.add(true);
        }

        @Override
        public void onSigningIn() {
            mSessionStateChangedQueue.add(true);
        }

        @Override
        public void onSignInSuccess() {
            mSessionStateChangedQueue.add(true);
        }

        @Override
        public void onSignInFail(@NonNull GeneralResult result) {
            mSessionStateChangedQueue.add(true);
        }

        @Override
        public void onKickedOffline() {
            mSessionStateChangedQueue.add(true);
        }

        @Override
        public void onTokenExpired() {
            mSessionStateChangedQueue.add(true);
        }

        @Override
        public void onSigningOut() {
            mSessionStateChangedQueue.add(true);
        }

        @Override
        public void onSignOutSuccess() {
            mSessionStateChangedQueue.add(true);
        }

        @Override
        public void onSignOutFail(@NonNull GeneralResult result) {
            mSessionStateChangedQueue.add(true);
        }
    };

    private void onSessionStateChanged() {
        setText(buildSessionState());
    }

    private String buildSessionState() {
        //noinspection StringBufferReplaceableByString
        final StringBuilder builder = new StringBuilder();
        builder.append(IMTokenOfflineManager.getInstance().getConnectStateHumanString());
        builder.append("\n");
        builder.append(IMTokenOfflineManager.getInstance().getSignStateHumanString());
        builder.append("\n");
        builder.append(IMSessionManager.getInstance().getSessionTcpClientProxyConfig());
        return builder.toString();
    }

}
