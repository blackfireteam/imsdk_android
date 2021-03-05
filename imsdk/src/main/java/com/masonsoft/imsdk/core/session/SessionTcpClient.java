package com.masonsoft.imsdk.core.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.core.Charsets;
import com.masonsoft.imsdk.IMLog;
import com.masonsoft.imsdk.MSIMManager;
import com.masonsoft.imsdk.MSIMSessionManager;
import com.masonsoft.imsdk.core.Message;
import com.masonsoft.imsdk.core.NettyTcpClient;
import com.masonsoft.imsdk.message.MessagePacketSend;
import com.masonsoft.imsdk.message.PingMessagePacket;
import com.masonsoft.imsdk.message.SignInMessagePacket;
import com.masonsoft.imsdk.message.SignOutMessagePacket;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 维护长连接的可用性，包括在长连接上进行 Session 认证，发送心跳等。
 */
public class SessionTcpClient extends NettyTcpClient {

    private static final String AES_KEY_DEFAULT = "10231234545613465926778834590126";
    private static final String AES_IV = "3101238945674526";

    @NonNull
    private final Session mSession;
    @SuppressWarnings("FieldCanBeLocal")
    private final MSIMSessionManager.SessionObserver mSessionObserver;

    /**
     * @see #onConnected()
     * @see #onFirstConnected()
     */
    private boolean mAlreadyConnected;
    /**
     * @see #onDisconnected()
     * @see #onFirstDisconnected()
     */
    private boolean mAlreadyDisconnected;

    private final SignInMessagePacket mSignInMessagePacket;
    private final SignOutMessagePacket mSignOutMessagePacket;

    public SessionTcpClient(@NonNull Session session) {
        super(session.getTcpHost(), session.getTcpPort());
        mSession = session;

        mSignInMessagePacket = SignInMessagePacket.create(mSession.getToken());
        mSignOutMessagePacket = SignOutMessagePacket.create();

        mSessionObserver = this::validateSession;
        MSIMManager.getInstance().getSessionManager().getSessionObservable().registerObserver(mSessionObserver);
        validateSession();
    }

    /**
     * 校验当前长连接上的 Session 状态，如果 Session 已经与当前登录的 Session 不一致，或者当前已经退出登录，则强制中断长连接.
     */
    protected void validateSession() {
        if (SessionValidator.isValid(mSession)) {
            return;
        }

        // 当前 Session 已经失效，强制断开链接
        dispatchDisconnected();
    }

    @Override
    protected byte[] encryptMessage(int messageType, byte[] messageData) {
        final String aesKey = mSession.getAesKey();

        // 请求登录的消息需要加密
        if (messageType == Message.Type.IM_LOGIN) {
            return crypt(messageData, aesKey, true);
        }

        return super.encryptMessage(messageType, messageData);
    }

    @NonNull
    @Override
    protected byte[] decryptMessage(@NonNull byte[] messageData) {
        // 解密消息内容
        final String aesKey = mSession.getAesKey();
        return crypt(messageData, aesKey, false);
    }

    /**
     * 按照约定参数 AES 加密/解密
     */
    @NonNull
    private static byte[] crypt(@NonNull byte[] input, @Nullable String key, boolean encrypt/*是否为加密模式*/) {
        try {
            if (key == null) {
                // 如果没有自定义 key, 则使用默认 key
                key = AES_KEY_DEFAULT;
            }

            final AlgorithmParameterSpec ivSpec = new IvParameterSpec(AES_IV.getBytes(Charsets.UTF8));
            final SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(Charsets.UTF8), "AES");
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", new BouncyCastleProvider());
            cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(input);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void onTcpClientWriteTimeout(boolean first) {
        super.onTcpClientWriteTimeout(first);

        // 发送心跳包
        if (getState() == STATE_CONNECTED) {
            // 在已经认证的情况下才发送心跳包
            if (mSignInMessagePacket.isActive()) {
                sendMessageQuietly(PingMessagePacket.create().getMessage());
            }
        }
    }

    @Override
    public void sendMessage(@NonNull Message message) throws Throwable {
        // 在发送长连接消息之前，检查当前 Session 的状态
        validateSession();

        synchronized (mSession) {
            super.sendMessage(message);
        }
    }

    @Override
    protected void onConnected() {
        super.onConnected();

        if (mAlreadyConnected) {
            return;
        }

        boolean invokeFirstConnected = false;
        // 需要加锁，存在不确定性的多线程多次调用
        synchronized (mSession) {
            if (!mAlreadyConnected) {
                mAlreadyConnected = true;
                invokeFirstConnected = true;
            }
        }
        if (invokeFirstConnected) {
            onFirstConnected();
        }
    }

    /**
     * 成功建立长连接。至多执行一次。
     */
    protected void onFirstConnected() {
        IMLog.v("onFirstConnected");

        // 发送认证信息
        signIn();
    }

    @Override
    protected void onDisconnected() {
        super.onDisconnected();

        if (mAlreadyDisconnected) {
            return;
        }

        boolean invokeFirstDisconnected = false;
        // 需要加锁，存在不确定性的多线程多次调用
        synchronized (mSession) {
            if (!mAlreadyDisconnected) {
                mAlreadyDisconnected = true;
                invokeFirstDisconnected = true;
            }
        }
        if (invokeFirstDisconnected) {
            onFirstDisconnected();
        }
    }

    /**
     * 长连接已断开。至多执行一次。
     */
    protected void onFirstDisconnected() {
        IMLog.v("onFirstDisconnected");
    }

    public void sendMessagePacketQuietly(@NonNull final MessagePacketSend messagePacket) {
        synchronized (mSession) {
            if (messagePacket.getState() != MessagePacketSend.STATE_IDLE) {
                IMLog.e(new IllegalStateException("require state STATE_IDLE"), messagePacket.toString());
                return;
            }

            messagePacket.moveToState(MessagePacketSend.STATE_GOING);
            final boolean writeSuccess = sendMessageQuietly(messagePacket.getMessage());
            if (writeSuccess) {
                messagePacket.moveToState(MessagePacketSend.STATE_WAIT_RESULT);
            } else {
                IMLog.e(
                        new IllegalStateException("current tcp state or connection is not ready or active"),
                        "tcp state:%s",
                        stateToString(getState())
                );
                messagePacket.moveToState(MessagePacketSend.STATE_FAIL);
            }
        }
    }

    @Override
    protected void onMessageReceived(@NonNull Message message) {
        super.onMessageReceived(message);

        // 在接收到长连接消息时，检查当前 Session 的状态
        validateSession();

        synchronized (mSession) {
            if (getState() != STATE_CONNECTED) {
                IMLog.e(
                        new IllegalStateException("message received, current tcp state or connection is not ready or active"),
                        "tcp state:%s, message:%s",
                        stateToString(getState()),
                        message.toString()
                );
                return;
            }

            // TODO 优先过滤本地
        }
    }

    /**
     * 登录
     */
    private void signIn() {
        sendMessagePacketQuietly(mSignInMessagePacket);
    }

    /**
     * 退出登录
     */
    public void signOut() {
        sendMessagePacketQuietly(mSignOutMessagePacket);
    }

}
