package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

public class IMSessionMessage {

    private final long mSessionUserId;
    @NonNull
    private final IMMessage mIMMessage;
    @NonNull
    private final EnqueueCallback mEnqueueCallback;

    public IMSessionMessage(long sessionUserId, @NonNull IMMessage imMessage, @NonNull EnqueueCallback enqueueCallback) {
        mSessionUserId = sessionUserId;
        mIMMessage = imMessage;
        mEnqueueCallback = enqueueCallback;
    }

    public long getSessionUserId() {
        return mSessionUserId;
    }

    @NonNull
    public IMMessage getIMMessage() {
        return mIMMessage;
    }

    @NonNull
    public EnqueueCallback getEnqueueCallback() {
        return mEnqueueCallback;
    }

    @NonNull
    public String toShortString() {
        return "IMSessionMessage sessionUserId:" + this.mSessionUserId + ", " + this.mIMMessage.toShortString();
    }

    @Override
    @NonNull
    public String toString() {
        return toShortString();
    }

    /**
     * 发送消息的入库回调
     */
    public interface EnqueueCallback {

        ///////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////
        /**
         * 非法的会话用户 id.
         */
        int ERROR_CODE_INVALID_SESSION_USER_ID = 1;

        //////////////////////////////////////////
        //////////////////////////////////////////
        /**
         * 文字消息，文字内容未设置
         */
        int ERROR_CODE_TEXT_MESSAGE_TEXT_UNSET = 10;

        /**
         * 文字消息，文字内容为空.
         */
        int ERROR_CODE_TEXT_MESSAGE_TEXT_EMPTY = 11;

        /**
         * 文字消息，文字内容过长
         */
        int ERROR_CODE_TEXT_MESSAGE_TEXT_TOO_LARGE = 12;

        //////////////////////////////////////////
        //////////////////////////////////////////
        /**
         * 图片消息，图片地址未设置
         */
        int ERROR_CODE_IMAGE_MESSAGE_IMAGE_PATH_UNSET = 20;

        /**
         * 图片消息，图片地址非法
         */
        int ERROR_CODE_IMAGE_MESSAGE_IMAGE_PATH_INVALID = 21;

        /**
         * 图片消息，图片文件太大
         */
        int ERROR_CODE_IMAGE_MESSAGE_IMAGE_FILE_SIZE_TOO_LARGE = 22;

        /**
         * 图片格式不支持
         */
        int ERROR_CODE_IMAGE_MESSAGE_IMAGE_FORMAT_NOT_SUPPORT = 23;

        /**
         * 图片消息，图片宽度或者高度不合法
         */
        int ERROR_CODE_IMAGE_MESSAGE_IMAGE_WIDTH_OR_HEIGHT_INVALID = 24;

        //////////////////////////////////////////
        //////////////////////////////////////////
        /**
         * 语音消息，语音地址未设置
         */
        int ERROR_CODE_AUDIO_MESSAGE_AUDIO_PATH_UNSET = 30;

        /**
         * 语音消息，语音地址非法
         */
        int ERROR_CODE_AUDIO_MESSAGE_AUDIO_PATH_INVALID = 31;

        /**
         * 语音消息，语音文件太大
         */
        int ERROR_CODE_AUDIO_MESSAGE_AUDIO_FILE_SIZE_TOO_LARGE = 32;

        /**
         * 语音消息，语音时长非法
         */
        int ERROR_CODE_AUDIO_MESSAGE_AUDIO_DURATION_INVALID = 33;

        //////////////////////////////////////////
        //////////////////////////////////////////
        /**
         * 视频消息，视频地址未设置
         */
        int ERROR_CODE_VIDEO_MESSAGE_VIDEO_PATH_UNSET = 40;

        /**
         * 视频消息，视频地址非法
         */
        int ERROR_CODE_VIDEO_MESSAGE_VIDEO_PATH_INVALID = 41;

        /**
         * 视频消息，视频文件太大
         */
        int ERROR_CODE_VIDEO_MESSAGE_VIDEO_FILE_SIZE_TOO_LARGE = 42;

        /**
         * 视频消息，视频时长非法
         */
        int ERROR_CODE_VIDEO_MESSAGE_VIDEO_DURATION_INVALID = 43;

        /**
         * 视频消息，视频封面图未设置
         */
        int ERROR_CODE_VIDEO_MESSAGE_VIDEO_THUMB_UNSET = 44;

        /**
         * 视频消息，视频封面图非法
         */
        int ERROR_CODE_VIDEO_MESSAGE_VIDEO_THUMB_INVALID = 45;

        //////////////////////////////////////////
        //////////////////////////////////////////
        /**
         * 未知错误
         */
        int ERROR_CODE_UNKNOWN = 1000;

        ///////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////

        /**
         * 消息持久化成功，此时消息尚未正式发送到网络，但是消息 id，seq 等关键信息已经产生。<br>
         * 仅当收到此入库成功的回调后才适宜清空 UI 上对应的输入框内容。
         */
        void onEnqueueSuccess(@NonNull IMSessionMessage imSessionMessage);

        /**
         * 消息入库失败，此时消息不会出现在会话中。<br>
         * 当收到此入库失败的回调时，应当以适宜的方式提示用户发送的消息格式不正确，或者缺少必要的条件(包括没有找到合法的登录用户信息等)，
         * UI 上对应的输入框不适宜关闭.
         *
         * @param errorCode 错误码，见 {@linkplain EnqueueCallback}.ERROR_CODE_* 定义, 例如：{@linkplain EnqueueCallback#ERROR_CODE_INVALID_SESSION_USER_ID}
         */
        void onEnqueueFail(@NonNull IMSessionMessage imSessionMessage, int errorCode, String errorMessage);
    }

}
