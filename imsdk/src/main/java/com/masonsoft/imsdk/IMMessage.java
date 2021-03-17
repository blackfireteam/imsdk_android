package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMConstants.MessageType;
import com.masonsoft.imsdk.lang.StateProp;

/**
 * 会话中的一条消息
 *
 * @since 1.0
 */
public class IMMessage {

    /**
     * 消息发送或接收时，对应的本地登录用户 id.
     */
    @NonNull
    public final StateProp<Long> sessionUserId = new StateProp<>();

    /**
     * 消息 id, 在一个会话中消息 id 是唯一的.
     */
    @NonNull
    public final StateProp<Long> id = new StateProp<>();

    /**
     * 消息的排序字段, 用于分页读取消息.
     */
    @NonNull
    public final StateProp<Long> seq = new StateProp<>();

    /**
     * 消息的发送方
     */
    @NonNull
    public final StateProp<Long> fromUserId = new StateProp<>();

    /**
     * 消息的接收方
     */
    @NonNull
    public final StateProp<Long> toUserId = new StateProp<>();

    /**
     * 消息产生的时间, 单位毫秒.<br>
     * 如果是本地发送的新消息，该时间是发送时的本地时间.<br>
     * 如果该消息是收到的服务器下发的消息, 并且该消息在本地不存在，则该时间是这一条
     * 服务器下发的消息上的服务器时间。
     */
    @NonNull
    public final StateProp<Long> timeMs = new StateProp<>();

    /**
     * 消息的类型
     *
     * @see MessageType
     */
    @NonNull
    public final StateProp<Integer> type = new StateProp<>();

    /**
     * 消息内容：title
     */
    @NonNull
    public final StateProp<String> title = new StateProp<>();

    /**
     * 消息内容：body
     */
    @NonNull
    public final StateProp<String> body = new StateProp<>();

    /**
     * 消息内容：thumb
     */
    @NonNull
    public final StateProp<String> thumb = new StateProp<>();

    /**
     * 消息内容：width
     */
    @NonNull
    public final StateProp<Integer> width = new StateProp<>();

    /**
     * 消息内容：height
     */
    @NonNull
    public final StateProp<Integer> height = new StateProp<>();

    /**
     * 消息内容：duration
     */
    @NonNull
    public final StateProp<Long> duration = new StateProp<>();

    /**
     * 消息内容：lat
     */
    @NonNull
    public final StateProp<Double> lat = new StateProp<>();

    /**
     * 消息内容：lng
     */
    @NonNull
    public final StateProp<Double> lng = new StateProp<>();

    /**
     * 消息内容：zoom
     */
    @NonNull
    public final StateProp<Integer> zoom = new StateProp<>();

    /**
     * 消息的发送状态
     */
    @NonNull
    public final StateProp<Integer> sendState = new StateProp<>();

    /**
     * 消息的发送进度. 范围 [0, 1]
     */
    @NonNull
    public final StateProp<Float> sendProgress = new StateProp<>();

}
