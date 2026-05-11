package com.example.zalo.data.repository

import com.example.zalo.data.remote.api.AiApi
import com.example.zalo.data.remote.api.ChatApi
import com.example.zalo.data.remote.api.GroupApi
import com.example.zalo.data.remote.api.MessageApi
import com.example.zalo.data.remote.dto.*
import com.example.zalo.domain.repository.ChatRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val messageApi: MessageApi,
    private val groupApi: GroupApi,
    private val aiApi: AiApi
) : ChatRepository {

    override suspend fun getChats(): Result<List<ChatDto>> {
        return try {
            val response = chatApi.getAllChats()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMessages(chatId: String, page: Int): Result<List<MessageDto>> {
        return try {
            val response = messageApi.getMessages(chatId, page, 30)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(message: MessageDto): Result<MessageDto> {
        return try {
            val response = messageApi.sendMessage(message)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsSeen(chatId: String): Result<Unit> {
        return try {
            val response = messageApi.markAsSeen(chatId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGroups(): Result<List<GroupDto>> {
        return try {
            val response = groupApi.getMyGroups()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGroupMessages(groupId: String, page: Int): Result<List<GroupMessageDto>> {
        return try {
            val response = groupApi.getGroupMessages(groupId, page)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendGroupMessage(groupId: String, content: String): Result<GroupMessageDto> {
        return try {
            val response = groupApi.sendGroupMessage(groupId, SendGroupMessageRequest(content))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createGroup(name: String, memberIds: List<String>): Result<GroupDto> {
        return try {
            val response = groupApi.createGroup(CreateGroupRequest(name = name, memberIds = memberIds))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGroup(groupId: String): Result<GroupDto> {
        return try {
            val response = groupApi.getGroup(groupId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsDelivered(chatId: String): Result<Unit> {
        return try {
            val response = messageApi.markAsDelivered(chatId)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun recallMessage(messageId: String): Result<Unit> {
        return try {
            val response = messageApi.recallMessage(messageId)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteMessageForMe(messageId: String): Result<Unit> {
        return try {
            val response = messageApi.deleteMessageForMe(messageId)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun toggleReaction(messageId: String, emoji: String): Result<Unit> {
        return try {
            val response = messageApi.toggleReaction(messageId, emoji)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteReaction(messageId: String): Result<Unit> {
        return try {
            val response = messageApi.deleteReaction(messageId)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateGroup(groupId: String, name: String, description: String): Result<GroupDto> {
        return try {
            val response = groupApi.updateGroup(groupId, mapOf("name" to name, "description" to description))
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun addMembers(groupId: String, userIds: List<String>): Result<GroupDto> {
        return try {
            val response = groupApi.addMembers(groupId, userIds)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun removeMember(groupId: String, userId: String): Result<Unit> {
        return try {
            val response = groupApi.removeMember(groupId, userId)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun setMemberAsAdmin(groupId: String, userId: String): Result<Unit> {
        return try {
            val response = groupApi.setMemberAsAdmin(groupId, userId)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun leaveGroup(groupId: String): Result<Unit> {
        return try {
            val response = groupApi.leaveGroup(groupId)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun dissolveGroup(groupId: String): Result<Unit> {
        return try {
            val response = groupApi.dissolveGroup(groupId)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun pinMessage(groupId: String, messageId: String): Result<Unit> {
        return try {
            val response = groupApi.pinMessage(groupId, messageId)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun unpinMessage(groupId: String, messageId: String): Result<Unit> {
        return try {
            val response = groupApi.unpinMessage(groupId, messageId)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getPinnedMessages(groupId: String): Result<List<GroupMessageDto>> {
        return try {
            val response = groupApi.getPinnedMessages(groupId)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getJoinRequests(groupId: String): Result<List<GroupJoinRequestDto>> {
        return try {
            val response = groupApi.getJoinRequests(groupId)
            if (response.isSuccessful && response.body() != null) Result.success(response.body()!!) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun approveJoinRequest(groupId: String, requestId: String): Result<Unit> {
        return try {
            val response = groupApi.approveJoinRequest(groupId, requestId)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun rejectJoinRequest(groupId: String, requestId: String): Result<Unit> {
        return try {
            val response = groupApi.rejectJoinRequest(groupId, requestId)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun toggleGroupReaction(messageId: String, emoji: String): Result<Unit> {
        return try {
            val response = groupApi.toggleGroupReaction(messageId, emoji)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception(response.message()))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getSmartReply(chatId: String, isGroup: Boolean): Result<SmartReplyResponse> {
        return try {
            val response = if (isGroup) aiApi.getGroupSmartReply(chatId) else aiApi.getChatSmartReply(chatId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun summarize(chatId: String, isGroup: Boolean, lastNMessages: Int): Result<SummarizeResponse> {
        return try {
            val request = SummarizeRequest(lastNMessages)
            val response = if (isGroup) aiApi.summarizeGroup(chatId, request) else aiApi.summarizeChat(chatId, request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun chatWithAi(message: String): Result<AiMessageDto> {
        return try {
            val response = aiApi.chatWithAi(AiChatRequest(message))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAiHistory(page: Int): Result<List<AiMessageDto>> {
        return try {
            val response = aiApi.getAiHistory(page, 30)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.content)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearAiHistory(): Result<Unit> {
        return try {
            val response = aiApi.clearAiHistory()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadMedia(chatId: String, file: File): Result<MessageDto> {
        return try {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = messageApi.uploadMedia(chatId, body)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadGroupMedia(groupId: String, file: File): Result<GroupMessageDto> {
        return try {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = groupApi.uploadGroupMedia(groupId, body)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
