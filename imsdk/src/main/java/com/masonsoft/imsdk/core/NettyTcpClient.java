package com.masonsoft.imsdk.core;

import android.util.Log;

import androidx.annotation.Nullable;

import com.idonans.lang.thread.Threads;
import com.idonans.lang.util.IOUtil;
import com.masonsoft.imsdk.util.IMLog;
import com.masonsoft.imsdk.util.ZipUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * 基于 Netty 实现 TCP 长连接
 */
public abstract class NettyTcpClient extends TcpClient {

    /**
     * 长连接写空闲超时时间，用来触发心跳。单位秒。当长连接写空闲超时事件被触发时，客户端将发送一个心跳包。
     */
    private static final int WRITE_IDLE_TIMEOUT_SECONDS = 30;
    /**
     * 长连接读空闲超时时间，用来检测错误的链接。单位秒。当长连接读空闲超时事件被触发时，客户端将主动关闭当前长连接。
     */
    private static final int READ_IDLE_TIMEOUT_SECONDS = WRITE_IDLE_TIMEOUT_SECONDS * 2;

    /**
     * 如果数据长度(Message Data)超过此值，则会触发压缩.
     */
    private static final int MAX_PLAIN_DATA_LENGTH = 20480;
    /**
     * 数据包长度的上限
     */
    private static final int MAX_MESSAGE_DATA_LENGTH = 0xfffff;
    /**
     * 数据类型的最大值
     */
    private static final int MAX_MESSAGE_TYPE_VALUE = 0x3ff;

    @Nullable
    private NioEventLoopGroup mEventLoopGroup;
    @Nullable
    private ChannelHandlerContext mChannelHandlerContext;

    public NettyTcpClient(final String host, final int port) {
        checkState(STATE_IDLE);

        mEventLoopGroup = new NioEventLoopGroup();
        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(mEventLoopGroup);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
        bootstrap.option(ChannelOption.SO_TIMEOUT, 10000);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) {
                if (debugNettyPackage()) {
                    ch.pipeline().addLast(new LoggingHandler(LogLevel.TRACE));
                }
                ch.pipeline()
                        .addLast(
                                new IdleStateHandler(
                                        READ_IDLE_TIMEOUT_SECONDS,
                                        WRITE_IDLE_TIMEOUT_SECONDS,
                                        0,
                                        TimeUnit.SECONDS)
                        )
                        .addLast(new MessageEncoder())
                        .addLast(new MessageDecoder())
                        .addLast(new TimeoutHandler())
                        .addLast(new ConnectionStateHandler())
                        .addLast(new MessageReader());
            }
        });
        moveToState(STATE_CONNECTING);
        Threads.postBackground(() -> {
            try {
                final InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
                bootstrap.remoteAddress(inetSocketAddress);
                final ChannelFuture channelFuture = bootstrap.connect().sync();
                if (!channelFuture.isSuccess()) {
                    channelFuture.channel().close();
                    dispatchDisconnected();
                }
            } catch (Throwable e) {
                IMLog.e(e, "NettyTcpClient connect fail");
                dispatchDisconnected();
            }
        });
    }

    /**
     * 关闭当前长连接
     */
    @Override
    public void close() throws IOException {
        if (mEventLoopGroup != null) {
            mEventLoopGroup.shutdownGracefully();
            mEventLoopGroup = null;
        }

        if (mChannelHandlerContext != null) {
            mChannelHandlerContext.close();
        }
    }

    private static boolean debugNettyPackage() {
        // 当指定的统一日志级别为 Log.VERBOSE 或更低时，才打开 netty 网络通信的日志
        return IMLog.getLogLevel() <= Log.VERBOSE;
    }

    /**
     * 消息编码，将 Message 编码为 byte[] 并输出
     */
    private class MessageEncoder extends MessageToByteEncoder<Message> {
        @Override
        protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
            IMLog.v("MessageEncoder %s", msg);
            out.writeBytes(message2Bytes(msg));
        }
    }

    /**
     * 消息解码，从网络流中读取适当的数据解码为 Message
     */
    protected class MessageDecoder extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            IMLog.v("MessageDecoder");
            bytes2Message(in, out);
        }
    }

    /**
     * 对消息内容进行加密，如果加密成功，返回加密后的 data, 否则返回 null. 默认返回 null(不加密).
     * 如果需要对不同的消息类型选择是否开启加密，可以通过 messageType 判断消息类型。
     *
     * @see Message#getType()
     */
    protected byte[] encryptMessage(int messageType, byte[] messageData) {
        return null;
    }

    /**
     * 对消息内容解密，返回解密后的内容。
     */
    @Nonnull
    protected abstract byte[] decryptMessage(@Nonnull byte[] messageData);

    /**
     * 对消息内容进行压缩，如果进行了压缩，返回压缩后的 data, 否则返回 null. 默认行为是当数据长度
     * 较长时(超过 {@linkplain #MAX_PLAIN_DATA_LENGTH}), 返回压缩后的数据，否则返回 null.
     * 如果需要对不同的消息类型选择是否开启压缩，可以通过 messageType 判断消息类型。
     */
    protected byte[] deflateMessage(int messageType, @Nonnull byte[] messageData) {
        final int length = messageData.length;
        if (length > MAX_PLAIN_DATA_LENGTH) {
            // 数据超过阈值，开启压缩
            final byte[] out = ZipUtil.deflate(messageData);
            IMLog.v("deflateMessage message type:%s, message data length: %s -> %s", messageType, length, out.length);
            return out;
        }
        return null;
    }

    /**
     * 对消息内容解压，返回解压后的内容.
     */
    @Nonnull
    protected byte[] inflateMessage(@Nonnull byte[] messageData) {
        return ZipUtil.inflate(messageData);
    }

    protected byte[] message2Bytes(Message message) {
        final int messageType = message.getType();
        if (messageType < 0) {
            throw new IllegalAccessError("message2Bytes invalid message type " + messageType);
        }
        if (messageType > MAX_MESSAGE_TYPE_VALUE) {
            throw new IllegalAccessError("message2Bytes message type too large " + messageType + ", max:" + MAX_MESSAGE_TYPE_VALUE);
        }

        byte[] tmpMessageData = message.getData();
        byte[] messageData = deflateMessage(messageType, tmpMessageData);
        // 是否压缩 0 or 1
        final int isZip;
        if (messageData != null) {
            // 已压缩
            isZip = 1;
        } else {
            // 没有压缩
            isZip = 0;
            messageData = tmpMessageData;
        }

        tmpMessageData = messageData;
        messageData = encryptMessage(messageType, tmpMessageData);
        // 是否加密 0 or 1
        final int isEncrypt;
        if (messageData != null) {
            // 已加密
            isEncrypt = 1;
        } else {
            // 没有加密
            isEncrypt = 0;
            messageData = tmpMessageData;
        }

        int messageDataLength = messageData.length + 4;
        if (messageDataLength > MAX_MESSAGE_DATA_LENGTH) {
            throw new IllegalAccessError("message2Bytes message data too large " + messageDataLength + ", max:" + MAX_MESSAGE_DATA_LENGTH);
        }

        final int header = (messageDataLength << 12) + (messageType << 2) + (isEncrypt << 1) + isZip;
        final byte[] protoHeader = int2Bytes(header);
        final byte[] protoMessage = new byte[protoHeader.length + messageData.length];
        System.arraycopy(protoHeader, 0, protoMessage, 0, protoHeader.length);
        System.arraycopy(messageData, 0, protoMessage, protoHeader.length, messageData.length);
        return protoMessage;
    }

    protected void bytes2Message(ByteBuf in, List<Object> out) {
        final int PACKET_HEADER_LENGTH = 4;

        while (in.readableBytes() > PACKET_HEADER_LENGTH) {
            in.markReaderIndex();
            byte[] headerBytes = new byte[PACKET_HEADER_LENGTH];
            in.readBytes(headerBytes);
            int header = bytes2Int(headerBytes);
            int messageDataLength = (header >> 12) - 4;
            int messageType = (header & 4095) >> 2;
            int isEncrypt = (header >> 1) & 1;
            int isZip = header & 1;

            // 如果剩下的包体小于数据包的长度，重置读取索引，并返回
            if (in.readableBytes() < messageDataLength) {
                in.resetReaderIndex();


                IMLog.v("bytes2Message header:%s, messageDataLength:%s, messageType:%s, isEncrypt:%s, isZip:%s. wait for more readable bytes...",
                        header, messageDataLength, messageType, isEncrypt, isZip);
                return;
            }

            IMLog.v("bytes2Message header:%s, messageDataLength:%s, messageType:%s, isEncrypt:%s, isZip:%s.",
                    header, messageDataLength, messageType, isEncrypt, isZip);
            ByteBuf dataByteBuf = in.readBytes(messageDataLength);
            int readableLen = dataByteBuf.readableBytes();
            byte[] messageData = new byte[readableLen];
            dataByteBuf.getBytes(dataByteBuf.readerIndex(), messageData, 0, readableLen);

            if (isEncrypt == 1) {
                // 解密
                messageData = decryptMessage(messageData);
            }
            if (isZip == 1) {
                // 解压
                messageData = inflateMessage(messageData);
            }

            final Message message = new Message(messageType, messageData);
            IMLog.v("bytes2Message %s", message);
            out.add(message);
        }
    }

    protected int bytes2Int(byte[] src) {
        ByteBuffer buffer = ByteBuffer.wrap(src);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getInt();
    }

    protected byte[] int2Bytes(int x) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(x);
        return buffer.array();
    }


    /**
     * 处理读写超时。
     */
    private class TimeoutHandler extends ChannelDuplexHandler {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                final IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state() == IdleState.READER_IDLE) {
                    // 读超时
                    onTcpClientReadTimeout(event.isFirst());
                } else if (event.state() == IdleState.WRITER_IDLE) {
                    // 写超时
                    onTcpClientWriteTimeout(event.isFirst());
                }
            }

            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * tcp 读超时。一般行为是强制关闭当前链接，根据具体需求会有所出入。
     *
     * @param first 是否是上一次正常读取数据之后的第一次超时
     */
    protected void onTcpClientReadTimeout(boolean first) {
        IMLog.v("onTcpClientReadTimeout first:%s", first);
    }

    /**
     * tcp 写超时。通常在此时需要发送心跳包。
     *
     * @param first 是否是上一次正常写数据之后的第一次超时
     */
    protected void onTcpClientWriteTimeout(boolean first) {
        IMLog.v("onTcpClientWriteTimeout first:%s", first);
    }

    /**
     * 分析与记录长连接的状态变化
     */
    private class ConnectionStateHandler extends ChannelDuplexHandler {
        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            super.handlerAdded(ctx);

            updateChannelHandlerContext(ctx, false);
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
            super.handlerRemoved(ctx);

            updateChannelHandlerContext(ctx, true);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);

            updateChannelHandlerContext(ctx, false);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);

            updateChannelHandlerContext(ctx, true);
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            super.channelRegistered(ctx);

            updateChannelHandlerContext(ctx, false);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            super.channelUnregistered(ctx);

            updateChannelHandlerContext(ctx, true);
        }
    }

    /**
     * @param forceClosable 在长连接不可用时，是否强制关闭链接。
     */
    protected void updateChannelHandlerContext(@Nullable ChannelHandlerContext context, boolean forceClosable) {
        if (context == null) {
            IMLog.v("updateChannelHandlerContext with null context");
            IOUtil.closeQuietly(this);
            return;
        }

        mChannelHandlerContext = context;
        final Channel channel = context.channel();
        if (channel == null) {
            IMLog.v("updateChannelHandlerContext with null channel");
            IOUtil.closeQuietly(this);
            return;
        }

        if (channel.isActive() && channel.isRegistered()) {
            // 长链接可用
            dispatchConnected();
        } else {
            if (forceClosable) {
                // 长连接不是处于可用状态，允许关闭时，直接发起关闭.
                dispatchDisconnected();
            }
        }
    }

    /**
     * 可能会多次调用
     */
    protected void dispatchConnected() {
        IMLog.v("dispatchConnected");
        moveToState(STATE_CONNECTED);
        onConnected();
    }

    /**
     * 可能会多次调用. 长连接已建立，此时可以正常收发数据.
     */
    protected void onConnected() {
        IMLog.v("onConnected");
    }

    /**
     * 可能会多次调用
     */
    protected void dispatchDisconnected() {
        IMLog.v("dispatchDisconnected");
        IOUtil.closeQuietly(this);
        moveToState(STATE_CLOSED);
        onDisconnected();
    }

    /**
     * 可能会多次调用. 长连接已断开.
     */
    protected void onDisconnected() {
        IMLog.v("onDisconnected");
    }

    private class MessageReader extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            super.channelRead(ctx, msg);

            if (msg instanceof Message) {
                onMessageReceived((Message) msg);
            }
        }
    }

    /**
     * 读取到服务器发送的原始消息
     */
    protected void onMessageReceived(@Nonnull Message message) {
        IMLog.v("onMessageReceived %s", message);
    }

}