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

    // Advanced Message Actions
    suspend fun markAsDelivered(chatId: String): Result<Unit>
    suspend fun recallMessage(messageId: String): Result<Unit>
    suspend fun deleteMessageForMe(messageId: String): Result<Unit>
    suspend fun toggleReaction(messageId: String, emoji: String): Result<Unit>
    suspend fun deleteReaction(messageId: String): Result<Unit>

    // Advanced Group Actions
    suspend fun updateGroup(groupId: String, name: String, description: String): Result<GroupDto>
    suspend fun addMembers(groupId: String, userIds: List<String>): Result<GroupDto>
    suspend fun removeMember(groupId: String, userId: String): Result<Unit>
    suspend fun setMemberAsAdmin(groupId: String, userId: String): Result<Unit>
    suspend fun leaveGroup(groupId: String): Result<Unit>
    suspend fun dissolveGroup(groupId: String): Result<Unit>
    suspend fun pinMessage(groupId: String, messageId: String): Result<Unit>
    suspend fun unpinMessage(groupId: String, messageId: String): Result<Unit>
    suspend fun getPinnedMessages(groupId: String): Result<List<GroupMessageDto>>
    suspend fun getJoinRequests(groupId: String): Result<List<GroupJoinRequestDto>>
    suspend fun approveJoinRequest(groupId: String, requestId: String): Result<Unit>
    suspend fun rejectJoinRequest(groupId: String, requestId: String): Result<Unit>
    suspend fun toggleGroupReaction(messageId: String, emoji: String): Result<Unit>

    // AI Features
    suspend fun getSmartReply(chatId: String, isGroup: Boolean): Result<SmartReplyResponse>
    suspend fun summarize(chatId: String, isGroup: Boolean, lastNMessages: Int): Result<SummarizeResponse>
    suspend fun chatWithAi(message: String): Result<AiMessageDto>
    suspend fun getAiHistory(page: Int): Result<List<AiMessageDto>>
    suspend fun clearAiHistory(): Result<Unit>

    // Media Upload
    suspend fun uploadMedia(chatId: String, file: File): Result<MessageDto>
    suspend fun uploadGroupMedia(groupId: String, file: File): Result<GroupMessageDto>
}
