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
         * 会话中的第一条消息 id(这条消息可能并没有存储在本地)。
         * 服务器端的逻辑消息，用来计算是否还有更多历史消息。
         * 这条消息可能是一个指令消息。
         */
        String C_MSG_START_ID = "c_msg_start_id";

        /**
         * 会话中的最后一条消息 id(这条消息可能并没有存储在本地)。
         * 服务器端的逻辑消息，用来计算是否还有更多新消息。
         * 如果在此会话中收到了一条消息 id 更大的消息，则会手动更新该值。
         * 这条消息可能是一个指令消息。
         */
        String C_MSG_END_ID = "c_msg_end_id";

        /**
         * 最后一条已读消息 id(这条消息可能并没有存储在本地)。
         */
        String C_MSG_LAST_READ_ID = "c_msg_last_read_id";

        /**
         * 在会话上需要展示的那条消息的类型
         */
        String C_SHOW_MSG_TYPE = "c_show_msg_type";

        /**
         * 在会话上需要展示的那条消息的 id(这条消息可能并没有存储在本地)。
         * 服务器端的逻辑消息，当收到的消息比这个消息 id 更大时，需要累加未读消息数。
         */
        String C_SHOW_MSG_ID = "c_show_msg_id";

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

        /**
         * 会话的展示时间，通常是最后一条消息的时间(毫秒).
         */
        String C_TIME_MS = "c_time_ms";

        /**
         * 业务定制：是否 match
         *
         * @since db version 1
         */
        String C_MATCHED = "c_matched";

        /**
         * 业务定制：是否是 new message
         */
        String C_NEW_MSG = "c_new_msg";

        /**
         * 业务定制：是否 my move
         */
        String C_MY_MOVE = "c_my_move";

        /**
         * 业务定制：是否 ice break
         */
        String C_ICE_BREAK = "c_ice_break";

        /**
         * 业务定制：是否 tip free
         */
        String C_TIP_FREE = "c_tip_free";

        /**
         * 业务定制：是否 top album
         */
        String C_TOP_ALBUM = "c_top_album";
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
         * 消息的排序字段.此字段有索引但是不唯一。
         * 在同一个会话中是唯一的。
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
         * 消息发送者的个人信息的最后更新时间。用来校验本地缓存是否需要更新。
         */
        String C_FROM_USER_PROFILE_LAST_MODIFY = "c_from_user_profile_last_modify";

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
         *
         * @since db version 1
         */
        String C_SEND_STATUS = "c_send_status";
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
                ColumnsConversation.C_MSG_START_ID + " integer not null default 0," +
                ColumnsConversation.C_MSG_END_ID + " integer not null default 0," +
                ColumnsConversation.C_MSG_LAST_READ_ID + " integer not null default 0," +
                ColumnsConversation.C_SHOW_MSG_TYPE + " integer not null default 0," +
                ColumnsConversation.C_SHOW_MSG_ID + " integer not null default 0," +
                ColumnsConversation.C_LAST_MSG_ID + " integer not null default 0," +
                ColumnsConversation.C_TOP + " integer not null default 0," +
                ColumnsConversation.C_UNREAD_COUNT + " integer not null default 0," +
                ColumnsConversation.C_CONVERSATION_TYPE + " integer not null default 0," +
                ColumnsConversation.C_TIME_MS + " integer not null," +
                ColumnsConversation.C_MATCHED + " integer not null default 0," +
                ColumnsConversation.C_NEW_MSG + " integer not null default 0," +
                ColumnsConversation.C_MY_MOVE + " integer not null default 0," +
                ColumnsConversation.C_ICE_BREAK + " integer not null default 0," +
                ColumnsConversation.C_TIP_FREE + " integer not null default 0," +
                ColumnsConversation.C_TOP_ALBUM + " integer not null default 0" +
                ")";
    }

    /**
     * 会话表创建索引语句(数据库最新版本)
     */
    @NonNull
    private String[] getSQLIndexTableConversation() {
        return new String[]{
                "create index " + TABLE_NAME_CONVERSATION + "_index_seq on " + TABLE_NAME_CONVERSATION + "(" + ColumnsConversation.C_SEQ + ")",
                "create index " + TABLE_NAME_CONVERSATION + "_index_target_user_id on " + TABLE_NAME_CONVERSATION + "(" + ColumnsConversation.C_TARGET_USER_ID + ")",
                "create index " + TABLE_NAME_CONVERSATION + "_index_conversation_type on " + TABLE_NAME_CONVERSATION + "(" + ColumnsConversation.C_CONVERSATION_TYPE + ")"
        };
    }

    /**
     * 消息表创建语句(数据库最新版本)
     */
    @NonNull
    private String getSQLCreateTableMessage() {
        return "create table " + TABLE_NAME_MESSAGE + " (" +
                ColumnsMessage.C_ID + " integer primary key autoincrement not null," +
                ColumnsMessage.C_SEQ + " integer not null," +
                ColumnsMessage.C_CONVERSATION_ID + " integer not null," +
                ColumnsMessage.C_MSG_ID + " integer not null default 0," +
                ColumnsMessage.C_TIME_MS + " integer not null," +
                ColumnsMessage.C_FROM_USER_ID + " integer not null," +
                ColumnsMessage.C_FROM_USER_PROFILE_LAST_MODIFY + " integer not null default 0," +
                ColumnsMessage.C_TO_USER_ID + " integer not null," +
                ColumnsMessage.C_MSG_TYPE + " integer not null default 0," +
                ColumnsMessage.C_BODY + " text," +
                ColumnsMessage.C_BODY_ORIGIN + " text," +
                ColumnsMessage.C_WIDTH + " integer not null default 0," +
                ColumnsMessage.C_HEIGHT + " integer not null default 0," +
                ColumnsMessage.C_DURATION + " integer not null default 0," +
                ColumnsMessage.C_THUMB + " text," +
                ColumnsMessage.C_LAT + " double," +
                ColumnsMessage.C_LNG + " double," +
                ColumnsMessage.C_ZOOM + " integer not null default 0," +
                ColumnsMessage.C_TITLE + " text," +
                ColumnsMessage.C_SEND_STATUS + " integer not null default 0" +
                ")";
    }

    /**
     * 消息表创建索引语句(数据库最新版本)
     */
    @NonNull
    private String[] getSQLIndexTableMessage() {
        return new String[]{
                "create index " + TABLE_NAME_MESSAGE + "_index_seq on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_SEQ + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_conversation_id on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_CONVERSATION_ID + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_msg_id on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_MSG_ID + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_from_user_id on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_FROM_USER_ID + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_from_user_profile_last_modify on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_FROM_USER_PROFILE_LAST_MODIFY + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_to_user_id on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_TO_USER_ID + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_msg_type on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_MSG_TYPE + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_send_status on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_SEND_STATUS + ")"
        };
    }

}
