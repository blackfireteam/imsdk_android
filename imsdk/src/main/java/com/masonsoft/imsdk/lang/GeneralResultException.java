package com.masonsoft.imsdk.lang;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GeneralResultException extends RuntimeException {

    @NonNull
    public final GeneralResult generalResult;

    public GeneralResultException(@NonNull GeneralResult generalResult) {
        this.generalResult = generalResult;
    }

    @Nullable
    public static GeneralResultException createOrNull(@Nullable GeneralResult generalResult) {
        if (generalResult == null || generalResult.isSuccess()) {
            return null;
        }
        return new GeneralResultException(generalResult);
    }

}
