package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.message.ProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;

/**
 * 过滤收到消息的 proto type 为指定 proto type 的内容. 如指定只处理 ChatR 类型的消息.
 *
 * @since 1.0
 */
public abstract class ReceivedProtoMessageProtoTypeProcessor<T> extends ReceivedProtoMessageNotNullProcessor {

    @NonNull
    private final Class<T> mProtoMessageObjectType;

    public ReceivedProtoMessageProtoTypeProcessor(@NonNull Class<T> protoMessageObjectType) {
        mProtoMessageObjectType = protoMessageObjectType;
    }

    @Override
    protected final boolean doNotNullProcess(@NonNull SessionProtoByteMessageWrapper target) {
        final ProtoByteMessageWrapper wrapper = target.getProtoByteMessageWrapper();
        final Object protoMessageObject = wrapper.getProtoMessageObject();
        if (protoMessageObject != null) {
            if (mProtoMessageObjectType.isInstance(protoMessageObject)) {
                //noinspection unchecked
                return doNotNullProtoMessageObjectProcess(target, (T) protoMessageObject);
            }
        }

        return false;
    }

    protected abstract boolean doNotNullProtoMessageObjectProcess(
            @NonNull SessionProtoByteMessageWrapper target,
            @NonNull T protoMessageObject);

}
