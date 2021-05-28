/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.permalinks

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.permalinks.PermalinkService
import org.matrix.android.sdk.api.session.permalinks.PermalinkService.Companion.MATRIX_TO_URL_BASE
import javax.inject.Inject

internal class DefaultPermalinkService @Inject constructor(
        private val permalinkFactory: PermalinkFactory
) : PermalinkService {

    override fun createPermalink(event: Event): String? {
        return permalinkFactory.createPermalink(event)
    }

    override fun createPermalink(id: String): String? {
        return permalinkFactory.createPermalink(id)
    }

    override fun createRoomPermalink(roomId: String, viaServers: List<String>?): String? {
        return permalinkFactory.createRoomPermalink(roomId, viaServers)
    }

    override fun createPermalink(roomId: String, eventId: String): String {
        return permalinkFactory.createPermalink(roomId, eventId)
    }

    override fun getLinkedId(url: String): String? {
        return url
                .takeIf { it.startsWith(MATRIX_TO_URL_BASE) }
                ?.substring(MATRIX_TO_URL_BASE.length)
                ?.substringBeforeLast("?")
    }
}
