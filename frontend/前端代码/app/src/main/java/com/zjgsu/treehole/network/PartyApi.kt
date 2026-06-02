package com.zjgsu.treehole.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class PartyRoomDto(
    val id: String,
    val name: String,
    val subtitle: String,
    val participantCount: Int = 0,
    val maxParticipants: Int = 6,
    val onlineCount: Int,
    val heat: Int,
    val messageCount: Int,
    val lastMessage: String,
    val lastMessageAt: String?
)

data class PartyRoomsResponse(
    val rooms: List<PartyRoomDto>
)

data class CreatePartyRoomRequest(
    val name: String,
    val subtitle: String
)

data class CreatePartyRoomResponse(
    val message: String,
    val room: PartyRoomDto
)

data class PartyMessageDto(
    val id: String,
    val roomId: String,
    val senderId: String,
    val senderNickname: String,
    val content: String,
    val createdAt: String,
    val isSystemMessage: Boolean = false
)

data class PartyRoomInfoDto(
    val id: String,
    val name: String,
    val subtitle: String,
    val participantCount: Int = 0,
    val maxParticipants: Int = 6
)

data class PartyMessagesResponse(
    val room: PartyRoomInfoDto,
    val messages: List<PartyMessageDto>
)

data class SendPartyMessageRequest(
    val content: String
)

data class SendPartyMessageResponse(
    val message: PartyMessageDto
)

data class JoinRoomResponse(
    val message: String,
    val room: PartyRoomInfoDto
)

interface PartyApi {
    @POST("api/party/rooms")
    suspend fun createRoom(
        @Body request: CreatePartyRoomRequest
    ): Response<CreatePartyRoomResponse>

    @GET("api/party/rooms")
    suspend fun getRooms(): Response<PartyRoomsResponse>

    @POST("api/party/rooms/{roomId}/join")
    suspend fun joinRoom(
        @Path("roomId") roomId: String
    ): Response<JoinRoomResponse>

    @POST("api/party/rooms/{roomId}/leave")
    suspend fun leaveRoom(
        @Path("roomId") roomId: String
    ): Response<MessageResponse>

    @GET("api/party/messages/{roomId}")
    suspend fun getMessages(
        @Path("roomId") roomId: String,
        @Query("limit") limit: Int = 80
    ): Response<PartyMessagesResponse>

    @POST("api/party/messages/{roomId}")
    suspend fun sendMessage(
        @Path("roomId") roomId: String,
        @Body request: SendPartyMessageRequest
    ): Response<SendPartyMessageResponse>
}
