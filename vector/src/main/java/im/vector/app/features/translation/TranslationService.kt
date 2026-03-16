/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.translation

import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationService @Inject constructor(
        private val config: TranslateConfig,
        private val cache: TranslationCache,
        private val rateLimiter: TranslationRateLimiter
) {
    private var currentApiUrl: String? = null
    private var currentApiKey: String? = null
    private var api: TranslateApi? = null
    private var currentClient: OkHttpClient? = null
    private val apiLock = Any()

    private val moshi: Moshi by lazy {
        Moshi.Builder().build()
    }

    init {
        cache.loadFromDisk()
    }

    private fun getApi(): TranslateApi {
        synchronized(apiLock) {
            val url = config.apiUrl.trimEnd('/')
            val key = config.apiKey
            if (api == null || currentApiUrl != url || currentApiKey != key) {
                // Shutdown previous client to avoid resource leaks
                currentClient?.dispatcher?.executorService?.shutdown()
                currentClient?.connectionPool?.evictAll()

                currentApiUrl = url
                currentApiKey = key
                val client = OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .apply {
                            if (key.isNotBlank()) {
                                addInterceptor(Interceptor { chain ->
                                    val request = chain.request().newBuilder()
                                            .addHeader("Authorization", "Bearer $key")
                                            .build()
                                    chain.proceed(request)
                                })
                            }
                        }
                        .build()
                currentClient = client

                val baseUrl = if (url.endsWith("/")) url else "$url/"
                api = Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .client(client)
                        .addConverterFactory(MoshiConverterFactory.create(moshi))
                        .build()
                        .create(TranslateApi::class.java)
            }
            return api!!
        }
    }

    suspend fun translate(text: String, targetLanguage: String): String? {
        if (text.isBlank()) return text

        // Check cache first
        cache.get(text, targetLanguage)?.let { return it }

        return try {
            rateLimiter.execute {
                val request = ChatCompletionRequest(
                        model = config.model,
                        messages = listOf(
                                ChatMessage(
                                        role = "system",
                                        content = "You are a translator. Translate the following message to $targetLanguage. " +
                                                "Reply ONLY with the translation, no explanations, no quotes, no extra text. " +
                                                "If the text is already in $targetLanguage, return it as-is. " +
                                                "Do NOT include any HTML tags, XML tags, or HTML entities in your response. " +
                                                "Return only plain text. No <p>, <br>, &quot;, &amp;, etc."
                                ),
                                ChatMessage(
                                        role = "user",
                                        content = text
                                )
                        )
                )
                val response = getApi().translate(request)
                val translated = response.choices.firstOrNull()?.message?.content?.trim()
                if (translated != null) {
                    cache.put(text, targetLanguage, translated)
                }
                translated
            }
        } catch (e: Exception) {
            Timber.e(e, "Translation failed for text: ${text.take(50)}...")
            null
        }
    }

    suspend fun translateOutgoing(text: String): String {
        val roomLang = config.roomLanguage
        if (!config.enabled || roomLang.isBlank()) return text
        return translate(text, roomLang) ?: text
    }

    suspend fun testConnection(): Result<List<String>> {
        return try {
            val response = getApi().listModels()
            Result.success(response.data.map { it.id })
        } catch (e: Exception) {
            Timber.e(e, "Test connection failed")
            Result.failure(e)
        }
    }

    suspend fun complete(systemPrompt: String, userMessage: String): String? {
        return try {
            rateLimiter.execute {
                val request = ChatCompletionRequest(
                        model = config.model,
                        messages = listOf(
                                ChatMessage(role = "system", content = systemPrompt),
                                ChatMessage(role = "user", content = userMessage)
                        )
                )
                val response = getApi().translate(request)
                response.choices.firstOrNull()?.message?.content?.trim()
            }
        } catch (e: Exception) {
            Timber.e(e, "AI completion failed")
            null
        }
    }

    fun getCacheSize(): Int = cache.size()

    fun clearCache() = cache.clear()
}
