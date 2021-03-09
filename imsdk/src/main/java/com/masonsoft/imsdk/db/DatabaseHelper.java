package com.masonsoft.imsdk.db;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import com.idonans.core.manager.ProcessManager;
import com.idonans.core.util.ContextUtil;
import com.masonsoft.imsdk.IMConstants;
import com.masonsoft.imsdk.IMLog;

/**
 * 每一个登录用户分别属于单独的数据库文件
 */
public final class DatabaseHelper {

    // 当前数据库最新版本号
    private static final int DB_VERSION = 1;
    // 会话表
    public static final String TABLE_NAME_CONVERSATION = "t_conversation";
    // 消息表
    public static final String TABLE_NAME_MESSAGE = "t_message";

    @NonNull
    private final SQLiteOpenHelper mDBHelper;

    /**
     * 会话表
     */
    public interface ColumnsConversation {

        /**
         * 自增主键
         *
         * @since db version 1
         */
        String C_ID = "c_id";

        /**
         * 会话的排序字段(根据逻辑可能会产生冲突，但是概率极小).
         * 此字段有索引但是不唯一(当产生相同值时可能会影响会话的分页读取准确性，数据库内容不会丢失)
         *
         * @see Sequence
         * @since db version 1
         */
        String C_SEQ = "c_seq";

        /**
         * 会话目标用户 id
         *
         * @since db version 1
         */
        String C_TARGET_USER_ID = "c_target_user_id";

        /**
         * 会话中的最后一条消息 id，可能是发送的，也可能是收到的 (对应消息表的自增主键)
         *
         * @since db version 1
         */
        String C_LAST_MSG_ID = "c_last_msg_id";

        /**
         * 该会话是否置顶
         *
         * @since db version 1
         */
        String C_TOP = "c_top";

        /**
         * 会话未读消息数
         *
         * @since db version 1
         */
        String C_UNREAD_COUNT = "c_unread_count";

        /**
         * 会话类型，用来区分是聊天消息，系统消息等等。
         *
         * @since db version 1
         */
        String C_CONVERSATION_TYPE = "c_conversation_type";
    }

    /**
     * 消息表
     */
    public interface ColumnsMessage {

        /**
         * 自增主键
         *
         * @since db version 1
         */
        String C_ID = "c_id";

        /**
         * 消息的排序字段
         *
         * @see Sequence
         * @since db version 1
         */
        String C_SEQ = "c_seq";

        /**
         * 所属会话，对应会话表的自增主键
         *
         * @since db version 1
         */
        String C_CONVERSATION_ID = "c_conversation_id";

        /**
         * 服务器消息 id
         *
         * @since db version 1
         */
        String C_MSG_ID = "c_msg_id";

        /**
         * 消息产生的时间(毫秒)
         *
         * @since db version 1
         */
        String C_TIME_MS = "c_time_ms";

        /**
         * 消息发送者的 user id
         *
         * @since db version 1
         */
        String C_FROM_USER_ID = "c_from_user_id";

        /**
         * 消息接收者的 user id
         *
         * @since db version 1
         */
        String C_TO_USER_ID = "c_to_user_id";

        /**
         * 消息类型，用来区分消息内容是哪一种格式. 当一条消息被撤回时，它的消息类型会发生变化(已撤回的消息是一个单独的消息类型).
         *
         * @since db version 1
         */
        String C_MSG_TYPE = "c_msg_type";

        /**
         * 消息内容
         *
         * @since db version 1
         */
        String C_BODY = "c_body";

        /**
         * 原始消息内容(当消息是从本地发出时，如果该消息需要中转处理，则该字段存储原始的内容)。
         * 例如：当发送一条图片消息时，此字段存储原始的本地文件地址。
         */
        String C_BODY_ORIGIN = "c_body_origin";

        /**
         * 宽度
         *
         * @since db version 1
         */
        String C_WIDTH = "c_width";

        /**
         * 高度
         *
         * @since db version 1
         */
        String C_HEIGHT = "c_height";

        /**
         * 时长
         *
         * @since db version 1
         */
        String C_DURATION = "c_duration";

        /**
         * 缩略图
         *
         * @since db version 1
         */
        String C_THUMB = "c_thumb";

        /**
         * 纬度
         *
         * @since db version 1
         */
        String C_LAT = "c_lat";

        /**
         * 经度
         *
         * @since db version 1
         */
        String C_LNG = "c_lng";

        /**
         * 地图缩放比
         *
         * @since db version 1
         */
        String C_ZOOM = "c_zoom";

        /**
         * 标题
         *
         * @since db version 1
         */
        String C_TITLE = "c_title";

        /**
         * 消息发送状态
         * <p>Added in version 1</p>
         */
        String C_SEND_STATUS = "c_send_status";

        /**
         * 消息阅读状态
         * <p>Added in version 1</p>
         */
        String C_READ_STATUS = "c_read_status";
    }

    /**
     * 不同的 sessionNamespace 使用不同的数据库文件。
     * <p>
     * <pre>
     * 用法举例：使用全局共享的数据时，可以使用固定的 sessionNamespace 值，如 -1.
     * 仅当前登录用户可见的数据，则用当前登录用户的 id 作为 sessionNamespace.
     * </pre>
     */
    public DatabaseHelper(final long sessionNamespace) {
        final String dbName = IMConstants.GLOBAL_NAMESPACE + "_" + sessionNamespace + "_" + ProcessManager.getInstance().getProcessTag();
        mDBHelper = new SQLiteOpenHelper(ContextUtil.getContext(), dbName, null, DB_VERSION) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.beginTransaction();
                try {
                    // 创建会话表
                    db.execSQL(getSQLCreateTableConversation());
                    // 创建会话表索引
                    for (String sqlIndex : getSQLIndexTableConversation()) {
                        db.execSQL(sqlIndex);
                    }

                    // 创建消息表
                    db.execSQL(getSQLCreateTableMessage());
                    // 创建消息表索引
                    for (String sqlIndex : getSQLIndexTableMessage()) {
                        db.execSQL(sqlIndex);
                    }

                    db.setTransactionSuccessful();
                } catch (Throwable e) {
                    IMLog.e(e);
                } finally {
                    db.endTransaction();
                }
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                IMLog.v("database version upgrade from %s to %s", oldVersion, newVersion);
            }
        };
    }

    @NonNull
    public SQLiteOpenHelper getDBHelper() {
        return mDBHelper;
    }

    /**
     * 会话表创建语句(数据库最新版本)
     */
    @NonNull
    private String getSQLCreateTableConversation() {
        return "create table " + TABLE_NAME_CONVERSATION + " (" +
                ColumnsConversation.C_ID + " integer primary key autoincrement not null," +
                ColumnsConversation.C_SEQ + " integer not null," +
                ColumnsConversation.C_TARGET_USER_ID + " integer not null," +
                ColumnsConversation.C_LAST_MSG_ID + " integer default 0," +
                ColumnsConversation.C_TOP + " integer default 0," +
                ColumnsConversation.C_UNREAD_COUNT + " integer default 0," +
                ColumnsConversation.C_CONVERSATION_TYPE + " integer default 0" +
                ")";
    }

    /**
     * 会话表创建索引语句(数据库最新版本)
     */
    @NonNull
    private String[] getSQLIndexTableConversation() {
        return new String[]{
                "create index " + TABLE_NAME_CONVERSATION + "_index_seq on " + TABLE_NAME_CONVERSATION + "(" + ColumnsConversation.C_TARGET_USER_ID + ")",
                "create index " + TABLE_NAME_CONVERSATION + "_index_target_user_id on " + TABLE_NAME_CONVERSATION + "(" + ColumnsConversation.C_TARGET_USER_ID + ")",
                "create index " + TABLE_NAME_CONVERSATION + "_index_conversation_type on " + TABLE_NAME_CONVERSATION + "(" + ColumnsConversation.C_CONVERSATION_TYPE + ")"
        };
    }

    /**
     * 数据库版本 3 中，会话表(Conversation) 相关的变更.
     * <p>
     * 增加列，分别是:<br/>
     * {@linkplain ColumnsConversation#C_LATEST_UNREAD_GIFT_MSG_ID}<br/>
     * </p>
     */
    private void exeTableConversationUpgradeSQLOnVersion3(SQLiteDatabase db) {
        Timber.v("exeTableConversationUpgradeSQLOnVersion3");
        db.execSQL("alter table " + TABLE_NAME_CONVERSATION + " add column " + ColumnsConversation.C_LATEST_UNREAD_GIFT_MSG_ID + " integer default 0");
    }

    /**
     * 数据库版本 5 中，会话表(Conversation) 相关的变更.
     * <p>
     * 业务逻辑上不再支持‘新的留言’消息类型，需要清除对应的未读消息数，以免影响未读消息总数的统计.
     * </p>
     */
    private void exeTableConversationUpgradeSQLOnVersion5(SQLiteDatabase db) {
        Timber.v("exeTableConversationUpgradeSQLOnVersion5");
        db.execSQL("update " + TABLE_NAME_CONVERSATION + " set " + ColumnsConversation.C_UNREAD_COUNT + "=0 where " + ColumnsConversation.C_SYSTEM_TYPE + "=" + ImConstant.ConversationSystemType.SYSTEM_TYPE_COMMENTS);
    }

    /**
     * 消息表创建语句(数据库最新版本)
     */
    @NonNull
    private final String getSQLCreateTableMessage() {
        return "create table " + TABLE_NAME_MESSAGE + " (" +
                ColumnsMessage.C_ID + " integer primary key autoincrement," +
                ColumnsMessage.C_CONVERSATION_ID + " integer default 0," +
                ColumnsMessage.C_MSG_ID + " integer default 0," +
                ColumnsMessage.C_MSG_SERVER_TIME + " integer default 0," +
                ColumnsMessage.C_FROM_USER_ID + " integer default 0," +
                ColumnsMessage.C_TO_USER_ID + " integer default 0," +
                ColumnsMessage.C_MSG_LOCAL_TIME + " integer default 0," +
                ColumnsMessage.C_MSG_TYPE + " integer default 0," +
                ColumnsMessage.C_MSG_TEXT + " text," +
                ColumnsMessage.C_MSG_TITLE + " text," +
                ColumnsMessage.C_MSG_IMAGE_SERVER_THUMB + " text," +
                ColumnsMessage.C_MSG_IMAGE_SERVER_URL + " text," +
                ColumnsMessage.C_MSG_IMAGE_LOCAL_URL + " text," +
                ColumnsMessage.C_MSG_IMAGE_WIDTH + " integer default 0," +
                ColumnsMessage.C_MSG_IMAGE_HEIGHT + " integer default 0," +
                ColumnsMessage.C_MSG_IMAGE_FILE_SIZE + " integer default 0," +
                ColumnsMessage.C_MSG_VOICE_SERVER_URL + " text," +
                ColumnsMessage.C_MSG_VOICE_LOCAL_URL + " text," +
                ColumnsMessage.C_MSG_VOICE_DURATION + " integer default 0," +
                ColumnsMessage.C_MSG_VOICE_FILE_SIZE + " integer default 0," +
                ColumnsMessage.C_MSG_VIDEO_SERVER_THUMB + " text," +
                ColumnsMessage.C_MSG_VIDEO_SERVER_URL + " text," +
                ColumnsMessage.C_MSG_VIDEO_LOCAL_URL + " text," +
                ColumnsMessage.C_MSG_VIDEO_WIDTH + " integer default 0," +
                ColumnsMessage.C_MSG_VIDEO_HEIGHT + " integer default 0," +
                ColumnsMessage.C_MSG_VIDEO_DURATION + " integer default 0," +
                ColumnsMessage.C_MSG_VIDEO_FILE_SIZE + " integer default 0," +
                ColumnsMessage.C_MSG_LOCATION_TITLE + " text," +
                ColumnsMessage.C_MSG_LOCATION_LAT + " text," +
                ColumnsMessage.C_MSG_LOCATION_LNG + " text," +
                ColumnsMessage.C_MSG_LOCATION_ZOOM + " integer default 0," +
                ColumnsMessage.C_MSG_LOCATION_ADDRESS + " text," +
                ColumnsMessage.C_MSG_UGC_ID + " integer default 0," +
                ColumnsMessage.C_MSG_UGC_USER_ID + " integer default 0," +
                ColumnsMessage.C_MSG_UGC_SERVER_THUMB + " text," +
                ColumnsMessage.C_MSG_UGC_NICE_NUM + " integer default 0," +
                ColumnsMessage.C_MSG_FROM_USER_ID + " integer default 0," +
                ColumnsMessage.C_MSG_NUMBER + " integer default 0," +
                ColumnsMessage.C_MSG_URL + " text," +
                ColumnsMessage.C_MSG_SUBJECT + " text," +
                ColumnsMessage.C_MSG_MSGS + " text," +
                ColumnsMessage.C_MSG_GIFT_ID + " integer default 0," +
                ColumnsMessage.C_MSG_GIFT_NAME + " text," +
                ColumnsMessage.C_MSG_GIFT_DESC + " text," +
                ColumnsMessage.C_MSG_GIFT_K_PRICE + " integer default 0," +
                ColumnsMessage.C_MSG_GIFT_COVER + " text," +
                ColumnsMessage.C_MSG_GIFT_ANIM + " text," +
                ColumnsMessage.C_SEND_STATUS + " integer default 0," +
                ColumnsMessage.C_READ_STATUS + " integer default 0," +
                ColumnsMessage.C_REVERT_STATUS + " integer default 0" +
                ")";
    }

    /**
     * 消息表创建索引语句(数据库最新版本)
     */
    @NonNull
    private final String[] getSQLIndexTableMessage() {
        return new String[]{
                "create index " + TABLE_NAME_MESSAGE + "_index_conversation_id on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_CONVERSATION_ID + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_msg_id on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_MSG_ID + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_msg_server_time on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_MSG_SERVER_TIME + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_from_user_id on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_FROM_USER_ID + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_to_user_id on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_TO_USER_ID + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_msg_local_time on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_MSG_LOCAL_TIME + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_send_status on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_SEND_STATUS + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_read_status on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_READ_STATUS + ")"
        };
    }

    /**
     * 数据库版本 2 中，消息表(Message) 相关的变更.
     * <p>
     * 增加列，分别是:<br/>
     * {@linkplain ColumnsMessage#C_MSG_GIFT_ID}<br/>
     * {@linkplain ColumnsMessage#C_MSG_GIFT_NAME}<br/>
     * {@linkplain ColumnsMessage#C_MSG_GIFT_DESC}<br/>
     * {@linkplain ColumnsMessage#C_MSG_GIFT_K_PRICE}<br/>
     * {@linkplain ColumnsMessage#C_MSG_GIFT_COVER}<br/>
     * {@linkplain ColumnsMessage#C_MSG_GIFT_ANIM}<br/>
     * </p>
     */
    private void exeTableMessageUpgradeSQLOnVersion2(SQLiteDatabase db) {
        Timber.v("exeTableMessageUpgradeSQLOnVersion2");
        db.execSQL("alter table " + TABLE_NAME_MESSAGE + " add column " + ColumnsMessage.C_MSG_GIFT_ID + " integer default 0");
        db.execSQL("alter table " + TABLE_NAME_MESSAGE + " add column " + ColumnsMessage.C_MSG_GIFT_NAME + " text");
        db.execSQL("alter table " + TABLE_NAME_MESSAGE + " add column " + ColumnsMessage.C_MSG_GIFT_DESC + " text");
        db.execSQL("alter table " + TABLE_NAME_MESSAGE + " add column " + ColumnsMessage.C_MSG_GIFT_K_PRICE + " integer default 0");
        db.execSQL("alter table " + TABLE_NAME_MESSAGE + " add column " + ColumnsMessage.C_MSG_GIFT_COVER + " text");
        db.execSQL("alter table " + TABLE_NAME_MESSAGE + " add column " + ColumnsMessage.C_MSG_GIFT_ANIM + " text");
    }

    /**
     * 数据库版本 4 中，消息表(Message) 相关的变更.
     * <p>
     * 增加列，分别是:<br/>
     * {@linkplain ColumnsMessage#C_MSG_URL}<br/>
     * {@linkplain ColumnsMessage#C_MSG_SUBJECT}<br/>
     * </p>
     */
    private void exeTableMessageUpgradeSQLOnVersion4(SQLiteDatabase db) {
        Timber.v("exeTableMessageUpgradeSQLOnVersion4");
        db.execSQL("alter table " + TABLE_NAME_MESSAGE + " add column " + ColumnsMessage.C_MSG_URL + " text");
        db.execSQL("alter table " + TABLE_NAME_MESSAGE + " add column " + ColumnsMessage.C_MSG_SUBJECT + " text");
    }

    @Override
    public void close() {
        mDBHelper.close();
    }

}
