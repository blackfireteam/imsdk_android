package com.masonsoft.imsdk.sample.uniontype;

import com.idonans.dynamic.uniontype.loadingstatus.UnionTypeLoadingStatus;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageTextReceivedViewHolder;
import com.masonsoft.imsdk.sample.uniontype.viewholder.IMMessageTextSendViewHolder;

public class UnionTypeMapper extends UnionTypeLoadingStatus {

    public static final int UNION_TYPE_IM_MESSAGE_TEXT_RECEIVED = 1; // 聊天消息-接收的文字
    public static final int UNION_TYPE_IM_MESSAGE_TEXT_SEND = 2; // 聊天消息-发送的文字

    public UnionTypeMapper() {
        put(UNION_TYPE_IM_MESSAGE_TEXT_RECEIVED, IMMessageTextReceivedViewHolder::new);
        put(UNION_TYPE_IM_MESSAGE_TEXT_SEND, IMMessageTextSendViewHolder::new);
    }

}
