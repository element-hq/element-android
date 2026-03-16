# Element Android — LLM Translation System Porting Guide

This document describes the complete translation system implemented in Element Desktop and how to port it to Element Android (or Element X Android). Give this file to an AI assistant working on the Android project.

---

## Overview

Element Desktop now has a real-time LLM translation system using any OpenAI-compatible API (Ollama, OpenAI, LM Studio, LocalAI, etc.). The goal is to replicate the same features on Android.

### Features to implement

| Feature | Description |
|---------|-------------|
| **Auto-translate incoming** | All received messages are translated to the user's language automatically |
| **Translate button per message** | A "Translate" button appears on each message (on long-press or as an icon) |
| **Reply quote translation** | Quoted messages in replies are also translated |
| **Selection translation** | Selected text can be translated on demand |
| **Notification translation** | Incoming notifications are translated before display |
| **Outgoing message translation** | Messages written in user's language are translated to the room's language before sending |
| **Persistent cache** | Translations are cached in memory + on disk, surviving app restarts |
| **Rate limiting** | Max 3 concurrent API requests, others queued |
| **Settings UI** | Full settings screen with API config, toggles, test button, cache management |
| **Keyboard/shortcut toggle** | Quick toggle for auto-translate |

---

## 1. Configuration

### Data model

```kotlin
data class TranslateConfig(
    val apiUrl: String = "http://localhost:11434/v1",  // or user's server
    val apiKey: String = "",                            // Bearer token, empty for local models
    val model: String = "llama3",                      // LLM model name
    val targetLanguage: String = "French",             // User's language (read messages in this)
    val roomLanguage: String = "",                      // Room's language (send messages in this, empty = disabled)
    val enabled: Boolean = false,                      // Master toggle
    val autoTranslate: Boolean = false                 // Auto-translate all incoming messages
)
```

### Storage

Use `SharedPreferences` or Jetpack `DataStore` with keys:
- `translate_api_url`
- `translate_api_key`
- `translate_model`
- `translate_target_language`
- `translate_room_language`
- `translate_enabled`
- `translate_auto_translate`

---

## 2. Translation API

### Endpoint: Chat Completions (OpenAI-compatible)

```
POST {apiUrl}/chat/completions
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {apiKey}    // only if apiKey is not empty
```

**Request body:**
```json
{
    "model": "llama3",
    "messages": [
        {
            "role": "system",
            "content": "You are a translator. Translate the following message to {targetLanguage}. Reply ONLY with the translation, no explanations, no quotes, no extra text. If the text is already in {targetLanguage}, return it as-is. Preserve any formatting, markdown, links, and mentions exactly as they are."
        },
        {
            "role": "user",
            "content": "{text to translate}"
        }
    ],
    "temperature": 0.1,
    "max_tokens": 2048
}
```

**Response (extract `choices[0].message.content`):**
```json
{
    "choices": [
        {
            "message": {
                "content": "translated text here"
            }
        }
    ]
}
```

### Endpoint: Test Connection

```
GET {apiUrl}/models
```

Returns available models. Use to validate URL + API key.

### Implementation (Kotlin/Retrofit)

```kotlin
interface TranslateApi {
    @POST("chat/completions")
    suspend fun translate(@Body request: ChatCompletionRequest): ChatCompletionResponse

    @GET("models")
    suspend fun listModels(): ModelsResponse
}

data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.1,
    val max_tokens: Int = 2048
)

data class Message(val role: String, val content: String)

data class ChatCompletionResponse(val choices: List<Choice>)
data class Choice(val message: Message)

data class ModelsResponse(val data: List<ModelInfo>)
data class ModelInfo(val id: String)
```

---

## 3. Cache System (3 levels)

### Level 1: In-memory cache (fastest, per-session)

```kotlin
object TranslationCache {
    private val memoryCache = LinkedHashMap<String, String>(5000, 0.75f, true)
    private const val MAX_SIZE = 5000

    fun key(text: String, lang: String) = "$lang:$text"

    fun get(text: String, lang: String): String? = memoryCache[key(text, lang)]

    fun put(text: String, lang: String, translated: String) {
        if (memoryCache.size >= MAX_SIZE) {
            memoryCache.remove(memoryCache.keys.first())
        }
        memoryCache[key(text, lang)] = translated
    }

    fun clear() = memoryCache.clear()
    fun size() = memoryCache.size
}
```

### Level 2: Disk cache (survives app restarts)

Use a JSON file or Room database:

```kotlin
// File-based (simple):
// Location: context.filesDir / "translate-cache.json"
// Format: JSON array of [key, value] pairs
// Save debounced every 2 seconds after last write
// Load on app startup

// Or Room database (more robust):
@Entity(tableName = "translation_cache")
data class CachedTranslation(
    @PrimaryKey val cacheKey: String,   // "French:Hello world"
    val translatedText: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

### Cache flow

```
translate(text, lang) →
    1. Check memory cache → hit? return immediately
    2. Check disk cache → hit? add to memory, return
    3. Call API → store in memory + schedule disk save
```

---

## 4. Rate Limiting

Max 3 concurrent API calls, queue the rest:

```kotlin
class TranslationRateLimiter(private val maxConcurrent: Int = 3) {
    private val semaphore = Semaphore(maxConcurrent)

    suspend fun <T> execute(block: suspend () -> T): T {
        semaphore.acquire()
        return try {
            block()
        } finally {
            semaphore.release()
        }
    }
}
```

---

## 5. Auto-translate incoming messages

### Where to hook

In the message list adapter/composable, when binding a message item:

```kotlin
// Pseudocode for RecyclerView adapter or Compose LazyColumn
fun bindMessage(event: TimelineEvent) {
    val body = event.root.getClearContent()?.get("body") as? String ?: return

    if (config.enabled && config.autoTranslate) {
        // Check cache first (instant)
        val cached = TranslationCache.get(body, config.targetLanguage)
        if (cached != null) {
            showTranslated(cached, originalText = body)
            return
        }
        // Translate async
        scope.launch {
            showTranslating()  // show "..." indicator
            val translated = translateService.translate(body, config.targetLanguage)
            showTranslated(translated, originalText = body)
        }
    }
}
```

### UI for translated messages

- Show translated text as the main content
- Add a collapsible "Original" section below (like a `<details>` in HTML)
- Or show original on long-press

---

## 6. Translate button per message

### Option A: Action in message long-press menu

Add "Translate" to the existing long-press context menu on messages. This is the simplest approach.

### Option B: Floating button (like reactions)

Add a small translate icon button that appears near the message actions (reply, react, etc.).

### Behavior

1. User taps "Translate"
2. Show loading indicator on message
3. Call translation API
4. Replace message text with translation
5. Show "Original" toggle below

---

## 7. Reply quote translation

When a message contains a reply (quoted message), also translate the quoted text.

In Element Android, replies have a `RelatesTo` field with the original event. When rendering the reply preview:

```kotlin
fun bindReplyPreview(repliedEvent: TimelineEvent) {
    val replyBody = repliedEvent.root.getClearContent()?.get("body") as? String
    if (replyBody != null && config.enabled) {
        val translated = TranslationCache.get(replyBody, config.targetLanguage)
            ?: translateService.translate(replyBody, config.targetLanguage)
        showReplyBody(translated)
    }
}
```

---

## 8. Notification translation

### Where to hook

Intercept notification creation. In Element Android, notifications are built in a `NotificationFactory` or similar class.

```kotlin
// Before showing the notification:
suspend fun translateNotification(title: String, body: String): Pair<String, String> {
    if (!config.enabled) return title to body

    val translatedTitle = translateService.translate(title, config.targetLanguage)
    val translatedBody = translateService.translate(body, config.targetLanguage)
    return (translatedTitle ?: title) to (translatedBody ?: body)
}

// In notification builder:
val (title, body) = translateNotification(originalTitle, originalBody)
builder.setContentTitle(title)
builder.setContentText(body)
```

---

## 9. Outgoing message translation

### Concept

User writes in French → message is translated to English before being sent to the Matrix server → auto-translate shows it back in French locally.

### Where to hook

Intercept the message send function in the Matrix SDK:

```kotlin
// Before sending:
suspend fun translateOutgoing(messageText: String): String {
    val roomLang = config.roomLanguage
    if (!config.enabled || roomLang.isBlank()) return messageText

    return translateService.translate(messageText, roomLang) ?: messageText
}

// In the send message flow:
fun sendMessage(roomId: String, text: String) {
    scope.launch {
        val translatedText = translateOutgoing(text)
        // Send translatedText to the room
        room.sendTextMessage(translatedText)
        // The auto-translate will show it back in French to the user
    }
}
```

### Important

- Only translate `m.text` message types
- Also translate `formatted_body` if present (HTML formatted messages)
- Don't translate media, stickers, or other message types

---

## 10. Settings UI

### Android Settings Screen

Create a new settings fragment/composable accessible from the app's settings menu.

**Fields:**

| Field | Type | Description |
|-------|------|-------------|
| Enable Translation | Switch | Master toggle |
| Auto-translate all messages | Switch | Translate everything automatically |
| API URL | EditText | URL of the OpenAI-compatible API |
| Test Connection | Button | Tests the URL, shows available models |
| API Key | EditText (password) | Bearer token, optional for local models |
| Model | EditText | Model name (e.g., llama3, gpt-4o-mini) |
| Target Language | EditText | User's language (e.g., "French") |
| Room Language | EditText | Room's language for outgoing translation (e.g., "English"), empty = disabled |
| Cache info | Text | "X translations cached" |
| Clear Cache | Button | Empties all caches |

### Test Connection button

```kotlin
btnTest.setOnClickListener {
    scope.launch {
        try {
            showLoading()
            val result = api.listModels()
            showSuccess("Connected! Models: ${result.data.joinToString { it.id }}")
        } catch (e: Exception) {
            showError("Failed: ${e.message}")
        }
    }
}
```

---

## 11. Quick toggle

### Option A: Quick Settings Tile

Register an Android Quick Settings tile that toggles auto-translate on/off.

### Option B: Notification action

Add a persistent notification with a "Toggle Translation" action button.

### Option C: In-app shortcut

Add a floating action button or toolbar icon in the chat screen to toggle.

---

## 12. Architecture summary

```
┌─────────────────────────────────────────────┐
│                   Settings UI               │
│  (PreferenceFragment / Compose Screen)      │
└────────────────┬────────────────────────────┘
                 │ reads/writes
                 ▼
┌─────────────────────────────────────────────┐
│              TranslateConfig                │
│         (SharedPreferences/DataStore)       │
└────────────────┬────────────────────────────┘
                 │ used by
                 ▼
┌─────────────────────────────────────────────┐
│           TranslationService                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │ Memory   │→ │  Disk    │→ │  API     │  │
│  │ Cache    │  │  Cache   │  │  Call    │  │
│  └──────────┘  └──────────┘  └──────────┘  │
│                                             │
│  ┌──────────────────────────────────────┐   │
│  │        Rate Limiter (3 max)         │   │
│  └──────────────────────────────────────┘   │
└────────────────┬────────────────────────────┘
                 │ called by
    ┌────────────┼────────────────┐
    ▼            ▼                ▼
┌────────┐ ┌──────────┐ ┌──────────────┐
│ Message│ │Notifica- │ │  Outgoing    │
│ List   │ │tion      │ │  Message     │
│Adapter │ │Builder   │ │  Interceptor │
└────────┘ └──────────┘ └──────────────┘
```

---

## 13. Key differences from Desktop

| Aspect | Desktop (Electron) | Android |
|--------|-------------------|---------|
| Message interception | DOM MutationObserver + injected JS | RecyclerView Adapter / Compose state |
| Outgoing interception | `fetch()` monkey-patch | SDK message send interceptor |
| Notification | `window.Notification` override | `NotificationCompat.Builder` hook |
| Cache storage | JSON file in userData | Room DB or JSON in filesDir |
| Settings | Separate BrowserWindow + HTML | PreferenceFragment or Compose |
| Rate limiting | Promise queue | Kotlin Coroutine Semaphore |
| API calls | Electron `net.fetch` | Retrofit/OkHttp |
| Toggle shortcut | Ctrl+Shift+T (keyboard) | Quick Settings tile or FAB |

---

## 14. API compatibility

This system works with any OpenAI-compatible API:

| Provider | API URL | API Key | Notes |
|----------|---------|---------|-------|
| **Ollama** (local) | `http://localhost:11434/v1` | empty | Free, runs on device or local server |
| **OpenAI** | `https://api.openai.com/v1` | `sk-...` | Paid, best quality |
| **LM Studio** | `http://localhost:1234/v1` | empty | Free, local |
| **LocalAI** | `http://localhost:8080/v1` | empty | Free, local |
| **Groq** | `https://api.groq.com/openai/v1` | `gsk-...` | Free tier, very fast |
| **Mistral** | `https://api.mistral.ai/v1` | `...` | Paid |
| **Any OpenAI-compatible** | varies | varies | Any server implementing the chat completions API |

For Android, the user would typically point to a remote server (not localhost) unless they're on the same network as their local LLM.

---

## 15. System prompt (do not modify)

The translation prompt is carefully crafted. Use it exactly:

```
You are a translator. Translate the following message to {language}. Reply ONLY with the translation, no explanations, no quotes, no extra text. If the text is already in {language}, return it as-is. Preserve any formatting, markdown, links, and mentions exactly as they are.
```

This ensures:
- No extra text in the response
- No wrapping in quotes
- Preserves markdown, links, @mentions
- Doesn't re-translate text already in the target language
