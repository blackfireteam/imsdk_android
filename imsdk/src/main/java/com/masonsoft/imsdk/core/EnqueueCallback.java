package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.lang.GeneralResult;

/**
 * 发送消息的入队回调<br>
 * 对于指令消息：表示消息已经进入到发送队列<br>
 * 对于会话消息(聊天消息)：表示消息已经进入到发送队列，并且已经写入数据库.<br>
 */
@Deprecated
public interface EnqueueCallback<T extends EnqueueMessage> {
    /**
     * 消息入队成功，此时消息尚未正式发送到网络.<br>
     * 对于会话消息(聊天消息)，此时消息已经写入到数据库，消息 id，seq 等关键信息已经产生。<br>
     * 对于会话消息，仅当收到此入队成功的回调后才适宜清空 UI 上对应的输入框内容。<br>
     */
    void onEnqueueSuccess(@NonNull T enqueueMessage);

    /**
     * 消息入库失败，此时消息不会出现在会话中。<br>
     * 当收到此入库失败的回调时，应当以适宜的方式提示用户发送的消息格式不正确，或者缺少必要的条件(包括没有找到合法的登录用户信息等)，
     * UI 上对应的输入框不适宜关闭.
     */
    void onEnqueueFail(@NonNull T enqueueMessage, @NonNull GeneralResult result);
}
