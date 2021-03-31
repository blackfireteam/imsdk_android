package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.core.WeakAbortSignal;
import com.idonans.core.thread.Threads;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.util.Objects;

public class IMSessionMessage {

    private final long mSessionUserId;
    private long mToUserId;
    private int mConversationType = IMConstants.ConversationType.C2C;

    // 是否是重发消息，重发消息时不会重新生成消息 id, seq 与 timeMs
    private final boolean mResend;

    @NonNull
    private final IMMessage mIMMessage;
    @NonNull
    private final EnqueueCallback mEnqueueCallback;

    public IMSessionMessage(
            long sessionUserId,
            long toUserId,
            boolean resend,
            @NonNull IMMessage imMessage,
            @NonNull EnqueueCallback enqueueCallback) {
        mSessionUserId = sessionUserId;
        mToUserId = toUserId;
        mResend = resend;
        mIMMessage = imMessage;
        mEnqueueCallback = enqueueCallback;
    }

    public long getSessionUserId() {
        return mSessionUserId;
    }

    public long getToUserId() {
        return mToUserId;
    }

    public void setToUserId(long toUserId) {
        mToUserId = toUserId;
    }

    public int getConversationType() {
        return mConversationType;
    }

    public void setConversationType(int conversationType) {
        mConversationType = conversationType;
    }

    public boolean isResend() {
        return mResend;
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
        final StringBuilder builder = new StringBuilder();
        builder.append(Objects.defaultObjectTag(this));
        builder.append(" sessionUserId:").append(this.mSessionUserId);
        builder.append(" toUserId:").append(this.mToUserId);
        builder.append(" ").append(this.mIMMessage.toShortString());
        return builder.toString();
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

        /**
         * 非法的发送者用户 id.
         */
        int ERROR_CODE_INVALID_FROM_USER_ID = 2;

        /**
         * 非法的接收者用户 id.
         */
        int ERROR_CODE_INVALID_TO_USER_ID = 3;

        /**
         * 非法的消息 id.
         */
        int ERROR_CODE_INVALID_MESSAGE_ID = 4;

        /**
         * 非法的消息 seq.
         */
        int ERROR_CODE_INVALID_MESSAGE_SEQ = 5;

        /**
         * 非法的消息时间.
         */
        int ERROR_CODE_INVALID_MESSAGE_TIME = 6;

        /**
         * 非法的消息发送状态
         */
        int ERROR_CODE_INVALID_MESSAGE_SEND_STATUS = 7;

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
        int ERROR_CODE_VIDEO_MESSAGE_VIDEO_THUMB_PATH_UNSET = 44;

        /**
         * 视频消息，视频封面图非法
         */
        int ERROR_CODE_VIDEO_MESSAGE_VIDEO_THUMB_PATH_INVALID = 45;

        /**
         * 视频消息，视频封面文件太大
         */
        int ERROR_CODE_VIDEO_MESSAGE_VIDEO_THUMB_FILE_SIZE_TOO_LARGE = 46;

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

    public static class EnqueueCallbackAdapter implements EnqueueCallback {
        @Override
        public void onEnqueueSuccess(@NonNull IMSessionMessage imSessionMessage) {
            IMLog.v("onEnqueueSuccess %s", imSessionMessage);
        }

        @Override
        public void onEnqueueFail(@NonNull IMSessionMessage imSessionMessage, int errorCode, String errorMessage) {
            IMLog.v("onEnqueueFail errorCode:%s, errorMessage:%s, %s", errorCode, errorMessage, imSessionMessage);
        }
    }

    public static class WeakEnqueueCallbackAdapter extends WeakAbortSignal implements EnqueueCallback {

        private final boolean mRunOnUiThread;

        public WeakEnqueueCallbackAdapter(@Nullable EnqueueCallback callback, boolean runOnUiThread) {
            super(callback);
            mRunOnUiThread = runOnUiThread;
        }

        @Nullable
        private EnqueueCallback getEnqueueCallback() {
            return (EnqueueCallback) getObject();
        }

        @Override
        public void onEnqueueSuccess(@NonNull IMSessionMessage imSessionMessage) {
            final Runnable runnable = () -> {
                final EnqueueCallback callback = getEnqueueCallback();
                if (callback != null) {
                    callback.onEnqueueSuccess(imSessionMessage);
                }
            };
            if (mRunOnUiThread) {
                Threads.postUi(runnable);
            } else {
                runnable.run();
            }
        }

        @Override
        public void onEnqueueFail(@NonNull IMSessionMessage imSessionMessage, int errorCode, String errorMessage) {
            final Runnable runnable = () -> {
                final EnqueueCallback callback = getEnqueueCallback();
                if (callback != null) {
                    callback.onEnqueueFail(imSessionMessage, errorCode, errorMessage);
                }
            };
            if (mRunOnUiThread) {
                Threads.postUi(runnable);
            } else {
                runnable.run();
            }
        }
    }

}