package com.masonsoft.imsdk.lang;

import androidx.annotation.Nullable;

import com.masonsoft.imsdk.R;
import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.util.Preconditions;

import java.util.HashMap;
import java.util.Map;

public class GeneralErrorCode {

    /**
     * 成功
     */
    public static final int CODE_SUCCESS = 0;

    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    private static final int FIRST_ERROR_CODE = Integer.MIN_VALUE / 2;
    private static int sNextErrorCode = FIRST_ERROR_CODE;
    /**
     * 未知错误
     */
    public static final int ERROR_CODE_UNKNOWN = sNextErrorCode++;
    /**
     * 其它错误
     */
    public static final int ERROR_CODE_OTHER = sNextErrorCode++;
    /**
     * 超时
     */
    public static final int ERROR_CODE_TIMEOUT = sNextErrorCode++;
    //////////////////////////////////////////////////////////////////////
    /**
     * 目标消息没有找到
     */
    public static final int ERROR_CODE_TARGET_MESSAGE_NOT_FOUND = sNextErrorCode++;
    /**
     * 构建 protoByteMessage 失败
     */
    public static final int ERROR_CODE_MESSAGE_PACKET_BUILD_FAIL = sNextErrorCode++;
    /**
     * sessionTcpClientProxy 为 null
     */
    public static final int ERROR_CODE_SESSION_TCP_CLIENT_PROXY_IS_NULL = sNextErrorCode++;
    /**
     * sessionTcpClientProxy session 无效
     */
    public static final int ERROR_CODE_SESSION_TCP_CLIENT_PROXY_SESSION_INVALID = sNextErrorCode++;
    /**
     * sessionTcpClientProxy 链接错误
     */
    public static final int ERROR_CODE_SESSION_TCP_CLIENT_PROXY_CONNECTION_ERROR = sNextErrorCode++;
    /**
     * sessionTcpClientProxy 未知错误
     */
    public static final int ERROR_CODE_SESSION_TCP_CLIENT_PROXY_ERROR_UNKNOWN = sNextErrorCode++;
    /**
     * messagePacket 发送失败
     */
    public static final int ERROR_CODE_MESSAGE_PACKET_SEND_FAIL = sNextErrorCode++;
    /**
     * messagePacket 发送超时
     */
    public static final int ERROR_CODE_MESSAGE_PACKET_SEND_TIMEOUT = sNextErrorCode++;
    //////////////////////////////////////////////////////////////////////
    /**
     * 非法的会话用户 id.
     */
    public static final int ERROR_CODE_INVALID_SESSION_USER_ID = sNextErrorCode++;
    /**
     * 非法的发送者用户 id.
     */
    public static final int ERROR_CODE_INVALID_FROM_USER_ID = sNextErrorCode++;
    /**
     * 非法的接收者用户 id.
     */
    public static final int ERROR_CODE_INVALID_TO_USER_ID = sNextErrorCode++;
    /**
     * 非法的消息 id.
     */
    public static final int ERROR_CODE_INVALID_MESSAGE_ID = sNextErrorCode++;
    /**
     * 非法的消息 seq.
     */
    public static final int ERROR_CODE_INVALID_MESSAGE_SEQ = sNextErrorCode++;
    /**
     * 非法的消息时间.
     */
    public static final int ERROR_CODE_INVALID_MESSAGE_TIME = sNextErrorCode++;
    /**
     * 非法的消息发送状态
     */
    public static final int ERROR_CODE_INVALID_MESSAGE_SEND_STATUS = sNextErrorCode++;
    /**
     * 消息已撤回
     */
    public static final int ERROR_CODE_MESSAGE_ALREADY_REVOKE = sNextErrorCode++;
    //////////////////////////////////////////////////////////////////////
    /**
     * 文字消息，文字内容未设置
     */
    public static final int ERROR_CODE_TEXT_MESSAGE_TEXT_UNSET = sNextErrorCode++;
    /**
     * 文字消息，文字内容为空.
     */
    public static final int ERROR_CODE_TEXT_MESSAGE_TEXT_EMPTY = sNextErrorCode++;
    /**
     * 文字消息，文字内容过长
     */
    public static final int ERROR_CODE_TEXT_MESSAGE_TEXT_TOO_LARGE = sNextErrorCode++;
    //////////////////////////////////////////////////////////////////////
    /**
     * 图片消息，图片地址未设置
     */
    public static final int ERROR_CODE_IMAGE_MESSAGE_IMAGE_PATH_UNSET = sNextErrorCode++;
    /**
     * 图片消息，图片地址非法
     */
    public static final int ERROR_CODE_IMAGE_MESSAGE_IMAGE_PATH_INVALID = sNextErrorCode++;
    /**
     * 图片消息，图片文件太大
     */
    public static final int ERROR_CODE_IMAGE_MESSAGE_IMAGE_FILE_SIZE_TOO_LARGE = sNextErrorCode++;
    /**
     * 图片格式不支持
     */
    public static final int ERROR_CODE_IMAGE_MESSAGE_IMAGE_FORMAT_NOT_SUPPORT = sNextErrorCode++;
    /**
     * 图片消息，图片宽度或者高度不合法
     */
    public static final int ERROR_CODE_IMAGE_MESSAGE_IMAGE_WIDTH_OR_HEIGHT_INVALID = sNextErrorCode++;
    //////////////////////////////////////////////////////////////////////
    /**
     * 语音消息，语音地址未设置
     */
    public static final int ERROR_CODE_AUDIO_MESSAGE_AUDIO_PATH_UNSET = sNextErrorCode++;
    /**
     * 语音消息，语音地址非法
     */
    public static final int ERROR_CODE_AUDIO_MESSAGE_AUDIO_PATH_INVALID = sNextErrorCode++;
    /**
     * 语音消息，语音文件太大
     */
    public static final int ERROR_CODE_AUDIO_MESSAGE_AUDIO_FILE_SIZE_TOO_LARGE = sNextErrorCode++;
    /**
     * 语音消息，语音时长非法
     */
    public static final int ERROR_CODE_AUDIO_MESSAGE_AUDIO_DURATION_INVALID = sNextErrorCode++;
    //////////////////////////////////////////////////////////////////////
    /**
     * 视频消息，视频地址未设置
     */
    public static final int ERROR_CODE_VIDEO_MESSAGE_VIDEO_PATH_UNSET = sNextErrorCode++;
    /**
     * 视频消息，视频地址非法
     */
    public static final int ERROR_CODE_VIDEO_MESSAGE_VIDEO_PATH_INVALID = sNextErrorCode++;
    /**
     * 视频消息，视频文件太大
     */
    public static final int ERROR_CODE_VIDEO_MESSAGE_VIDEO_FILE_SIZE_TOO_LARGE = sNextErrorCode++;
    /**
     * 视频消息，视频时长非法
     */
    public static final int ERROR_CODE_VIDEO_MESSAGE_VIDEO_DURATION_INVALID = sNextErrorCode++;
    /**
     * 视频消息，视频封面图未设置
     */
    public static final int ERROR_CODE_VIDEO_MESSAGE_VIDEO_THUMB_PATH_UNSET = sNextErrorCode++;
    /**
     * 视频消息，视频封面图非法
     */
    public static final int ERROR_CODE_VIDEO_MESSAGE_VIDEO_THUMB_PATH_INVALID = sNextErrorCode++;
    /**
     * 视频消息，视频封面文件太大
     */
    public static final int ERROR_CODE_VIDEO_MESSAGE_VIDEO_THUMB_FILE_SIZE_TOO_LARGE = sNextErrorCode++;
    //////////////////////////////////////////////////////////////////////
    private static final Map<Integer, Integer> DEFAULT_ERROR_MESSAGE_LOCAL_MAP = new HashMap<>();

    static {
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(CODE_SUCCESS, R.string.msimsdk_general_error_message_success);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_UNKNOWN, R.string.msimsdk_general_error_message_unknown);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_OTHER, R.string.msimsdk_general_error_message_other);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_TIMEOUT, R.string.msimsdk_general_error_message_timeout);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_TARGET_MESSAGE_NOT_FOUND, R.string.msimsdk_general_error_message_target_message_not_found);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_MESSAGE_PACKET_BUILD_FAIL, R.string.msimsdk_general_error_message_message_packet_build_fail);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_SESSION_TCP_CLIENT_PROXY_IS_NULL, R.string.msimsdk_general_error_message_session_tcp_client_proxy_is_null);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_SESSION_TCP_CLIENT_PROXY_SESSION_INVALID, R.string.msimsdk_general_error_message_session_tcp_client_proxy_session_invalid);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_SESSION_TCP_CLIENT_PROXY_CONNECTION_ERROR, R.string.msimsdk_general_error_message_session_tcp_client_proxy_connection_error);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_SESSION_TCP_CLIENT_PROXY_ERROR_UNKNOWN, R.string.msimsdk_general_error_message_session_tcp_client_proxy_error_unknown);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_MESSAGE_PACKET_SEND_FAIL, R.string.msimsdk_general_error_message_message_packet_send_fail);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_MESSAGE_PACKET_SEND_TIMEOUT, R.string.msimsdk_general_error_message_message_packet_send_timeout);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_INVALID_SESSION_USER_ID, R.string.msimsdk_general_error_message_invalid_session_user_id);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_INVALID_FROM_USER_ID, R.string.msimsdk_general_error_message_invalid_from_user_id);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_INVALID_TO_USER_ID, R.string.msimsdk_general_error_message_invalid_to_user_id);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_INVALID_MESSAGE_ID, R.string.msimsdk_general_error_message_invalid_message_id);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_INVALID_MESSAGE_SEQ, R.string.msimsdk_general_error_message_invalid_message_seq);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_INVALID_MESSAGE_TIME, R.string.msimsdk_general_error_message_invalid_message_time);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_INVALID_MESSAGE_SEND_STATUS, R.string.msimsdk_general_error_message_invalid_message_send_status);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_MESSAGE_ALREADY_REVOKE, R.string.msimsdk_general_error_message_message_already_revoke);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_TEXT_MESSAGE_TEXT_UNSET, R.string.msimsdk_general_error_message_text_message_text_unset);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_TEXT_MESSAGE_TEXT_EMPTY, R.string.msimsdk_general_error_message_text_message_text_empty);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_TEXT_MESSAGE_TEXT_TOO_LARGE, R.string.msimsdk_general_error_message_text_message_text_too_large);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_IMAGE_MESSAGE_IMAGE_PATH_UNSET, R.string.msimsdk_general_error_message_image_message_image_path_unset);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_IMAGE_MESSAGE_IMAGE_PATH_INVALID, R.string.msimsdk_general_error_message_image_message_image_path_invalid);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_IMAGE_MESSAGE_IMAGE_FILE_SIZE_TOO_LARGE, R.string.msimsdk_general_error_message_image_message_image_file_size_too_large);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_IMAGE_MESSAGE_IMAGE_FORMAT_NOT_SUPPORT, R.string.msimsdk_general_error_message_image_message_image_format_not_support);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_IMAGE_MESSAGE_IMAGE_WIDTH_OR_HEIGHT_INVALID, R.string.msimsdk_general_error_message_image_message_image_width_or_height_invalid);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_AUDIO_MESSAGE_AUDIO_PATH_UNSET, R.string.msimsdk_general_error_message_audio_message_audio_path_unset);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_AUDIO_MESSAGE_AUDIO_PATH_INVALID, R.string.msimsdk_general_error_message_audio_message_audio_path_invalid);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_AUDIO_MESSAGE_AUDIO_FILE_SIZE_TOO_LARGE, R.string.msimsdk_general_error_message_audio_message_audio_file_size_too_large);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_AUDIO_MESSAGE_AUDIO_DURATION_INVALID, R.string.msimsdk_general_error_message_audio_message_audio_duration_invalid);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_VIDEO_MESSAGE_VIDEO_PATH_UNSET, R.string.msimsdk_general_error_message_video_message_video_path_unset);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_VIDEO_MESSAGE_VIDEO_PATH_INVALID, R.string.msimsdk_general_error_message_video_message_video_path_invalid);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_VIDEO_MESSAGE_VIDEO_FILE_SIZE_TOO_LARGE, R.string.msimsdk_general_error_message_video_message_video_file_size_too_large);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_VIDEO_MESSAGE_VIDEO_DURATION_INVALID, R.string.msimsdk_general_error_message_video_message_video_duration_invalid);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_VIDEO_MESSAGE_VIDEO_THUMB_PATH_UNSET, R.string.msimsdk_general_error_message_video_message_video_thumb_path_unset);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_VIDEO_MESSAGE_VIDEO_THUMB_PATH_INVALID, R.string.msimsdk_general_error_message_video_message_video_thumb_path_invalid);
        DEFAULT_ERROR_MESSAGE_LOCAL_MAP.put(ERROR_CODE_VIDEO_MESSAGE_VIDEO_THUMB_FILE_SIZE_TOO_LARGE, R.string.msimsdk_general_error_message_video_message_video_thumb_file_size_too_large);

        Preconditions.checkArgument(DEFAULT_ERROR_MESSAGE_LOCAL_MAP.size() == sNextErrorCode - FIRST_ERROR_CODE + 1);
    }
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////

    @Nullable
    public static String findDefaultErrorMessage(int errorCode) {
        final Integer resId = DEFAULT_ERROR_MESSAGE_LOCAL_MAP.get(errorCode);
        if (resId != null) {
            return I18nResources.getString(resId);
        }
        return null;
    }

}
