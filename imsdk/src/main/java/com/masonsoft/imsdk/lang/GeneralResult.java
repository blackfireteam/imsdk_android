package com.masonsoft.imsdk.lang;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.util.Objects;

public class GeneralResult extends GeneralErrorCode {

    public int code;

    @Nullable
    public String message;

    @Nullable
    public GeneralResult other;

    public boolean isSuccess() {
        return this.code == CODE_SUCCESS;
    }

    @NonNull
    public GeneralResult getCause() {
        if (isSuccess()) {
            return this;
        }
        if (other != null) {
            return other;
        }
        return this;
    }

    @NonNull
    public String toShortString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(Objects.defaultObjectTag(this));
        builder.append(" code:").append(this.code);
        builder.append(" message:").append(this.message);
        if (this.other == null) {
            builder.append(" other:null");
        } else {
            builder.append(" other:").append(this.other.toShortString());
        }
        return builder.toString();
    }

    @NonNull
    @Override
    public String toString() {
        return this.toShortString();
    }

    public static GeneralResult success() {
        return valueOf(CODE_SUCCESS);
    }

    public static GeneralResult valueOf(int code) {
        return valueOf(code, findDefaultErrorMessage(code));
    }

    public static GeneralResult valueOf(int code, String message) {
        final GeneralResult result = new GeneralResult();
        result.code = code;
        result.message = message;
        return result;
    }

    public static GeneralResult valueOfOther(GeneralResult other) {
        final GeneralResult result = valueOf(ERROR_CODE_OTHER);
        result.other = other;
        return result;
    }

}
