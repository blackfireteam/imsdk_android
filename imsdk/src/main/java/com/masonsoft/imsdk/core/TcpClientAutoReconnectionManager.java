package com.masonsoft.imsdk.core;

import com.masonsoft.imsdk.core.message.packet.MessagePacket;
import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.lang.SafetyRunnable;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.thread.TaskQueue;
import io.github.idonans.core.thread.Threads;

/**
 * 处理长连接的自动重连
 */
public class TcpClientAutoReconnectionManager {

    private static final Singleton<TcpClientAutoReconnectionManager> INSTANCE = new Singleton<TcpClientAutoReconnectionManager>() {
        @Override
        protected TcpClientAutoReconnectionManager create() {
            return new TcpClientAutoReconnectionManager();
        }
    };

    public static TcpClientAutoReconnectionManager getInstance() {
        return INSTANCE.get();
    }

    private final TaskQueue mActionQueue = new TaskQueue(1);

    private TcpClientAutoReconnectionManager() {
    }

    /**
     * 请求重新建立长连接
     */
    public void enqueueReconnect() {
        this.enqueueReconnect(0L);
    }

    /**
     * 在指定延迟之后请求重新建立长连接
     */
    public void enqueueReconnect(long delayMs) {
        Threads.postUi(() -> mActionQueue.enqueue(new SafetyRunnable(new ReconnectionTask())), delayMs);
    }

    private static class ReconnectionTask implements Runnable {
        @Override
        public void run() {
            final IMSessionManager.SessionTcpClientProxy proxy = IMSessionManager.getInstance().getSessionTcpClientProxy();
            if (proxy == null) {
                reconnectNow();
                return;
            }

            if (proxy.isOnline()) {
                // 长连接是处于正常的连接状态，不需要重新连接。
                return;
            }

            final SessionTcpClient sessionTcpClient = proxy.getSessionTcpClient();
            if (sessionTcpClient == null) {
                reconnectNow();
                return;
            }

            final int tcpClientState = sessionTcpClient.getState();
            if (tcpClientState == TcpClient.STATE_IDLE
                    || tcpClientState == TcpClient.STATE_CONNECTING) {
                // 长连接正在连接中，不需要重新连接。
                return;
            }

            if (tcpClientState == TcpClient.STATE_CONNECTED) {
                // 长连接已经建立成功
                final int signInState = sessionTcpClient.getSignInMessagePacket().getState();

                // TODO
                if (signInState != MessagePacket.STATE_FAIL) {
                    // 等待登录的结果中，不需要重新连接
                    return;
                }
            }

            // TODO
        }

        /**
         * 立即重连
         */
        private void reconnectNow() {
            // TODO
        }
    }

}
