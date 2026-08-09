package com.firstapp.esehat

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class ChatMessage(val sender: String, val text: String, val createdAt: Long)
data class ConversationSummary(val id: Long, val title: String, val updatedAt: Long)
data class ConversationState(val facts: String, val started: Boolean)

/**
 * Local, on-device chat history. No server involved — this is separate from
 * the backend's own "facts" triage state, though we persist that here too
 * (per conversation) so resuming a chat also resumes its triage context.
 */
class ChatHistoryDbHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE conversations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                updated_at INTEGER NOT NULL,
                facts TEXT NOT NULL DEFAULT '{}',
                started INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                conversation_id INTEGER NOT NULL,
                sender TEXT NOT NULL,
                text TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS messages")
        db.execSQL("DROP TABLE IF EXISTS conversations")
        onCreate(db)
    }

    fun createConversation(initialTitle: String = "New chat"): Long {
        val values = ContentValues().apply {
            put("title", initialTitle)
            put("updated_at", System.currentTimeMillis())
        }
        return writableDatabase.insert("conversations", null, values)
    }

    fun renameConversation(conversationId: Long, title: String) {
        val values = ContentValues().apply { put("title", title) }
        writableDatabase.update("conversations", values, "id = ?", arrayOf(conversationId.toString()))
    }

    fun touchConversation(conversationId: Long) {
        val values = ContentValues().apply { put("updated_at", System.currentTimeMillis()) }
        writableDatabase.update("conversations", values, "id = ?", arrayOf(conversationId.toString()))
    }

    fun updateState(conversationId: Long, factsJson: String, started: Boolean) {
        val values = ContentValues().apply {
            put("facts", factsJson)
            put("started", if (started) 1 else 0)
        }
        writableDatabase.update("conversations", values, "id = ?", arrayOf(conversationId.toString()))
    }

    fun getConversationState(conversationId: Long): ConversationState {
        readableDatabase.rawQuery(
            "SELECT facts, started FROM conversations WHERE id = ?",
            arrayOf(conversationId.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return ConversationState(cursor.getString(0) ?: "{}", cursor.getInt(1) == 1)
            }
        }
        return ConversationState("{}", false)
    }

    fun appendMessage(conversationId: Long, sender: String, text: String) {
        val values = ContentValues().apply {
            put("conversation_id", conversationId)
            put("sender", sender)
            put("text", text)
            put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insert("messages", null, values)
        touchConversation(conversationId)
    }

    fun getMessageCount(conversationId: Long): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM messages WHERE conversation_id = ?",
            arrayOf(conversationId.toString())
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun getMessages(conversationId: Long): List<ChatMessage> {
        val list = mutableListOf<ChatMessage>()
        readableDatabase.rawQuery(
            "SELECT sender, text, created_at FROM messages WHERE conversation_id = ? ORDER BY id ASC",
            arrayOf(conversationId.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(ChatMessage(cursor.getString(0), cursor.getString(1), cursor.getLong(2)))
            }
        }
        return list
    }

    fun getRecentConversations(limit: Int = 20): List<ConversationSummary> {
        val list = mutableListOf<ConversationSummary>()
        readableDatabase.rawQuery(
            "SELECT id, title, updated_at FROM conversations ORDER BY updated_at DESC LIMIT ?",
            arrayOf(limit.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(ConversationSummary(cursor.getLong(0), cursor.getString(1), cursor.getLong(2)))
            }
        }
        return list
    }

    companion object {
        private const val DB_NAME = "baymax_chat_history.db"
        private const val DB_VERSION = 1
    }
}