package com.masonsoft.imsdk.core.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMMessageQueueManager;
import com.masonsoft.imsdk.core.NettyTcpClient;
import com.masonsoft.imsdk.core.ProtoByteMessage;
import com.masonsoft.imsdk.core.message.ProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.message.packet.MessagePacket;
import com.masonsoft.imsdk.core.message.packet.PingMessagePacket;
import com.masonsoft.imsdk.core.message.packet.SignInMessagePacket;
import com.masonsoft.imsdk.core.message.packet.SignOutMessagePacket;
import com.masonsoft.imsdk.core.observable.MessagePacketStateObservable;
import com.masonsoft.imsdk.core.observable.SessionObservable;
import com.masonsoft.imsdk.core.observable.SessionTcpClientObservable;
import com.masonsoft.imsdk.lang.MultiProcessor;
import com.masonsoft.imsdk.util.Objects;
import com.masonsoft.imsdk.util.Preconditions;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import io.github.idonans.core.Charsets;

/**
 * 维护长连接的可用性，包括在长连接上进行 Session 认证，发送心跳等。
 *
 * @since 1.0
 */
public class SessionTcpClient extends NettyTcpClient {

    private static final String AES_IV = "3101238945674526";

    @NonNull
    private final Session mSession;
    @SuppressWarnings("FieldCanBeLocal")
    private final SessionObservable.SessionObserver mSessionObserver;

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

    // 登录消息包
    private final SignInMessagePacket mSignInMessagePacket;
    // 退出登录消息包
    private final SignOutMessagePacket mSignOutMessagePacket;

    // 监听登录消息包的状态变化
    @SuppressWarnings("FieldCanBeLocal")
    private final MessagePacketStateObservable.MessagePacketStateObserver mSignInMessagePacketStateObserver;
    // 监听退出登录消息包的状态变化
    @SuppressWarnings("FieldCanBeLocal")
    private final MessagePacketStateObservable.MessagePacketStateObserver mSignOutMessagePacketStateObserver;

    /**
     * 用来处理服务器返回的与登录，退出登录相关的消息
     */
    private final MultiProcessor<ProtoByteMessageWrapper> mLocalMessageProcessor;

    public SessionTcpClient(@NonNull Session session) {
        mSession = session;

        mSignInMessagePacket = SignInMessagePacket.create(mSession.getToken());
        mSignOutMessagePacket = SignOutMessagePacket.create();

        mSignInMessagePacketStateObserver = (packet, oldState, newState) -> {
            // 登录的消息包发生状态变化
            SessionTcpClientObservable.DEFAULT.notifySignInStateChanged(this, (SignInMessagePacket) packet);
        };
        mSignInMessagePacket.getMessagePacketStateObservable().registerObserver(mSignInMessagePacketStateObserver);
        mSignOutMessagePacketStateObserver = (packet, oldState, newState) -> {
            // 退出登录的消息包发生状态变化
            SessionTcpClientObservable.DEFAULT.notifySignOutStateChanged(this, (SignOutMessagePacket) packet);
        };
        mSignOutMessagePacket.getMessagePacketStateObservable().registerObserver(mSignOutMessagePacketStateObserver);

        mLocalMessageProcessor = new MultiProcessor<>();
        mLocalMessageProcessor.addLastProcessor(mSignInMessagePacket);
        mLocalMessageProcessor.addLastProcessor(mSignOutMessagePacket);

        mSessionObserver = new SessionObservable.SessionObserver() {
            @Override
            public void onSessionChanged() {
                validateSession();
            }

            @Override
            public void onSessionUserIdChanged() {
            }
        };
        SessionObservable.DEFAULT.registerObserver(mSessionObserver);
    }

    public void connect() {
        validateSession();

        if (getState() == STATE_IDLE) {
            connect(mSession.getTcpHost(), mSession.getTcpPort());
        }
    }

    @NonNull
    public SignInMessagePacket getSignInMessagePacket() {
        return mSignInMessagePacket;
    }

    @NonNull
    public SignOutMessagePacket getSignOutMessagePacket() {
        return mSignOutMessagePacket;
    }

    @NonNull
    public MultiProcessor<ProtoByteMessageWrapper> getLocalMessageProcessor() {
        return mLocalMessageProcessor;
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

    /*
    @Override
    protected byte[] encryptMessage(int messageType, byte[] messageData) {
        final String aesKey = mSession.getAesKey();

        // 请求登录的消息需要加密
        if (messageType == ProtoByteMessage.Type.IM_LOGIN) {
            return crypt(messageData, aesKey, true);
        }

        return super.encryptMessage(messageType, messageData);
    }
    */

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
            Preconditions.checkNotNull(key);
            //noinspection CharsetObjectCanBeUsed
            final AlgorithmParameterSpec ivSpec = new IvParameterSpec(AES_IV.getBytes(Charsets.UTF8));
            //noinspection CharsetObjectCanBeUsed
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
        // 在已经登录的情况下才发送心跳包
        if (isOnline()) {
            sendMessageQuietly(PingMessagePacket.create().getProtoByteMessage());
        }
    }

    /**
     * 当前长连接是否在线(长连接已建立并且已经登录成功)
     */
    public boolean isOnline() {
        return SessionValidator.isValid(mSession)
                && getState() == STATE_CONNECTED
                && mSignInMessagePacket.isSignIn();
    }

    /**
     * 判断当前长连接是否已经链接失败
     *
     * @return
     */
    public boolean isConnectFail() {
        if (!SessionValidator.isValid(mSession)) {
            return true;
        }
        return getState() == STATE_CLOSED;
    }

    @NonNull
    public Session getSession() {
        return mSession;
    }

    /**
     * 获取当前 token 对应的用户 id
     */
    public long getSessionUserId() {
        return mSignInMessagePacket.getSessionUserId();
    }

    @Override
    protected void sendMessage(@NonNull ProtoByteMessage protoByteMessage) throws Throwable {
        // 在发送长连接消息之前，检查当前 Session 的状态
        validateSession();

        synchronized (mSession) {
            super.sendMessage(protoByteMessage);
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

        //noinspection UnnecessaryLocalVariable
        final Session session = mSession;
        if (session.isPendingSignOut()) {
            signOut();
        } else {
            // 发送认证信息
            signIn();
        }
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

    public void sendMessagePacketQuietly(@NonNull final MessagePacket messagePacket) {
        this.sendMessagePacketQuietly(messagePacket, true);
    }

    protected void sendMessagePacketQuietly(@NonNull final MessagePacket messagePacket, boolean requireSignIn) {
        synchronized (mSession) {
            if (requireSignIn && !isOnline()) {
                IMLog.e(new IllegalStateException("current is offline, abort send message."));
                return;
            }

            if (messagePacket.getState() != MessagePacket.STATE_IDLE) {
                IMLog.e(new IllegalStateException("require state STATE_IDLE"), messagePacket.toString());
                return;
            }

            messagePacket.moveToState(MessagePacket.STATE_GOING);
            final boolean writeSuccess = sendMessageQuietly(messagePacket.getProtoByteMessage());
            if (writeSuccess) {
                messagePacket.moveToState(MessagePacket.STATE_WAIT_RESULT);
            } else {
                IMLog.e(
                        new IllegalStateException("current tcp state or connection is not ready or active"),
                        "tcp state:%s",
                        stateToString(getState())
                );
                messagePacket.moveToState(MessagePacket.STATE_FAIL);
            }
        }
    }

    @Override
    protected void onMessageReceived(@NonNull ProtoByteMessage protoByteMessage) {
        super.onMessageReceived(protoByteMessage);

        // 在接收到长连接消息时，检查当前 Session 的状态
        validateSession();

        synchronized (mSession) {
            if (getState() != STATE_CONNECTED) {
                IMLog.e(
                        new IllegalStateException(Objects.defaultObjectTag(SessionTcpClient.this)
                                + " message received, current tcp state or connection is not ready or active"),
                        "tcp state:%s, message:%s",
                        stateToString(getState()),
                        protoByteMessage.toString()
                );
                return;
            }

            final ProtoByteMessageWrapper protoByteMessageWrapper = new ProtoByteMessageWrapper(protoByteMessage);

            // 优先本地消费(直接消费，快速响应)
            if (mLocalMessageProcessor.doProcess(protoByteMessageWrapper)) {
                return;
            }

            // 其它消息，分发给消息队列处理
            // 需要是已登录状态
            final long sessionUserId = mSignInMessagePacket.getSessionUserId();
            if (!mSignInMessagePacket.isSignIn()) {
                // 当前没有正确登录，但是收到了意外地消息
                IMLog.e(new IllegalStateException(
                        Objects.defaultObjectTag(SessionTcpClient.this) +
                                " is not sign in, but received message"), "protoByteMessageWrapper:%s", protoByteMessageWrapper);
                return;
            }
            IMMessageQueueManager.getInstance().enqueueReceivedMessage(new SessionProtoByteMessageWrapper(sessionUserId, protoByteMessageWrapper));
        }
    }

    /**
     * 登录
     */
    private void signIn() {
        IMLog.v(Objects.defaultObjectTag(this) + " signIn");
        sendMessagePacketQuietly(mSignInMessagePacket, false);
    }

    /**
     * 退出登录
     */
    public void signOut() {
        IMLog.v(Objects.defaultObjectTag(this) + " signOut");
        sendMessagePacketQuietly(mSignOutMessagePacket, false);
    }

    @Override
    protected void onStateChanged(int oldState, int newState) {
        super.onStateChanged(oldState, newState);

        SessionTcpClientObservable.DEFAULT.notifyConnectionStateChanged(this);
    }

}
