package com.masonsoft.imsdk.sample.util;

import androidx.annotation.Nullable;

import java.util.UUID;

public class FilenameUtil {

    public static String createUnionFilename(@Nullable String fileExtension) {
        String filename = UUID.randomUUID().toString().replace("-", "");
        if (fileExtension != null) {
            filename += "." + fileExtension.toLowerCase();
        }
        return filename;
    }

}
