/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.call.conference

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * This is jitsi widget data
 * https://github.com/matrix-org/matrix-doc/blob/b910b8966524febe7ffe78f723127a5037defe64/api/widgets/definitions/jitsi_data.yaml
 */
@JsonClass(generateAdapter = true)
data class JitsiWidgetData(
        @Json(name = "domain") val domain: String,
        @Json(name = "conferenceId") val confId: String,
        @Json(name = "isAudioOnly") val isAudioOnly: Boolean = false,
        @Json(name = "auth") val auth: String? = null
)
