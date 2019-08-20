/*

  * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.room.timeline

import im.vector.matrix.android.internal.database.model.ReadMarkerEntity
import im.vector.matrix.android.internal.database.model.TimelineEventEntity
import im.vector.matrix.android.internal.database.model.TimelineEventEntityFields
import im.vector.matrix.android.internal.database.query.where
import io.realm.Realm
import io.realm.RealmObjectChangeListener
import io.realm.RealmResults

/**
 * This class is responsible for handling the read marker for hidden events.
 * When an hidden event has read marker, we want to transfer it on the first older displayed event.
 * It has to be used in [DefaultTimeline] and we should call the [start] and [dispose] methods to properly handle realm subscription.
 */
internal class TimelineHiddenReadMarker constructor(private val roomId: String) {

    interface Delegate {
        fun rebuildEvent(eventId: String, hasReadMarker: Boolean): Boolean
        fun onReadMarkerUpdated()
    }

    private var previousDisplayedEventId: String? = null
    private var readMarkerEntity: ReadMarkerEntity? = null

    private lateinit var liveEvents: RealmResults<TimelineEventEntity>
    private lateinit var delegate: Delegate

    private val readMarkerListener = RealmObjectChangeListener<ReadMarkerEntity> { readMarker, _ ->
        var hasChange = false
        previousDisplayedEventId?.also {
            hasChange = delegate.rebuildEvent(it, false)
            previousDisplayedEventId = null
        }
        val isEventHidden = liveEvents.where().equalTo(TimelineEventEntityFields.EVENT_ID, readMarker.eventId).findFirst() == null
        if (isEventHidden) {
            val hiddenEvent = readMarker.timelineEvent?.firstOrNull()
                    ?: return@RealmObjectChangeListener
            val displayIndex = hiddenEvent.root?.displayIndex
            if (displayIndex != null) {
                // Then we are looking for the first displayable event after the hidden one
                val firstDisplayedEvent = liveEvents.where()
                        .lessThanOrEqualTo(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, displayIndex)
                        .findFirst()

                // If we find one, we should rebuild this one with marker
                if (firstDisplayedEvent != null) {
                    previousDisplayedEventId = firstDisplayedEvent.eventId
                    hasChange = delegate.rebuildEvent(firstDisplayedEvent.eventId, true)
                }
            }
        }
        if (hasChange) delegate.onReadMarkerUpdated()
    }


    /**
     * Start the realm query subscription. Has to be called on an HandlerThread
     */
    fun start(realm: Realm, liveEvents: RealmResults<TimelineEventEntity>, delegate: Delegate) {
        this.liveEvents = liveEvents
        this.delegate = delegate
        // We are looking for read receipts set on hidden events.
        // We only accept those with a timelineEvent (so coming from pagination/sync).
        readMarkerEntity = ReadMarkerEntity.where(realm, roomId = roomId)
                .findFirstAsync()
                .also { it.addChangeListener(readMarkerListener) }

    }

    /**
     * Dispose the realm query subscription. Has to be called on an HandlerThread
     */
    fun dispose() {
        this.readMarkerEntity?.removeAllChangeListeners()
    }

}