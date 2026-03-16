/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.translation

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface TranslateApi {
    @POST("chat/completions")
    suspend fun translate(@Body request: ChatCompletionRequest): ChatCompletionResponse

    @GET("models")
    suspend fun listModels(): ModelsResponse
}

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
        @Json(name = "model") val model: String,
        @Json(name = "messages") val messages: List<ChatMessage>,
        @Json(name = "temperature") val temperature: Double = 0.1,
        @Json(name = "max_tokens") val maxTokens: Int = 2048
)

@JsonClass(generateAdapter = true)
data class ChatMessage(
        @Json(name = "role") val role: String,
        @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class ChatCompletionResponse(
        @Json(name = "choices") val choices: List<ChatChoice>
)

@JsonClass(generateAdapter = true)
data class ChatChoice(
        @Json(name = "message") val message: ChatMessage
)

@JsonClass(generateAdapter = true)
data class ModelsResponse(
        @Json(name = "data") val data: List<ModelInfo>
)

@JsonClass(generateAdapter = true)
data class ModelInfo(
        @Json(name = "id") val id: String
)
