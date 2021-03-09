package com.masonsoft.imsdk.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.core.Singleton;

import java.util.List;

/**
 * 会话表
 */
public class ConversationDatabaseProvider {

    private static final Singleton<ConversationDatabaseProvider> INSTANCE = new Singleton<ConversationDatabaseProvider>() {
        @Override
        protected ConversationDatabaseProvider create() {
            return new ConversationDatabaseProvider();
        }
    };

    public static ConversationDatabaseProvider getInstance() {
        return INSTANCE.get();
    }

    private ConversationDatabaseProvider() {
    }

    @NonNull
    public List<ContentValues> getAllConversation(final long sessionUserId, final int conversationType, @Nullable ColumnsSelector columnsSelector) {
        if (columnsSelector == null) {
            columnsSelector = COLUMNS_SELECTOR_FULL;
        }
        final List<Conversation> items = new ArrayList<>();
        Cursor cursor = null;
        try {
            com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper dbHelper = com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            cursor = db.query(
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.TABLE_NAME_CONVERSATION,
                    columnsSelector.queryColumns(),
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_SYSTEM_TYPE + "=?",
                    new String[]{
                            String.valueOf(systemType)
                    },
                    null,
                    null,
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_TOP + " desc, "
                            + com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_LAST_MODIFY + " desc"
            );

            for (; cursor.moveToNext(); ) {
                Conversation item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                items.add(item);
            }
        } catch (Throwable e) {
            Timber.e(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }

        Timber.v("found %s conversations for session user id:%s, systemType:%s",
                items.size(), sessionUserId, systemType);
        return items;
    }

    /**
     * 是否有未读消息数
     */
    public boolean hasAnyUnreadMessage(final long sessionUserId) {
        Cursor cursor = null;
        try {
            com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper dbHelper = com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            cursor = db.query(
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.TABLE_NAME_CONVERSATION,
                    new String[]{com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_ID},
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_UNREAD_COUNT + ">0",
                    null,
                    null,
                    null,
                    null,
                    "0,1"
            );
            if (cursor.moveToNext()) {
                return true;
            }
        } catch (Throwable e) {
            Timber.e(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }
        return false;
    }

    /**
     * 更新未读消息数, unreadCountDiff 传递 Long.MIN_VALUE 表示清空未读数
     */
    public void updateConversationUnreadCount(final long sessionUserId, long conversationId, long unreadCountDiff) {
        try {
            com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper dbHelper = com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            if (unreadCountDiff == Long.MIN_VALUE) {
                // 清空未读数
                // 清空未读数时，同时清除未读礼物记录
                db.execSQL("update " + com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.TABLE_NAME_CONVERSATION + " set "
                        + com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_UNREAD_COUNT + "=0, "
                        + com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_LATEST_UNREAD_GIFT_MSG_ID + "=0 where "
                        + com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_ID + "=" + conversationId);
            } else {
                // 变更未读数
                if (unreadCountDiff > 0) {
                    db.execSQL("update " + com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.TABLE_NAME_CONVERSATION + " set "
                            + com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_UNREAD_COUNT + "=" + com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_UNREAD_COUNT + "+" + unreadCountDiff + " where "
                            + com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_ID + "=" + conversationId);
                } else if (unreadCountDiff < 0) {
                    db.execSQL("update " + com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.TABLE_NAME_CONVERSATION + " set "
                            + com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_UNREAD_COUNT + "=" + com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_UNREAD_COUNT + unreadCountDiff + " where "
                            + com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_ID + "=" + conversationId);
                }
            }
        } catch (Throwable e) {
            Timber.e(e);
        }
    }

    /**
     * @return 没有找到返回 null
     */
    @Nullable
    public Conversation getTargetConversation(final long sessionUserId, final long conversationId, @Nullable ColumnsSelector<Conversation> columnsSelector) {
        if (columnsSelector == null) {
            columnsSelector = COLUMNS_SELECTOR_FULL;
        }
        Cursor cursor = null;
        try {
            com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper dbHelper = com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            cursor = db.query(
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.TABLE_NAME_CONVERSATION,
                    columnsSelector.queryColumns(),
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_ID + "=?",
                    new String[]{
                            String.valueOf(conversationId)
                    },
                    null,
                    null,
                    null,
                    "0,1"
            );

            if (cursor.moveToNext()) {
                Conversation item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                Timber.v("found conversation with id:%s for session user id:%s, conversationId:%s",
                        item.id, sessionUserId, conversationId);
                return item;
            }

            // conversation not found
            Timber.v("conversation for session user id:%s, conversationId:%s",
                    sessionUserId, conversationId);
        } catch (Throwable e) {
            Timber.e(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }
        return null;
    }

    /**
     * @param sessionUserId
     * @param targetUserId
     * @param autoCreate    当会话信息没有找到时，是否自动创建一个
     * @return 没有找到或者创建失败返回 null
     */
    @Nullable
    public Conversation getTargetConversation(final long sessionUserId, final long targetUserId, final int systemType, final boolean autoCreate, @Nullable ColumnsSelector<Conversation> columnsSelector) {
        if (columnsSelector == null) {
            columnsSelector = COLUMNS_SELECTOR_FULL;
        }
        Cursor cursor = null;
        try {
            com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper dbHelper = com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();

            if (systemType == ImConstant.ConversationSystemType.SYSTEM_TYPE_CHAT) {
                // 普通聊天按照 target user id 分组
                cursor = db.query(
                        com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.TABLE_NAME_CONVERSATION,
                        columnsSelector.queryColumns(),
                        com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_TARGET_USER_ID + "=? and "
                                + com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_SYSTEM_TYPE + "=?",
                        new String[]{
                                String.valueOf(targetUserId),
                                String.valueOf(systemType)
                        },
                        null,
                        null,
                        null,
                        "0,1"
                );
            } else {
                // 其他消息按照 system type 分组
                cursor = db.query(
                        com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.TABLE_NAME_CONVERSATION,
                        columnsSelector.queryColumns(),
                        com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_SYSTEM_TYPE + "=?",
                        new String[]{
                                String.valueOf(systemType)
                        },
                        null,
                        null,
                        null,
                        "0,1"
                );
            }

            if (cursor.moveToNext()) {
                Conversation item = columnsSelector.cursorToObjectWithQueryColumns(cursor);
                Timber.v("found conversation with id:%s for session user id:%s, systemType:%s",
                        item.id, sessionUserId, systemType);
                return item;
            }

            // conversation not found
            Timber.v("conversation for session user id:%s, target user id:%s, systemType:%s not found, auto create:%s",
                    sessionUserId, targetUserId, systemType, autoCreate);
            if (autoCreate) {
                Conversation target = Conversation.valueOf(targetUserId, systemType);
                long rowId = db.insert(com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.TABLE_NAME_CONVERSATION, null, target.toContentValues());
                if (rowId != -1) {
                    return getTargetConversation(sessionUserId, targetUserId, systemType, false, columnsSelector);
                } else {
                    Throwable e = new IllegalAccessError("fail to create conversation for session user id:" + sessionUserId
                            + ", target user id:" + targetUserId + ", systemType:" + systemType);
                    Timber.e(e);
                }
            }
        } catch (Throwable e) {
            Timber.e(e);
        } finally {
            IOUtil.closeQuietly(cursor);
        }
        return null;
    }

    /**
     * @param conversation
     * @return 更新成功返回 true, 否则返回 false.
     */
    public boolean updateConversation(final long sessionUserId, final Conversation conversation) {
        if (conversation == null) {
            Throwable e = new IllegalArgumentException("conversation is null");
            Timber.e(e);
            return false;
        }

        if (conversation.id <= 0) {
            Throwable e = new IllegalArgumentException("invalid conversation id " + conversation.id);
            Timber.e(e);
            return false;
        }

        try {
            com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper dbHelper = com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            int rowsAffected = db.update(
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.TABLE_NAME_CONVERSATION,
                    conversation.toContentValues(),
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_ID + "=?",
                    new String[]{String.valueOf(conversation.id)}
            );
            if (rowsAffected != 1) {
                Throwable e = new IllegalAccessException("update conversation for session user id:" + sessionUserId + " with id:" + conversation.id + " affected " + rowsAffected + " rows");
                Timber.e(e);
            } else {
                Timber.v("update conversation for session user id:%s with id:%s affected %s rows", sessionUserId, conversation.id, rowsAffected);
            }
            return rowsAffected > 0;
        } catch (Throwable e) {
            Timber.e(e);
        }
        return false;
    }

    public boolean deleteConversation(final long sessionUserId, final Conversation conversation) {
        if (conversation == null) {
            Throwable e = new IllegalArgumentException("conversation is null");
            Timber.e(e);
            return false;
        }

        if (conversation.id <= 0) {
            Throwable e = new IllegalArgumentException("invalid conversation id " + conversation.id);
            Timber.e(e);
            return false;
        }

        try {
            com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper dbHelper = com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            int rowsAffected = db.delete(
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.TABLE_NAME_CONVERSATION,
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_ID + "=?",
                    new String[]{String.valueOf(conversation.id)}
            );
            if (rowsAffected != 1) {
                Throwable e = new IllegalAccessException("delete conversation for session user id:" + sessionUserId + " with id:" + conversation.id + " affected " + rowsAffected + " rows");
                Timber.e(e);
            } else {
                Timber.v("delete conversation for session user id:%s with id:%s affected %s rows", sessionUserId, conversation.id, rowsAffected);
            }
            return rowsAffected > 0;
        } catch (Throwable e) {
            Timber.e(e);
        }
        return false;
    }

    public boolean deleteAllConversation(final long sessionUserId, int systemType) {
        try {
            if (systemType != ImConstant.ConversationSystemType.SYSTEM_TYPE_CHAT) {
                Throwable e = new IllegalAccessException("WARN:: deleteAllConversation sessionUserId:" + sessionUserId + ", systemType:" + systemType + ", use update would be better.");
                Timber.e(e);
            }

            com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper dbHelper = com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseProvider.getInstance().getDBHelper(sessionUserId);
            SQLiteDatabase db = dbHelper.getDBHelper().getWritableDatabase();
            int rowsAffected = db.delete(
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.TABLE_NAME_CONVERSATION,
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_SYSTEM_TYPE + "=?",
                    new String[]{
                            String.valueOf(systemType)
                    }
            );
            Timber.v("delete all conversation  with session user id:%s, systemType:%s affected %s rows",
                    sessionUserId,
                    systemType,
                    rowsAffected);
            return true;
        } catch (Throwable e) {
            Timber.e(e);
        }
        return false;
    }

    public static final ColumnsSelector<Conversation> COLUMNS_SELECTOR_FULL = new ColumnsSelector<Conversation>() {
        @Override
        public String[] queryColumns() {
            return new String[]{
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_ID,
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_TARGET_USER_ID,
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_LAST_MSG_ID,
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_LATEST_UNREAD_GIFT_MSG_ID,
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_LAST_MODIFY,
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_TOP,
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_UNREAD_COUNT,
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_SYSTEM_TYPE,
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_DRAFT_TEXT,
                    com.xmqvip.xiaomaiquan.common.im.core.db.ImDatabaseHelper.ColumnsConversation.C_DRAFT_TIME
            };
        }

        @Override
        public Conversation cursorToObjectWithQueryColumns(Cursor cursor) {
            Conversation object = new Conversation();
            int index = -1;
            object.id = cursor.getLong(++index);
            object.targetUserId = cursor.getLong(++index);
            object.lastMsgId = cursor.getLong(++index);
            object.latestUnreadGiftMsgId = cursor.getLong(++index);
            object.lastModify = cursor.getLong(++index);
            object.top = cursor.getInt(++index);
            object.unreadCount = cursor.getLong(++index);
            object.systemType = cursor.getInt(++index);
            object.draftText = cursor.getString(++index);
            object.draftTime = cursor.getLong(++index);
            return object;
        }

    };

}
