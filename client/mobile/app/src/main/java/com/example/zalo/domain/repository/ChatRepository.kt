package com.example.zalo.domain.repository

import com.example.zalo.data.remote.dto.*
import java.io.File

interface ChatRepository {
    suspend fun getChats(): Result<List<ChatDto>>
    suspend fun getMessages(chatId: String, page: Int): Result<List<MessageDto>>
    suspend fun sendMessage(message: MessageDto): Result<MessageDto>
    suspend fun markAsSeen(chatId: String): Result<Unit>

    // Group Chat
    suspend fun getGroups(): Result<List<GroupDto>>
    suspend fun getGroupMessages(groupId: String, page: Int): Result<List<GroupMessageDto>>
    suspend fun sendGroupMessage(groupId: String, content: String): Result<GroupMessageDto>
    suspend fun createGroup(name: String, memberIds: List<String>): Result<GroupDto>
    suspend fun getGroup(groupId: String): Result<GroupDto>

    // AI Features
    suspend fun getSmartReply(chatId: String, isGroup: Boolean): Result<SmartReplyResponse>
    suspend fun summarize(chatId: String, isGroup: Boolean, lastNMessages: Int): Result<SummarizeResponse>

    // Media Upload
    suspend fun uploadMedia(chatId: String, file: File): Result<MessageDto>
    suspend fun uploadGroupMedia(groupId: String, file: File): Result<GroupMessageDto>
}
