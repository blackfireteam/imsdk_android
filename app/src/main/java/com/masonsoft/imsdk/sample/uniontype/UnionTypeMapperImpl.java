package com.masonsoft.imsdk.sample.uniontype;

import com.masonsoft.imsdk.sample.uniontype.viewholder.DiscoverUserViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.HomeSparkViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMConversationViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageImageReceivedViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageImageSendViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageRevokeReceivedViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageRevokeSendViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageTextReceivedViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageTextSendViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageVideoReceivedViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageVideoSendViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageVoiceReceivedViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageVoiceSendViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.MediaPickerBucketViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.MediaPickerGridViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.MediaPickerPagerViewHolder;

import io.github.idonans.dynamic.uniontype.loadingstatus.UnionTypeLoadingStatus;

public class UnionTypeMapperImpl extends UnionTypeLoadingStatus {

    private static int sNextUnionType = 1;
    public static final int UNION_TYPE_IMPL_IM_HOME_SPARK = sNextUnionType++; // 首页中的一条 spark
    public static final int UNION_TYPE_IMPL_IM_DISCOVER_USER = sNextUnionType++; // 发现页中的一个 user 信息
    public static final int UNION_TYPE_IMPL_IM_CONVERSATION = sNextUnionType++; // 会话列表中的一条会话
    public static final int UNION_TYPE_IMPL_IM_MESSAGE_REVOKE_RECEIVED = sNextUnionType++; // 接收到已撤回的消息
    public static final int UNION_TYPE_IMPL_IM_MESSAGE_REVOKE_SEND = sNextUnionType++; // 发送的已撤回的消息
    public static final int UNION_TYPE_IMPL_IM_MESSAGE_TEXT_RECEIVED = sNextUnionType++; // 聊天消息-接收的文字
    public static final int UNION_TYPE_IMPL_IM_MESSAGE_TEXT_SEND = sNextUnionType++; // 聊天消息-发送的文字
    public static final int UNION_TYPE_IMPL_MEDIA_PICKER_GRID = sNextUnionType++; // 媒体选择器 Grid 视图中的一个 item
    public static final int UNION_TYPE_IMPL_MEDIA_PICKER_BUCKET = sNextUnionType++; // 媒体选择器 bucket 视图中的一个 item
    public static final int UNION_TYPE_IMPL_MEDIA_PICKER_PAGER = sNextUnionType++; // 媒体选择器 Pager 视图中的一个 item
    public static final int UNION_TYPE_IMPL_IM_MESSAGE_IMAGE_RECEIVED = sNextUnionType++; // 聊天消息-接收的图片
    public static final int UNION_TYPE_IMPL_IM_MESSAGE_IMAGE_SEND = sNextUnionType++; // 聊天消息-发送的图片
    public static final int UNION_TYPE_IMPL_IM_MESSAGE_VOICE_RECEIVED = sNextUnionType++; // 聊天消息-接收的语音
    public static final int UNION_TYPE_IMPL_IM_MESSAGE_VOICE_SEND = sNextUnionType++; // 聊天消息-发送的语音
    public static final int UNION_TYPE_IMPL_IM_MESSAGE_VIDEO_RECEIVED = sNextUnionType++; // 聊天消息-接收的视频
    public static final int UNION_TYPE_IMPL_IM_MESSAGE_VIDEO_SEND = sNextUnionType++; // 聊天消息-发送的视频

    public UnionTypeMapperImpl() {
        put(UNION_TYPE_IMPL_IM_HOME_SPARK, HomeSparkViewHolder::new);
        put(UNION_TYPE_IMPL_IM_DISCOVER_USER, DiscoverUserViewHolder::new);
        put(UNION_TYPE_IMPL_IM_CONVERSATION, IMConversationViewHolder::new);
        put(UNION_TYPE_IMPL_IM_MESSAGE_REVOKE_RECEIVED, IMMessageRevokeReceivedViewHolder::new);
        put(UNION_TYPE_IMPL_IM_MESSAGE_REVOKE_SEND, IMMessageRevokeSendViewHolder::new);
        put(UNION_TYPE_IMPL_IM_MESSAGE_TEXT_RECEIVED, IMMessageTextReceivedViewHolder::new);
        put(UNION_TYPE_IMPL_IM_MESSAGE_TEXT_SEND, IMMessageTextSendViewHolder::new);
        put(UNION_TYPE_IMPL_MEDIA_PICKER_GRID, MediaPickerGridViewHolder::new);
        put(UNION_TYPE_IMPL_MEDIA_PICKER_BUCKET, MediaPickerBucketViewHolder::new);
        put(UNION_TYPE_IMPL_MEDIA_PICKER_PAGER, MediaPickerPagerViewHolder::new);
        put(UNION_TYPE_IMPL_IM_MESSAGE_IMAGE_RECEIVED, IMMessageImageReceivedViewHolder::new);
        put(UNION_TYPE_IMPL_IM_MESSAGE_IMAGE_SEND, IMMessageImageSendViewHolder::new);
        put(UNION_TYPE_IMPL_IM_MESSAGE_VOICE_RECEIVED, IMMessageVoiceReceivedViewHolder::new);
        put(UNION_TYPE_IMPL_IM_MESSAGE_VOICE_SEND, IMMessageVoiceSendViewHolder::new);
        put(UNION_TYPE_IMPL_IM_MESSAGE_VIDEO_RECEIVED, IMMessageVideoReceivedViewHolder::new);
        put(UNION_TYPE_IMPL_IM_MESSAGE_VIDEO_SEND, IMMessageVideoSendViewHolder::new);
    }

}
