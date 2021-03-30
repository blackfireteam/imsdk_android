package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.IMSessionMessage;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.lang.Processor;
import com.masonsoft.imsdk.util.Objects;

/**
 * @since 1.0
 */
public abstract class SendMessageNotNullValidateProcessor implements Processor<IMSessionMessage> {

    @Override
    public final boolean doProcess(@Nullable IMSessionMessage target) {
        if (target == null) {
            // unexpected
            final Throwable e = new NullPointerException("unexpected " + Objects.defaultObjectTag(this) + " doProcess target is null");
            IMLog.e(e);
            return false;
        }

        return doNotNullProcess(target);
    }

    protected abstract boolean doNotNullProcess(@NonNull IMSessionMessage target);

}
