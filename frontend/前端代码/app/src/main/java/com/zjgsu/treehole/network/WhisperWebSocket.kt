package com.zjgsu.treehole.network

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.*
import java.util.concurrent.TimeUnit

/**
 * WebSocket client for real-time whisper chat
 * Uses Socket.IO protocol
 */
object WhisperWebSocket {

    private const val TAG = "WhisperWS"
    private val socketUrlCandidates: List<String>
        get() {
            val baseUrl = RetrofitClient.BASE_URL.trimEnd('/')
            return listOf("$baseUrl/socket.io/?EIO=4&transport=websocket")
        }

    @Volatile
    private var activeSocketUrl = ""

    // Events
    interface OnWhisperListener {
        fun onNewMessage(message: WhisperMessage)
        fun onMessageSent(message: WhisperMessage)
        fun onMessageError(message: String)
        fun onMessageLimitReached()
        fun onMessageLimitLifted(roomId: String)
        fun onUnreadUpdate(roomId: String, count: Int)
        fun onFollowUpdated(userId: String, isFollowing: Boolean)
        fun onOnlineUsersUpdated(onlineUserIds: List<String>)
        fun onConnected()
        fun onDisconnected()
        fun onSoulMatchFound(roomId: String, participantId: String, nickname: String, avatar: String) {}
    }

    // Party room events listener
    interface OnPartyRoomListener {
        fun onPartyRoomCreated(room: PartyRoomDto)
        fun onPartyRoomDismissed(roomId: String) {}
        fun onNewPartyMessage(message: com.zjgsu.treehole.network.PartyMessageDto) {}
    }

    data class PartyRoomDto(
        val id: String,
        val name: String,
        val subtitle: String,
        val participantCount: Int,
        val maxParticipants: Int,
        val onlineCount: Int,
        val heat: Int,
        val messageCount: Int,
        val lastMessage: String,
        val lastMessageAt: String?,
        val avatars: List<String>? = emptyList()
    )

    data class WhisperMessage(
        val id: String = "",
        val chatId: String = "",
        val from: String = "",
        val fromNickname: String = "",
        val content: String = "",
        val timestamp: String = "",  // ISO 8601 format
        val messageLimitReached: Boolean = false
    )

    private var webSocket: WebSocket? = null
    private var listener: OnWhisperListener? = null
    private var partyRoomListener: OnPartyRoomListener? = null
    private var connectionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(5000, TimeUnit.MILLISECONDS)
        .build()

    private var currentRoomId: String? = null
    private var currentUserId: String? = null
    private var currentNickname: String? = null
    private var isConnected = false
    private var isConnecting = false
    private var isHandshakeComplete = false

    fun connect(userId: String, nickname: String, listener: OnWhisperListener) {
        // If already connected with same user, just update listener and room
        if (isConnected && currentUserId == userId) {
            Log.d(TAG, "Already connected with same user, updating listener")
            this.listener = listener
            scope.launch(Dispatchers.Main) {
                listener.onConnected()
            }
            return
        }

        // If connected with different user or state is wrong, disconnect first
        if (isConnected || isConnecting) {
            Log.d(TAG, "Disconnecting existing connection before new connect")
            disconnectImmediate()
        }

        this.listener = listener
        this.currentUserId = userId
        this.currentNickname = nickname

        connectionJob?.cancel()
        connectionJob = scope.launch {
            try {
                isConnecting = true
                isHandshakeComplete = false
                val currentCandidates = socketUrlCandidates
                activeSocketUrl = if (activeSocketUrl.isNotEmpty()) activeSocketUrl else currentCandidates.first()
                val orderedUrls = listOf(activeSocketUrl) +
                    currentCandidates.filter { it != activeSocketUrl }

                fun connectWithCandidate(index: Int) {
                    if (index >= orderedUrls.size) {
                        isConnected = false
                        isConnecting = false
                        this@WhisperWebSocket.listener?.onDisconnected()
                        return
                    }

                    val candidateUrl = orderedUrls[index]
                    val request = Request.Builder()
                        .url(candidateUrl)
                        .build()

                    Log.d(TAG, "Trying WebSocket endpoint: $candidateUrl")
                    webSocket = client.newWebSocket(request, object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            Log.d(TAG, "WebSocket onOpen called, response: $response")
                            Log.d(TAG, "Waiting for Socket.IO handshake from server...")
                            isConnected = true
                            activeSocketUrl = candidateUrl
                        }

                        override fun onMessage(webSocket: WebSocket, text: String) {
                            Log.d(TAG, "WS message: $text")
                            // Handle Socket.IO protocol messages
                            if (!handleSocketIOMessage(text)) {
                                handleMessage(text)
                            }
                        }

                        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                            Log.d(TAG, "WS closing: $code $reason")
                            webSocket.close(1000, null)
                        }

                        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                            Log.d(TAG, "WS closed: $code $reason")
                            isConnected = false
                            isConnecting = false
                            this@WhisperWebSocket.listener?.onDisconnected()
                        }

                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            Log.e(TAG, "WS failure on $candidateUrl: ${t.message}")
                            isConnected = false

                            // Try next candidate before declaring disconnected
                            if (!isHandshakeComplete) {
                                connectWithCandidate(index + 1)
                                return
                            }

                            isConnecting = false
                            this@WhisperWebSocket.listener?.onDisconnected()
                        }
                    })
                }

                connectWithCandidate(0)
            } catch (e: Exception) {
                Log.e(TAG, "Connect error: ${e.message}")
                isConnecting = false
                listener.onDisconnected()
            }
        }
    }

    /**
     * Handle Socket.IO protocol messages
     * Returns true if the message was handled (e.g., ping/pong)
     */
    private fun handleSocketIOMessage(text: String): Boolean {
        Log.d(TAG, "handleSocketIOMessage received: '$text'")
        when {
            text == "2" -> {
                // Ping - respond with pong immediately
                Log.d(TAG, "Received ping, sending pong")
                webSocket?.send("3")
                return true
            }
            text.startsWith("0{") -> {
                // Socket.IO initial handshake - server sends {"sid":"xxx",...}
                // Client must respond with "40" to connect to namespace
                Log.d(TAG, "Socket.IO handshake (0{) received: $text")
                // Send namespace connect (40 with no payload)
                webSocket?.send("40")
                Log.d(TAG, "Sent 40 to complete handshake")
                return true
            }
            text == "0" -> {
                // Socket.IO connect message (without sid) - older protocol
                Log.d(TAG, "Socket.IO connected (0), sending 40")
                webSocket?.send("40")
                return true
            }
            text.startsWith("40") && !text.startsWith("42") -> {
                // Socket.IO connect with namespace confirmed (40 or 40{...})
                Log.d(TAG, "Socket.IO namespace connected: $text")
                isHandshakeComplete = true
                // Now send user-online event
                scope.launch {
                    delay(100)
                    if (isConnected && currentUserId != null) {
                        if (!TokenManager.isIncognitoModeEnabled()) {
                            Log.d(TAG, "Sending user-online event")
                            sendEvent("user-online", currentUserId!!)
                        } else {
                            Log.d(TAG, "Incognito mode enabled, skip user-online event")
                        }
                        scope.launch(Dispatchers.Main) {
                            listener?.onConnected()
                        }
                    }
                }
                return true
            }
        }
        return false
    }

    private fun handleMessage(text: String) {
        Log.d(TAG, "handleMessage received: '$text'")
        try {
            // Socket.IO event messages start with "42"
            if (!text.startsWith("42")) {
                Log.d(TAG, "Message does not start with 42, ignoring")
                return
            }

            val data = text.substringAfter("42")
            if (data.isEmpty()) return

            val jsonArray = gson.fromJson(data, Array<Any>::class.java)
            val eventName = jsonArray.getOrNull(0) as? String ?: return
            val eventDataRaw = jsonArray.getOrNull(1)

            // Handle users-online specially - it sends an array directly, not an object
            if (eventName == "users-online") {
                val onlineIds = try {
                    val rawArray = org.json.JSONArray(data)
                    val list = mutableListOf<String>()
                    if (rawArray.length() > 1) {
                        val idsArray = rawArray.getJSONArray(1)
                        for (i in 0 until idsArray.length()) {
                            val id = idsArray.getString(i)
                            if (id.length > 10) list.add(id)
                        }
                    }
                    list
                } catch (e: Exception) {
                    Log.e(TAG, "Parse online users error: ${e.message}")
                    emptyList()
                }
                scope.launch(Dispatchers.Main) {
                    listener?.onOnlineUsersUpdated(onlineIds)
                }
                return
            }

            // Handle party-room-dismissed
            if (eventName == "party-room-dismissed") {
                val dismissData = eventDataRaw as? Map<*, *> ?: return
                val dismissRoomId = dismissData["roomId"] as? String ?: return
                scope.launch(Dispatchers.Main) {
                    partyRoomListener?.onPartyRoomDismissed(dismissRoomId)
                }
                return
            }

            val eventData = eventDataRaw as? Map<String, Any> ?: return
            handleEvent(eventName, eventData)
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
        }
    }

    private fun handleEvent(event: String, data: Map<String, Any>) {
        when (event) {
            "new-message" -> {
                val msg = WhisperMessage(
                    id = data["id"] as? String ?: "",
                    chatId = data["chatId"] as? String ?: "",
                    from = data["from"] as? String ?: "",
                    fromNickname = data["fromNickname"] as? String ?: "匿名",
                    content = data["content"] as? String ?: "",
                    timestamp = data["timestamp"] as? String ?: "",
                    messageLimitReached = data["messageLimitReached"] as? Boolean ?: false
                )
                scope.launch(Dispatchers.Main) {
                    listener?.onNewMessage(msg)
                }
            }

            "message-error" -> {
                scope.launch(Dispatchers.Main) {
                    listener?.onMessageError(data["message"] as? String ?: "发送失败")
                }
            }

            "message-limit-lifted" -> {
                val roomId = data["roomId"] as? String ?: ""
                scope.launch(Dispatchers.Main) {
                    listener?.onMessageLimitLifted(roomId)
                }
            }

            "unread-update" -> {
                val roomId = data["roomId"] as? String ?: ""
                val count = (data["unreadCount"] as? Number)?.toInt() ?: 0
                scope.launch(Dispatchers.Main) {
                    listener?.onUnreadUpdate(roomId, count)
                }
            }

            "follow-updated" -> {
                val userId = data["targetUserId"] as? String ?: ""
                val isFollowing = data["isFollowing"] as? Boolean ?: false
                scope.launch(Dispatchers.Main) {
                    listener?.onFollowUpdated(userId, isFollowing)
                }
            }

            "soul-match-found" -> {
                val roomId = data["roomId"] as? String ?: ""
                val participantId = data["participantId"] as? String ?: ""
                val nickname = data["nickname"] as? String ?: ""
                val avatar = data["avatar"] as? String ?: ""
                scope.launch(Dispatchers.Main) {
                    listener?.onSoulMatchFound(roomId, participantId, nickname, avatar)
                }
            }

            "read-receipt" -> {
                // Handle read receipt
            }

            "party-room-created" -> {
                val room = PartyRoomDto(
                    id = data["id"] as? String ?: "",
                    name = data["name"] as? String ?: "",
                    subtitle = data["subtitle"] as? String ?: "",
                    participantCount = (data["participantCount"] as? Number)?.toInt() ?: 0,
                    maxParticipants = (data["maxParticipants"] as? Number)?.toInt() ?: 6,
                    onlineCount = (data["onlineCount"] as? Number)?.toInt() ?: 0,
                    heat = (data["heat"] as? Number)?.toInt() ?: 0,
                    messageCount = (data["messageCount"] as? Number)?.toInt() ?: 0,
                    lastMessage = data["lastMessage"] as? String ?: "",
                    lastMessageAt = data["lastMessageAt"] as? String,
                    avatars = (data["avatars"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                )
                scope.launch(Dispatchers.Main) {
                    partyRoomListener?.onPartyRoomCreated(room)
                }
            }

            "new-party-message" -> {
                val msg = com.zjgsu.treehole.network.PartyMessageDto(
                    id = data["id"] as? String ?: "",
                    roomId = data["roomId"] as? String ?: "",
                    senderId = data["senderId"] as? String ?: "",
                    senderNickname = data["senderNickname"] as? String ?: "匿名用户",
                    content = data["content"] as? String ?: "",
                    createdAt = data["createdAt"] as? String ?: "",
                    isSystemMessage = data["isSystemMessage"] as? Boolean ?: false
                )
                scope.launch(Dispatchers.Main) {
                    partyRoomListener?.onNewPartyMessage(msg)
                }
            }
        }
    }

    private fun sendEvent(event: String, data: Map<String, Any>) {
        try {
            val jsonArray = gson.toJson(arrayOf(event, data))
            val message = "42$jsonArray"
            Log.d(TAG, "Sending: $message")
            webSocket?.send(message)
        } catch (e: Exception) {
            Log.e(TAG, "Send error: ${e.message}")
        }
    }

    private fun sendEvent(event: String, data: String) {
        try {
            val jsonArray = gson.toJson(arrayOf(event, data))
            val message = "42$jsonArray"
            Log.d(TAG, "Sending: $message")
            webSocket?.send(message)
        } catch (e: Exception) {
            Log.e(TAG, "Send error: ${e.message}")
        }
    }

    fun joinRoom(roomId: String, userId: String) {
        this.currentRoomId = roomId
        sendEvent("join-room", mapOf("roomId" to roomId, "userId" to userId))
    }

    fun leaveRoom(roomId: String) {
        sendEvent("leave-room", mapOf("roomId" to roomId))
        currentRoomId = null
    }

    fun sendMessage(content: String) {
        Log.d(TAG, "sendMessage called, isConnected=$isConnected, roomId=$currentRoomId")
        val userId = currentUserId ?: run {
            Log.d(TAG, "currentUserId is null, cannot send")
            return
        }
        val nickname = currentNickname ?: "匿名"
        val roomId = currentRoomId ?: run {
            Log.d(TAG, "currentRoomId is null, cannot send")
            return
        }
        if (!isConnected) {
            Log.d(TAG, "WebSocket not connected, cannot send")
            return
        }

        sendEvent("send-message", mapOf(
            "roomId" to roomId,
            "senderId" to userId,
            "senderNickname" to nickname,
            "content" to content
        ))
        Log.d(TAG, "sendMessage completed")
    }

    fun markAsRead(roomId: String) {
        val userId = currentUserId ?: return
        sendEvent("mark-read", mapOf("roomId" to roomId, "userId" to userId))
    }

    fun followUser(targetUserId: String) {
        val userId = currentUserId ?: return
        sendEvent("follow-user", mapOf(
            "currentUserId" to userId,
            "targetUserId" to targetUserId
        ))
    }

    fun requestOnlineUsers() {
        if (!TokenManager.isIncognitoModeEnabled()) {
            sendEvent("request-online-users", "")
        }
    }

    fun requestSoulMatch() {
        val userId = currentUserId ?: return
        sendEvent("soul-match-request", mapOf("userId" to userId))
    }

    fun cancelSoulMatch() {
        sendEvent("soul-match-cancel", emptyMap<String, String>())
    }

    fun setPartyRoomListener(listener: OnPartyRoomListener?) {
        this.partyRoomListener = listener
    }

    private fun disconnectImmediate() {
        try {
            webSocket?.close(1000, "Disconnect")
        } catch (e: Exception) { }
        webSocket = null
        isConnected = false
        isConnecting = false
        isHandshakeComplete = false
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        isConnecting = false
        currentRoomId?.let { roomId ->
            sendEvent("leave-room", mapOf("roomId" to roomId))
        }
        disconnectImmediate()
        listener = null
    }

    // Reset connection state for reconnection (without closing WebSocket)
    fun resetConnection() {
        disconnect()  // Properly close WebSocket before reset
    }

    // Check if WebSocket is connected
    fun isConnected(): Boolean = isConnected

    fun onDestroy() {
        disconnect()
        scope.cancel()
    }
}
