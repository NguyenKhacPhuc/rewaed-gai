package com.tcp.rewaed.data.repositories

import com.tcp.rewaed.data.models.ChatPostBody
import com.tcp.rewaed.data.models.ChatResponseBody
import com.tcp.rewaed.data.network.ApiInterface
import com.tcp.rewaed.data.network.SafeApiRequest
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val api: ApiInterface
) : SafeApiRequest() {

    suspend fun sendMessage(
        chatPostBody: ChatPostBody
    ): ChatResponseBody = apiRequest {
        api.sendMessage(chatPostBody)
    }
}