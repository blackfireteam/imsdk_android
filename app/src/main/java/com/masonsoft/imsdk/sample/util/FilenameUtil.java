package com.masonsoft.imsdk.sample.util;

import java.util.UUID;

public class FilenameUtil {

    public static String createUnionFilename(String fileExtension) {
        return UUID.randomUUID().toString().replace("-", "") + "." + fileExtension.toLowerCase();
    }

}
