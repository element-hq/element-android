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

package im.vector.matrix.android.api.session.room.timeline

import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.EventAnnotationsSummary
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.isReply
import im.vector.matrix.android.api.util.ContentUtils.extractUsefulTextFromReply
import im.vector.matrix.android.internal.crypto.model.event.EncryptedEventContent

/**
 * This data class is a wrapper around an Event. It allows to get useful data in the context of a timeline.
 * This class is used by [TimelineService]
 * Users can also enrich it with metadata.
 */
data class TimelineEvent(
        val root: Event,
        val localId: Long,
        val displayIndex: Int,
        val senderName: String?,
        val isUniqueDisplayName: Boolean,
        val senderAvatar: String?,
        val annotations: EventAnnotationsSummary? = null
) {

    val metadata = HashMap<String, Any>()

    /**
     * The method to enrich this timeline event.
     * If you provides multiple data with the same key, only first one will be kept.
     * @param key the key to associate data with.
     * @param data the data to enrich with.
     */
    fun enrichWith(key: String?, data: Any?) {
        if (key == null || data == null) {
            return
        }
        if (!metadata.containsKey(key)) {
            metadata[key] = data
        }
    }

    fun getDisambiguatedDisplayName(): String {
        return if (isUniqueDisplayName) {
            senderName
        } else {
            senderName?.let { name ->
                "$name (${root.senderId})"
            }
        }
                ?: root.senderId
                ?: ""
    }

    /**
     * Get the metadata associated with a key.
     * @param key the key to get the metadata
     * @return the metadata
     */
    inline fun <reified T> getMetadata(key: String): T? {
        return metadata[key] as T?
    }

    fun isEncrypted(): Boolean {
        // warning: Do not use getClearType here
        return EventType.ENCRYPTED == root.type
    }
}

/**
 * Get last MessageContent, after a possible edition
 */
fun TimelineEvent.getLastMessageContent(): MessageContent? = annotations?.editSummary?.aggregatedContent?.toModel()
        ?: root.getClearContent().toModel()


fun TimelineEvent.getTextEditableContent(): String? {
    val originalContent = root.getClearContent().toModel<MessageContent>() ?: return null
    val isReply = originalContent.isReply() || root.content.toModel<EncryptedEventContent>()?.relatesTo?.inReplyTo?.eventId != null
    val lastContent = getLastMessageContent()
    return if (isReply) {
        return extractUsefulTextFromReply(lastContent?.body ?: "")
    } else {
        lastContent?.body ?: ""
    }
}