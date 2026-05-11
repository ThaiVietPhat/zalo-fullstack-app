package com.example.zalo.data.remote

import android.util.Log
import com.example.zalo.data.local.TokenManager
import com.example.zalo.data.remote.dto.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.stomp.conversions.convertAndSendText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val tokenManager: TokenManager,
    private val networkConfig: NetworkConfig
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var session: StompSession? = null
    private val json = Json { ignoreUnknownKeys = true }

    private val _messages = MutableSharedFlow<MessageDto>()
    val messages: SharedFlow<MessageDto> = _messages.asSharedFlow()

    private val _groupMessages = MutableSharedFlow<GroupMessageDto>()
    val groupMessages: SharedFlow<GroupMessageDto> = _groupMessages.asSharedFlow()

    private val _typingStatus = MutableSharedFlow<Pair<String, TypingPayload>>()
    val typingStatus: SharedFlow<Pair<String, TypingPayload>> = _typingStatus.asSharedFlow()

    private val _callSignals = MutableSharedFlow<CallSignalDto>()
    val callSignals: SharedFlow<CallSignalDto> = _callSignals.asSharedFlow()

    fun connect() {
        val token = tokenManager.getToken() ?: return
        if (session != null) return

        scope.launch {
            try {
                val client = StompClient(OkHttpWebSocketClient())
                val wsUrl = networkConfig.railwayWsUrl
                session = client.connect(url = wsUrl, customHeaders = mapOf("Authorization" to "Bearer $token"))
                Log.d("WebSocketManager", "Connected to $wsUrl")
                
                val userId = tokenManager.getUserId() ?: return@launch
                subscribeToUserNotifications(userId)
                subscribeToCallSignals(userId)
                
            } catch (e: Exception) {
                Log.e("WebSocketManager", "Connection failed", e)
                session = null
            }
        }
    }

    private suspend fun subscribeToUserNotifications(userId: String) {
        session?.subscribeText("/user/queue/messages")?.collect { body ->
            try { _messages.emit(json.decodeFromString(body)) } catch (e: Exception) { Log.e("WebSocketManager", "Error", e) }
        }
    }

    private suspend fun subscribeToCallSignals(userId: String) {
        session?.subscribeText("/topic/call/$userId")?.collect { body ->
            try { _callSignals.emit(json.decodeFromString(body)) } catch (e: Exception) { Log.e("WebSocketManager", "Error", e) }
        }
    }

    fun subscribeToChat(chatId: String) {
        scope.launch {
            session?.subscribeText("/topic/chat/$chatId")?.collect { body ->
                try { _messages.emit(json.decodeFromString(body)) } catch (e: Exception) { Log.e("WebSocketManager", "Error", e) }
            }
        }
        scope.launch {
            session?.subscribeText("/topic/chat/$chatId/typing")?.collect { body ->
                try { _typingStatus.emit(chatId to json.decodeFromString(body)) } catch (e: Exception) { Log.e("WebSocketManager", "Error", e) }
            }
        }
    }

    fun subscribeToGroup(groupId: String) {
        scope.launch {
            session?.subscribeText("/topic/group/$groupId")?.collect { body ->
                try { _groupMessages.emit(json.decodeFromString(body)) } catch (e: Exception) { Log.e("WebSocketManager", "Error", e) }
            }
        }
        scope.launch {
            session?.subscribeText("/topic/group/$groupId/typing")?.collect { body ->
                try { _typingStatus.emit(groupId to json.decodeFromString(body)) } catch (e: Exception) { Log.e("WebSocketManager", "Error", e) }
            }
        }
    }

    fun sendTyping(id: String, isTyping: Boolean, isGroup: Boolean = false) {
        scope.launch {
            val dest = if (isGroup) "/app/group/$id/typing" else "/app/chat/$id/typing"
            session?.convertAndSendText(dest, mapOf("typing" to isTyping), kotlinx.serialization.serializer())
        }
    }

    fun sendCallSignal(signal: CallSignalDto) {
        scope.launch {
            session?.convertAndSendText("/app/call/signal", signal, kotlinx.serialization.serializer())
        }
    }

    fun disconnect() {
        scope.launch { session?.disconnect(); session = null }
    }
}
