package com.masonsoft.imsdk.sample.uniontype;

import com.idonans.dynamic.uniontype.loadingstatus.UnionTypeLoadingStatus;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageImageReceivedViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageImageSendViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageTextReceivedViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageTextSendViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.ImagePickerBucketViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.ImagePickerGridViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.ImagePickerPagerViewHolder;

public class UnionTypeMapperImpl extends UnionTypeLoadingStatus {

    private static int sNextUnionType = 1;
    public static final int UNION_TYPE_IM_MESSAGE_TEXT_RECEIVED = sNextUnionType++; // 聊天消息-接收的文字
    public static final int UNION_TYPE_IM_MESSAGE_TEXT_SEND = sNextUnionType++; // 聊天消息-发送的文字
    public static final int UNION_TYPE_APP_IMPL_IMAGE_PICKER_GRID = sNextUnionType++; // 图片选择器 Grid 视图中的一个 item
    public static final int UNION_TYPE_APP_IMPL_IMAGE_PICKER_BUCKET = sNextUnionType++; // 图片选择器 bucket 视图中的一个 item
    public static final int UNION_TYPE_APP_IMPL_IMAGE_PICKER_PAGER = sNextUnionType++; // 图片选择器 Pager 视图中的一个 item
    public static final int UNION_TYPE_APP_IMPL_IM_MESSAGE_IMAGE_RECEIVED = sNextUnionType++; // 聊天消息-接收的图片
    public static final int UNION_TYPE_APP_IMPL_IM_MESSAGE_IMAGE_SEND = sNextUnionType++; // 聊天消息-发送的图片

    public UnionTypeMapperImpl() {
        put(UNION_TYPE_IM_MESSAGE_TEXT_RECEIVED, IMMessageTextReceivedViewHolder::new);
        put(UNION_TYPE_IM_MESSAGE_TEXT_SEND, IMMessageTextSendViewHolder::new);
        put(UNION_TYPE_APP_IMPL_IMAGE_PICKER_GRID, ImagePickerGridViewHolder::new);
        put(UNION_TYPE_APP_IMPL_IMAGE_PICKER_BUCKET, ImagePickerBucketViewHolder::new);
        put(UNION_TYPE_APP_IMPL_IMAGE_PICKER_PAGER, ImagePickerPagerViewHolder::new);
        put(UNION_TYPE_APP_IMPL_IM_MESSAGE_IMAGE_RECEIVED, IMMessageImageReceivedViewHolder::new);
        put(UNION_TYPE_APP_IMPL_IM_MESSAGE_IMAGE_SEND, IMMessageImageSendViewHolder::new);
    }

}
