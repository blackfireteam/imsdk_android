package com.masonsoft.imsdk.db;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import com.idonans.core.manager.ProcessManager;
import com.idonans.core.util.ContextUtil;
import com.masonsoft.imsdk.IMConstants;
import com.masonsoft.imsdk.IMLog;
import com.masonsoft.imsdk.annotation.Local;
import com.masonsoft.imsdk.annotation.Remote;

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
        @Local
        String C_LOCAL_ID = "c_local_id";

        /**
         * 会话的排序字段(根据逻辑可能会产生重复，但是概率极小).
         * 此字段有索引但是不唯一(当产生相同值时可能会影响会话的分页读取准确性，数据库内容不会丢失)
         *
         * @see Sequence
         * @since db version 1
         */
        @Local
        String C_LOCAL_SEQ = "c_local_seq";

        /**
         * 会话目标用户 id
         *
         * @since db version 1
         */
        @Remote("uid")
        @Local
        String C_TARGET_USER_ID = "c_target_user_id";

        /**
         * 会话中的第一条消息 id(这条消息可能并没有存储在本地)。
         * 服务器端的逻辑消息 id
         *
         * @since db version 1
         */
        @Remote("msg_start")
        String C_REMOTE_MSG_START = "c_remote_msg_start";

        /**
         * 会话中的最后一条消息 id(这条消息可能并没有存储在本地)。
         * 服务器端的逻辑消息 id
         *
         * @since db version 1
         */
        @Remote("msg_end")
        String C_REMOTE_MSG_END = "c_remote_msg_end";

        /**
         * 最后一条已读消息 id(这条消息可能并没有存储在本地)。
         *
         * @since db version 1
         */
        @Remote("msg_last_read")
        String C_REMOTE_MSG_LAST_READ = "c_remote_msg_last_read";

        /**
         * 在会话上需要展示的那条消息的 id(这条消息可能并没有存储在本地)。
         * 服务器端的逻辑消息 id, 在一个会话内是唯一的。
         *
         * @since db version 1
         */
        @Remote("show_msg_id")
        String C_REMOTE_SHOW_MSG_ID = "c_remote_show_msg_id";

        /**
         * 在会话上需要展示的那一条消息内容(不一定会是最后一条消息，例如：当最后一条消息是撤回消息的指令消息时).
         * 这是一个本地消息 id, 对应消息表的自增主键。
         *
         * @since db version 1
         */
        @Local
        String C_LOCAL_SHOW_MSG_ID = "c_local_show_msg_id";

        /**
         * 会话的未读消息数，最近一次服务器下发的或者是最近一次同步的(与 msg_last_read，msg_end 相关)。
         *
         * @since db version 1
         */
        @Remote("unread")
        String C_REMOTE_UNREAD = "c_remote_unread";

        /**
         * 会话的未读消息数，本地统计的
         *
         * @since db version 1
         */
        @Local
        String C_LOCAL_UNREAD_COUNT = "c_local_unread_count";

        /**
         * 会话的展示时间，通常是最后一条消息的时间(毫秒).
         *
         * @since db version 1
         */
        @Local
        String C_LOCAL_TIME_MS = "c_local_time_ms";

        /**
         * 会话是否已删除
         *
         * @since db version 1
         */
        @Local
        String C_LOCAL_DELETE = "c_local_delete";

        /**
         * 业务定制：是否 match
         *
         * @since db version 1
         */
        @Remote("matched")
        @Local
        String C_MATCHED = "c_matched";

        /**
         * 业务定制：是否是 new message
         *
         * @since db version 1
         */
        @Remote("new_msg")
        @Local
        String C_NEW_MSG = "c_new_msg";

        /**
         * 业务定制：是否 my move
         *
         * @since db version 1
         */
        @Remote("my_move")
        @Local
        String C_MY_MOVE = "c_my_move";

        /**
         * 业务定制：是否 ice break
         *
         * @since db version 1
         */
        @Remote("ice_break")
        @Local
        String C_ICE_BREAK = "c_ice_break";

        /**
         * 业务定制：是否 tip free
         *
         * @since db version 1
         */
        @Remote("tip_free")
        @Local
        String C_TIP_FREE = "c_tip_free";

        /**
         * 业务定制：是否 top album
         *
         * @since db version 1
         */
        @Remote("top_album")
        @Local
        String C_TOP_ALBUM = "c_top_album";

        /**
         * 业务定制：是否 block 了对方
         *
         * @since db version 1
         */
        @Remote("i_block_u")
        @Local
        String C_I_BLOCK_U = "c_i_block_u";

        /**
         * 业务定制：双方互发过消息了
         *
         * @since db version 1
         */
        @Remote("connected")
        @Local
        String C_CONNECTED = "c_connected";
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
        @Local
        String C_LOCAL_ID = "c_local_id";

        /**
         * 消息的排序字段.此字段有索引但是不唯一。(根据逻辑可能会产生重复，但是概率极小).
         * 在同一个会话中是唯一的。
         *
         * @see Sequence
         * @since db version 1
         */
        @Local
        String C_LOCAL_SEQ = "c_local_seq";

        /**
         * 所属会话，对应会话表的自增主键
         *
         * @since db version 1
         */
        @Local
        String C_LOCAL_CONVERSATION_ID = "c_local_conversation_id";

        /**
         * 消息的发送方
         *
         * @since db version 1
         */
        @Remote("from_uid")
        @Local
        String C_FROM_USER_ID = "c_from_user_id";

        /**
         * 消息的接收方
         *
         * @since db version 1
         */
        @Remote("to_uid")
        @Local
        String C_TO_USER_ID = "c_to_user_id";

        /**
         * 服务器消息 id
         *
         * @since db version 1
         */
        @Remote("msg_id")
        String C_REMOTE_MSG_ID = "c_remote_msg_id";

        /**
         * 消息时间(秒)
         *
         * @since db version 1
         */
        @Remote("msg_time")
        String C_REMOTE_MSG_TIME = "c_remote_msg_time";

        /**
         * 消息产生的时间(毫秒)
         *
         * @since db version 1
         */
        @Local
        String C_LOCAL_TIME_MS = "c_local_time_ms";

        /**
         * 消息发送者的个人信息的最后更新时间。用来校验本地缓存是否需要更新。
         *
         * @since db version 1
         */
        @Remote("sput")
        String C_REMOTE_FROM_USER_PROFILE_LAST_MODIFY = "c_remote_from_user_profile_last_modify";

        /**
         * 消息类型，用来区分消息内容是哪一种格式. 当一条消息被撤回时，它的消息类型会发生变化(已撤回的消息是一个单独的消息类型).
         *
         * @since db version 1
         */
        @Remote("type")
        @Local
        String C_MSG_TYPE = "c_msg_type";

        /**
         * 消息内容：title
         *
         * @since db version 1
         */
        @Remote("title")
        @Local
        String C_TITLE = "c_title";

        /**
         * 消息内容：body
         *
         * @since db version 1
         */
        @Remote("body")
        @Local
        String C_BODY = "c_body";

        /**
         * 原始消息内容(当消息是从本地发出时，如果该消息需要中转处理，则该字段存储原始的内容)。
         * 例如：当发送一条图片消息时，此字段存储原始的本地文件地址。
         */
        @Local
        String C_LOCAL_BODY_ORIGIN = "c_local_body_origin";

        /**
         * 消息内容：thumb
         *
         * @since db version 1
         */
        @Remote("thumb")
        @Local
        String C_THUMB = "c_thumb";

        /**
         * 宽度
         *
         * @since db version 1
         */
        @Remote("width")
        @Local
        String C_WIDTH = "c_width";

        /**
         * 高度
         *
         * @since db version 1
         */
        @Remote("height")
        @Local
        String C_HEIGHT = "c_height";

        /**
         * 时长
         *
         * @since db version 1
         */
        @Remote("duration")
        @Local
        String C_DURATION = "c_duration";

        /**
         * 纬度
         *
         * @since db version 1
         */
        @Remote("lat")
        @Local
        String C_LAT = "c_lat";

        /**
         * 经度
         *
         * @since db version 1
         */
        @Remote("lng")
        @Local
        String C_LNG = "c_lng";

        /**
         * 地图缩放比
         *
         * @since db version 1
         */
        @Remote("zoom")
        @Local
        String C_ZOOM = "c_zoom";

        /**
         * 消息发送状态
         *
         * @since db version 1
         */
        @Local
        String C_LOCAL_SEND_STATUS = "c_local_send_status";

        /**
         * 该消息是否是一条指令消息。指令消息不会显示在屏幕上。（如撤回消息是一条指令消息）
         */
        @Local
        String C_LOCAL_ACTION_MSG = "c_local_action_msg";

        /**
         * 消息所属 block。相同 block 的消息是连续的。
         */
        @Local
        String C_LOCAL_BLOCK_ID = "c_local_block_id";
    }

    /**
     * 不同的 sessionNamespace 使用不同的数据库文件。
     * <p>
     * <pre>
     * 用法举例：使用全局共享的数据时，可以使用固定的 sessionNamespace 值，如 "share".
     * 仅当前登录用户可见的数据，则用当前登录用户的 id 作为 sessionNamespace.
     * </pre>
     */
    public DatabaseHelper(final String sessionNamespace) {
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
                ColumnsConversation.C_LOCAL_ID + " integer primary key autoincrement not null," +
                ColumnsConversation.C_LOCAL_SEQ + " integer not null," +
                ColumnsConversation.C_TARGET_USER_ID + " integer not null," +
                ColumnsConversation.C_REMOTE_MSG_START + " integer not null default 0," +
                ColumnsConversation.C_REMOTE_MSG_END + " integer not null default 0," +
                ColumnsConversation.C_REMOTE_MSG_LAST_READ + " integer not null default 0," +
                ColumnsConversation.C_REMOTE_SHOW_MSG_ID + " integer not null default 0," +
                ColumnsConversation.C_LOCAL_SHOW_MSG_ID + " integer not null default 0," +
                ColumnsConversation.C_REMOTE_UNREAD + " integer not null default 0," +
                ColumnsConversation.C_LOCAL_UNREAD_COUNT + " integer not null default 0," +
                ColumnsConversation.C_LOCAL_TIME_MS + " integer not null default 0," +
                ColumnsConversation.C_LOCAL_DELETE + " integer not null default 0," +
                ColumnsConversation.C_MATCHED + " integer not null default 0," +
                ColumnsConversation.C_NEW_MSG + " integer not null default 0," +
                ColumnsConversation.C_MY_MOVE + " integer not null default 0," +
                ColumnsConversation.C_ICE_BREAK + " integer not null default 0," +
                ColumnsConversation.C_TIP_FREE + " integer not null default 0," +
                ColumnsConversation.C_TOP_ALBUM + " integer not null default 0," +
                ColumnsConversation.C_I_BLOCK_U + " integer not null default 0," +
                ColumnsConversation.C_CONNECTED + " integer not null default 0" +
                ")";
    }

    /**
     * 会话表创建索引语句(数据库最新版本)
     */
    @NonNull
    private String[] getSQLIndexTableConversation() {
        return new String[]{
                "create index " + TABLE_NAME_CONVERSATION + "_index_local_seq on " + TABLE_NAME_CONVERSATION + "(" + ColumnsConversation.C_LOCAL_SEQ + ")",
                "create index " + TABLE_NAME_CONVERSATION + "_index_target_user_id on " + TABLE_NAME_CONVERSATION + "(" + ColumnsConversation.C_TARGET_USER_ID + ")",
        };
    }

    /**
     * 消息表创建语句(数据库最新版本)
     */
    @NonNull
    private String getSQLCreateTableMessage() {
        return "create table " + TABLE_NAME_MESSAGE + " (" +
                ColumnsMessage.C_LOCAL_ID + " integer primary key autoincrement not null," +
                ColumnsMessage.C_LOCAL_SEQ + " integer not null," +
                ColumnsMessage.C_LOCAL_CONVERSATION_ID + " integer not null," +
                ColumnsMessage.C_FROM_USER_ID + " integer not null default 0," +
                ColumnsMessage.C_TO_USER_ID + " integer not null," +
                ColumnsMessage.C_REMOTE_MSG_ID + " integer not null," +
                ColumnsMessage.C_REMOTE_MSG_TIME + " integer not null default 0," +
                ColumnsMessage.C_LOCAL_TIME_MS + " integer not null," +
                ColumnsMessage.C_REMOTE_FROM_USER_PROFILE_LAST_MODIFY + " integer not null default 0," +
                ColumnsMessage.C_MSG_TYPE + " integer not null," +
                ColumnsMessage.C_TITLE + " text," +
                ColumnsMessage.C_BODY + " text," +
                ColumnsMessage.C_LOCAL_BODY_ORIGIN + " text," +
                ColumnsMessage.C_THUMB + " text," +
                ColumnsMessage.C_WIDTH + " integer not null default 0," +
                ColumnsMessage.C_HEIGHT + " integer not null default 0," +
                ColumnsMessage.C_DURATION + " integer not null default 0," +
                ColumnsMessage.C_LAT + " double," +
                ColumnsMessage.C_LNG + " double," +
                ColumnsMessage.C_ZOOM + " integer not null default 0," +
                ColumnsMessage.C_LOCAL_SEND_STATUS + " integer not null default 0," +
                ColumnsMessage.C_LOCAL_ACTION_MSG + " integer not null," +
                ColumnsMessage.C_LOCAL_BLOCK_ID + " integer not null" +
                ")";
    }

    /**
     * 消息表创建索引语句(数据库最新版本)
     */
    @NonNull
    private String[] getSQLIndexTableMessage() {
        return new String[]{
                "create index " + TABLE_NAME_MESSAGE + "_index_local_seq on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_LOCAL_SEQ + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_local_conversation_id on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_LOCAL_CONVERSATION_ID + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_from_user_id on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_FROM_USER_ID + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_to_user_id on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_TO_USER_ID + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_remote_msg_id on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_REMOTE_MSG_ID + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_msg_type on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_MSG_TYPE + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_local_send_status on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_LOCAL_SEND_STATUS + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_local_action_msg on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_LOCAL_ACTION_MSG + ")",
                "create index " + TABLE_NAME_MESSAGE + "_index_local_block_id on " + TABLE_NAME_MESSAGE + "(" + ColumnsMessage.C_LOCAL_BLOCK_ID + ")",
        };
    }

}
