/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.space.model

import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.MatrixPatterns

/**
 * {
 * "type": "m.space_order",
 *   "content": {
 *       "order": "..."
 *   }
 * }
 */
@JsonClass(generateAdapter = true)
data class SpaceOrderContent(
        val order: String? = null
) {
    fun safeOrder(): String? {
        return order?.takeIf { MatrixPatterns.isValidOrderString(it) }
    }
}
