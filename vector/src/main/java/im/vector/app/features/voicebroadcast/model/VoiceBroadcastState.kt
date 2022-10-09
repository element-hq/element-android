/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package im.vector.app.features.voicebroadcast.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Ref: https://github.com/vector-im/element-meta/discussions/632
 */
@JsonClass(generateAdapter = false)
enum class VoiceBroadcastState(val value: String) {
    /**
     * The voice broadcast had been started and is currently being live.
     */
    @Json(name = "started") STARTED("started"),

    /**
     * The voice broadcast has been paused and may be resumed at any time by the recorder.
     */
    @Json(name = "paused") PAUSED("paused"),

    /**
     * The voice broadcast is currently being live again.
     */
    @Json(name = "resumed") RESUMED("resumed"),

    /**
     * The voice broadcast has ended.
     */
    @Json(name = "stopped") STOPPED("stopped"),
}
