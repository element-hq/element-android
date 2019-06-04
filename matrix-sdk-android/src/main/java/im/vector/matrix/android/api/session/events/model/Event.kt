/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.api.session.events.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import im.vector.matrix.android.internal.di.MoshiProvider
import timber.log.Timber
import java.lang.reflect.ParameterizedType

typealias Content = Map<String, @JvmSuppressWildcards Any>

/**
 * This methods is a facility method to map a json content to a model.
 */
inline fun <reified T> Content?.toModel(catchError: Boolean = true): T? {
    return this?.let {
        val moshi = MoshiProvider.providesMoshi()
        val moshiAdapter = moshi.adapter(T::class.java)
        return try {
            moshiAdapter.fromJsonValue(it)
        } catch (e: Exception) {
            if (catchError) {
                Timber.e(e, "To model failed : $e")
                null
            } else {
                throw e
            }
        }
    }
}

/**
 * This methods is a facility method to map a model to a json Content
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> T?.toContent(): Content? {
    return this?.let {
        val moshi = MoshiProvider.providesMoshi()
        val moshiAdapter = moshi.adapter(T::class.java)
        return moshiAdapter.toJsonValue(it) as Content
    }
}

/**
 * Generic event class with all possible fields for events.
 * The content and prevContent json fields can easily be mapped to a model with [toModel] method.
 */
@JsonClass(generateAdapter = true)
data class Event(
        @Json(name = "type") val type: String,
        @Json(name = "event_id") val eventId: String? = null,
        @Json(name = "content") val content: Content? = null,
        @Json(name = "prev_content") val prevContent: Content? = null,
        @Json(name = "origin_server_ts") val originServerTs: Long? = null,
        @Json(name = "sender") val sender: String? = null,
        @Json(name = "state_key") val stateKey: String? = null,
        @Json(name = "room_id") val roomId: String? = null,
        @Json(name = "unsigned") val unsignedData: UnsignedData? = null,
        @Json(name = "redacts") val redacts: String? = null

) {

    /**
     * Check if event is a state event.
     * @return true if event is state event.
     */
    fun isStateEvent(): Boolean {
        return EventType.isStateEvent(type)
    }

    companion object {
        internal val CONTENT_TYPE: ParameterizedType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    }
}