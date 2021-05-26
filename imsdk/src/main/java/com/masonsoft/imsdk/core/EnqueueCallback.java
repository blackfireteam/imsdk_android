package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;

/**
 * 发送消息的入队回调<br>
 * 对于指令消息：表示消息已经进入到发送队列<br>
 * 对于会话消息(聊天消息)：表示消息已经进入到发送队列，并且已经写入数据库.<br>
 */
public interface EnqueueCallback<T extends EnqueueMessage> {

    ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////

    /**
     * 非法的会话用户 id.
     */
    int ERROR_CODE_INVALID_SESSION_USER_ID = 2;

    /**
     * 非法的发送者用户 id.
     */
    int ERROR_CODE_INVALID_FROM_USER_ID = 3;

    /**
     * 非法的接收者用户 id.
     */
    int ERROR_CODE_INVALID_TO_USER_ID = 4;

    /**
     * 非法的消息 id.
     */
    int ERROR_CODE_INVALID_MESSAGE_ID = 5;

    /**
     * 非法的消息 seq.
     */
    int ERROR_CODE_INVALID_MESSAGE_SEQ = 6;

    /**
     * 非法的消息时间.
     */
    int ERROR_CODE_INVALID_MESSAGE_TIME = 7;

    /**
     * 非法的消息发送状态
     */
    int ERROR_CODE_INVALID_MESSAGE_SEND_STATUS = 8;

    /**
     * 消息已撤回
     */
    int ERROR_CODE_MESSAGE_ALREADY_REVOKE = 9;

    /**
     * 非法的会话 id.
     */
    int ERROR_CODE_INVALID_CONVERSATION_ID = 10;

    /**
     * 非法的消息 sign.
     */
    int ERROR_CODE_INVALID_MESSAGE_SIGN = 11;

    //////////////////////////////////////////
    //////////////////////////////////////////
    /**
     * 文字消息，文字内容未设置
     */
    int ERROR_CODE_TEXT_MESSAGE_TEXT_UNSET = 100;

    /**
     * 文字消息，文字内容为空.
     */
    int ERROR_CODE_TEXT_MESSAGE_TEXT_EMPTY = 101;

    /**
     * 文字消息，文字内容过长
     */
    int ERROR_CODE_TEXT_MESSAGE_TEXT_TOO_LARGE = 102;

    //////////////////////////////////////////
    //////////////////////////////////////////
    /**
     * 图片消息，图片地址未设置
     */
    int ERROR_CODE_IMAGE_MESSAGE_IMAGE_PATH_UNSET = 200;

    /**
     * 图片消息，图片地址非法
     */
    int ERROR_CODE_IMAGE_MESSAGE_IMAGE_PATH_INVALID = 201;

    /**
     * 图片消息，图片文件太大
     */
    int ERROR_CODE_IMAGE_MESSAGE_IMAGE_FILE_SIZE_TOO_LARGE = 202;

    /**
     * 图片格式不支持
     */
    int ERROR_CODE_IMAGE_MESSAGE_IMAGE_FORMAT_NOT_SUPPORT = 203;

    /**
     * 图片消息，图片宽度或者高度不合法
     */
    int ERROR_CODE_IMAGE_MESSAGE_IMAGE_WIDTH_OR_HEIGHT_INVALID = 204;

    //////////////////////////////////////////
    //////////////////////////////////////////
    /**
     * 语音消息，语音地址未设置
     */
    int ERROR_CODE_AUDIO_MESSAGE_AUDIO_PATH_UNSET = 300;

    /**
     * 语音消息，语音地址非法
     */
    int ERROR_CODE_AUDIO_MESSAGE_AUDIO_PATH_INVALID = 301;

    /**
     * 语音消息，语音文件太大
     */
    int ERROR_CODE_AUDIO_MESSAGE_AUDIO_FILE_SIZE_TOO_LARGE = 302;

    /**
     * 语音消息，语音时长非法
     */
    int ERROR_CODE_AUDIO_MESSAGE_AUDIO_DURATION_INVALID = 303;

    //////////////////////////////////////////
    //////////////////////////////////////////
    /**
     * 视频消息，视频地址未设置
     */
    int ERROR_CODE_VIDEO_MESSAGE_VIDEO_PATH_UNSET = 400;

    /**
     * 视频消息，视频地址非法
     */
    int ERROR_CODE_VIDEO_MESSAGE_VIDEO_PATH_INVALID = 401;

    /**
     * 视频消息，视频文件太大
     */
    int ERROR_CODE_VIDEO_MESSAGE_VIDEO_FILE_SIZE_TOO_LARGE = 402;

    /**
     * 视频消息，视频时长非法
     */
    int ERROR_CODE_VIDEO_MESSAGE_VIDEO_DURATION_INVALID = 403;

    /**
     * 视频消息，视频封面图未设置
     */
    int ERROR_CODE_VIDEO_MESSAGE_VIDEO_THUMB_PATH_UNSET = 404;

    /**
     * 视频消息，视频封面图非法
     */
    int ERROR_CODE_VIDEO_MESSAGE_VIDEO_THUMB_PATH_INVALID = 405;

    /**
     * 视频消息，视频封面文件太大
     */
    int ERROR_CODE_VIDEO_MESSAGE_VIDEO_THUMB_FILE_SIZE_TOO_LARGE = 406;

    /**
     * 视频消息，视频宽度或者高度不合法
     */
    int ERROR_CODE_VIDEO_MESSAGE_VIDEO_WIDTH_OR_HEIGHT_INVALID = 407;

    /**
     * 视频格式不支持
     */
    int ERROR_CODE_VIDEO_MESSAGE_VIDEO_FORMAT_NOT_SUPPORT = 408;

    /**
     * 视频封面图格式不支持
     */
    int ERROR_CODE_VIDEO_MESSAGE_VIDEO_THUMB_FORMAT_NOT_SUPPORT = 409;

    //////////////////////////////////////////
    //////////////////////////////////////////
    ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////

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
     *
     * @param errorCode 错误码，见 {@linkplain EnqueueCallback}.ERROR_CODE_* 定义, 例如：{@linkplain EnqueueCallback#ERROR_CODE_INVALID_SESSION_USER_ID}
     */
    void onEnqueueFail(@NonNull T enqueueMessage, int errorCode, String errorMessage);
}
