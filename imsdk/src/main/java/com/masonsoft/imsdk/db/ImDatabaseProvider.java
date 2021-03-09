package com.masonsoft.imsdk.db;

import com.idonans.lang.Singleton;
import com.xmqvip.xiaomaiquan.common.im.ImConstant;

import java.util.HashMap;
import java.util.Map;

public class ImDatabaseProvider {

    private static final Singleton<com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseProvider> sInstance = new Singleton<com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseProvider>() {
        @Override
        protected com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseProvider create() {
            return new com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseProvider();
        }
    };

    public static com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseProvider getInstance() {
        return sInstance.get();
    }

    private final Map<String, com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper> mDBHelpers = new HashMap<>();

    private ImDatabaseProvider() {
    }

    /**
     * 获取全局共享的数据空间
     */
    public com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper getShareDBHelper() {
        return getDBHelper(ImConstant.SHARE_SESSION_NAMESPACE);
    }

    public com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper getDBHelper(long sessionNamespace) {
        synchronized (mDBHelpers) {
            String key = String.valueOf(sessionNamespace);
            com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper dbHelper = mDBHelpers.get(key);
            if (dbHelper == null) {
                dbHelper = new com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper(sessionNamespace);
                mDBHelpers.put(key, dbHelper);
            }
            return dbHelper;
        }
    }

}
