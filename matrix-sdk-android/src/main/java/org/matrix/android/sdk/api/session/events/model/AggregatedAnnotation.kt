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
package org.matrix.android.sdk.api.session.events.model

import com.squareup.moshi.JsonClass

/**
 * <code>
 *     {
 *       "chunk": [
 *            {
 *                "type": "m.reaction",
 *                "key": "👍",
 *                "count": 3
 *            }
 *           ],
 *           "limited": false,
 *           "count": 1
 *     },
 * </code>
 */

@JsonClass(generateAdapter = true)
data class AggregatedAnnotation(
        override val limited: Boolean? = false,
        override val count: Int? = 0,
        val chunk: List<RelationChunkInfo>? = null

) : UnsignedRelationInfo
