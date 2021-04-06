package com.masonsoft.imsdk.sample.uniontype;

import com.idonans.dynamic.uniontype.loadingstatus.UnionTypeLoadingStatus;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageTextReceivedViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageTextSendViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.ImagePicker3BucketViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.ImagePicker3GridViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.ImagePicker3PagerViewHolder;

public class UnionTypeMapperImpl extends UnionTypeLoadingStatus {

    private static int sNextUnionType = 1;
    public static final int UNION_TYPE_IM_MESSAGE_TEXT_RECEIVED = sNextUnionType++; // 聊天消息-接收的文字
    public static final int UNION_TYPE_IM_MESSAGE_TEXT_SEND = sNextUnionType++; // 聊天消息-发送的文字
    public static final int UNION_TYPE_APP_IMPL_IMAGE_PICKER_3_GRID = sNextUnionType++; // 图片选择器3 Grid 视图中的一个 item
    public static final int UNION_TYPE_APP_IMPL_IMAGE_PICKER_3_BUCKET = sNextUnionType++; // 图片选择器3 bucket 视图中的一个 item
    public static final int UNION_TYPE_APP_IMPL_IMAGE_PICKER_3_PAGER = sNextUnionType++; // 图片选择器3 Pager 视图中的一个 item

    public UnionTypeMapperImpl() {
        put(UNION_TYPE_IM_MESSAGE_TEXT_RECEIVED, IMMessageTextReceivedViewHolder::new);
        put(UNION_TYPE_IM_MESSAGE_TEXT_SEND, IMMessageTextSendViewHolder::new);
        put(UNION_TYPE_APP_IMPL_IMAGE_PICKER_3_GRID, ImagePicker3GridViewHolder::new);
        put(UNION_TYPE_APP_IMPL_IMAGE_PICKER_3_BUCKET, ImagePicker3BucketViewHolder::new);
        put(UNION_TYPE_APP_IMPL_IMAGE_PICKER_3_PAGER, ImagePicker3PagerViewHolder::new);
    }

}
