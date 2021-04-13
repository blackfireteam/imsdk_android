package com.masonsoft.imsdk.lang;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.R;
import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.util.Objects;

public class GeneralResult {

    /**
     * 成功
     */
    public static final int CODE_SUCCESS = 0;
    /**
     * 超时
     */
    public static final int CODE_ERROR_TIMEOUT = -1;
    /**
     * 由 sub result 指定的其它错误
     */
    public static final int CODE_ERROR_SUB_RESULT = -2;

    /**
     * 0 表示成功，非 0 表示失败
     */
    public int code;
    public String message;
    @Nullable
    public GeneralResult subResult;

    public boolean isSuccess() {
        return this.code == CODE_SUCCESS;
    }

    @NonNull
    public String toShortString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(Objects.defaultObjectTag(this));
        builder.append(" code:").append(this.code);
        builder.append(" message:").append(this.message);
        if (this.subResult != null) {
            builder.append(" subResult:").append(this.subResult.toShortString());
        } else {
            builder.append(" subResult:null");
        }
        return builder.toString();
    }

    @NonNull
    @Override
    public String toString() {
        return this.toShortString();
    }

    public static GeneralResult success() {
        final GeneralResult result = new GeneralResult();
        result.code = CODE_SUCCESS;
        return result;
    }

    public static GeneralResult valueOf(int code, String message) {
        final GeneralResult result = new GeneralResult();
        result.code = code;
        result.message = message;
        return result;
    }

    public static GeneralResult valueOfSubResult(GeneralResult subResult) {
        final GeneralResult result = new GeneralResult();
        result.code = CODE_ERROR_SUB_RESULT;
        result.message = defaultMessage(result.code);
        result.subResult = subResult;
        return result;
    }

    @Nullable
    public static String defaultMessage(int code) {
        if (code == CODE_SUCCESS) {
            return I18nResources.getString(R.string.msimsdk_general_result_message_success);
        }
        if (code == CODE_ERROR_TIMEOUT) {
            return I18nResources.getString(R.string.msimsdk_general_result_message_error_timeout);
        }
        if (code == CODE_ERROR_SUB_RESULT) {
            return I18nResources.getString(R.string.msimsdk_general_result_message_error_sub_result);
        }
        return null;
    }

}
