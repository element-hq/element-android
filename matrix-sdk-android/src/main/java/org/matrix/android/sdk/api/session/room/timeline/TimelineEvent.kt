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

package org.matrix.android.sdk.api.session.room.timeline

import org.matrix.android.sdk.BuildConfig
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.getRelationContent
import org.matrix.android.sdk.api.session.events.model.isEdition
import org.matrix.android.sdk.api.session.events.model.isPoll
import org.matrix.android.sdk.api.session.events.model.isReply
import org.matrix.android.sdk.api.session.events.model.isSticker
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.EventAnnotationsSummary
import org.matrix.android.sdk.api.session.room.model.ReadReceipt
import org.matrix.android.sdk.api.session.room.model.livelocation.LiveLocationBeaconContent
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.model.message.MessageStickerContent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.util.ContentUtils
import org.matrix.android.sdk.api.util.ContentUtils.extractUsefulTextFromReply

/**
 * This data class is a wrapper around an Event. It allows to get useful data in the context of a timeline.
 * This class is used by [TimelineService]
 * Users can also enrich it with metadata.
 */
data class TimelineEvent(
        val root: Event,
        /**
         * Uniquely identify an event, computed locally by the sdk
         */
        val localId: Long,
        val eventId: String,
        /**
         * This display index is the position in the current chunk.
         * It's not unique on the timeline as it's reset on each chunk.
         */
        val displayIndex: Int,
        val ownedByThreadChunk: Boolean = false,
        val senderInfo: SenderInfo,
        val annotations: EventAnnotationsSummary? = null,
        val readReceipts: List<ReadReceipt> = emptyList()
) {

    init {
        if (BuildConfig.DEBUG) {
            assert(eventId == root.eventId)
        }
    }

    val roomId = root.roomId ?: ""

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
 * Tells if the event has been edited
 */
fun TimelineEvent.hasBeenEdited() = annotations?.editSummary != null

/**
 * Get the latest known eventId for an edited event, or the eventId for an Event which has not been edited
 */
fun TimelineEvent.getLatestEventId(): String {
    return annotations
            ?.editSummary
            ?.sourceEvents
            ?.lastOrNull()
            ?: eventId
}

/**
 * Get the relation content if any
 */
fun TimelineEvent.getRelationContent(): RelationDefaultContent? {
    return root.getRelationContent()
}

/**
 * Get the eventId which was edited by this event if any
 */
fun TimelineEvent.getEditedEventId(): String? {
    return getRelationContent()?.takeIf { it.type == RelationType.REPLACE }?.eventId
}

/**
 * Get last MessageContent, after a possible edition
 */
fun TimelineEvent.getLastMessageContent(): MessageContent? {
    return when (root.getClearType()) {
        EventType.STICKER                   -> root.getClearContent().toModel<MessageStickerContent>()
        in EventType.POLL_START             -> (annotations?.editSummary?.latestContent ?: root.getClearContent()).toModel<MessagePollContent>()
        in EventType.STATE_ROOM_BEACON_INFO -> (annotations?.editSummary?.latestContent ?: root.getClearContent()).toModel<LiveLocationBeaconContent>()
        else                                -> (annotations?.editSummary?.latestContent ?: root.getClearContent()).toModel()
    }
}

/**
 * Returns true if it's a reply
 */
fun TimelineEvent.isReply(): Boolean {
    return root.isReply()
}

fun TimelineEvent.isEdition(): Boolean {
    return root.isEdition()
}

fun TimelineEvent.isPoll(): Boolean =
        root.isPoll()

fun TimelineEvent.isSticker(): Boolean {
    return root.isSticker()
}

/**
 * Returns whether or not the event is a root thread event
 */
fun TimelineEvent.isRootThread(): Boolean {
    return root.threadDetails?.isRootThread.orFalse()
}

/**
 * Get the latest message body, after a possible edition, stripping the reply prefix if necessary
 */
fun TimelineEvent.getTextEditableContent(): String {
    val lastContentBody = getLastMessageContent()?.body ?: return ""
    return if (isReply()) {
        extractUsefulTextFromReply(lastContentBody)
    } else {
        lastContentBody
    }
}

/**
 * Get the latest displayable content.
 * Will take care to hide spoiler text
 */
fun MessageContent.getTextDisplayableContent(): String {
    return newContent?.toModel<MessageTextContent>()?.matrixFormattedBody?.let { ContentUtils.formatSpoilerTextFromHtml(it) }
            ?: newContent?.toModel<MessageContent>()?.body
            ?: (this as MessageTextContent?)?.matrixFormattedBody?.let { ContentUtils.formatSpoilerTextFromHtml(it) }
            ?: body
}
