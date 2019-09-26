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

import im.vector.matrix.android.api.session.room.timeline.TimelineSettings
import im.vector.matrix.android.internal.database.model.ReadMarkerEntity
import im.vector.matrix.android.internal.database.model.ReadMarkerEntityFields
import im.vector.matrix.android.internal.database.model.TimelineEventEntity
import im.vector.matrix.android.internal.database.model.TimelineEventEntityFields
import im.vector.matrix.android.internal.database.query.FilterContent
import im.vector.matrix.android.internal.database.query.where
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.RealmResults

/**
 * This class is responsible for handling the read marker for hidden events.
 * When an hidden event has read marker, we want to transfer it on the first older displayed event.
 * It has to be used in [DefaultTimeline] and we should call the [start] and [dispose] methods to properly handle realm subscription.
 */
internal class TimelineHiddenReadMarker constructor(private val roomId: String,
                                                    private val settings: TimelineSettings) {

    interface Delegate {
        fun rebuildEvent(eventId: String, hasReadMarker: Boolean): Boolean
        fun onReadMarkerUpdated()
    }

    private var previousDisplayedEventId: String? = null
    private var hiddenReadMarker: RealmResults<ReadMarkerEntity>? = null

    private lateinit var filteredEvents: RealmResults<TimelineEventEntity>
    private lateinit var nonFilteredEvents: RealmResults<TimelineEventEntity>
    private lateinit var delegate: Delegate

    private val readMarkerListener = OrderedRealmCollectionChangeListener<RealmResults<ReadMarkerEntity>> { readMarkers, changeSet ->
        if (!readMarkers.isLoaded || !readMarkers.isValid) {
            return@OrderedRealmCollectionChangeListener
        }
        var hasChange = false
        if (changeSet.deletions.isNotEmpty()) {
            previousDisplayedEventId?.also {
                hasChange = delegate.rebuildEvent(it, false)
                previousDisplayedEventId = null
            }
        }
        val readMarker = readMarkers.firstOrNull() ?: return@OrderedRealmCollectionChangeListener
        val hiddenEvent = readMarker.timelineEvent?.firstOrNull()
                ?: return@OrderedRealmCollectionChangeListener

        val isLoaded = nonFilteredEvents.where().equalTo(TimelineEventEntityFields.EVENT_ID, hiddenEvent.eventId).findFirst() != null
        val displayIndex = hiddenEvent.root?.displayIndex
        if (isLoaded && displayIndex != null) {
            // Then we are looking for the first displayable event after the hidden one
            val firstDisplayedEvent = filteredEvents.where()
                    .lessThanOrEqualTo(TimelineEventEntityFields.ROOT.DISPLAY_INDEX, displayIndex)
                    .findFirst()

            // If we find one, we should rebuild this one with marker
            if (firstDisplayedEvent != null) {
                previousDisplayedEventId = firstDisplayedEvent.eventId
                hasChange = delegate.rebuildEvent(firstDisplayedEvent.eventId, true)
            }
        }
        if (hasChange) {
            delegate.onReadMarkerUpdated()
        }
    }


    /**
     * Start the realm query subscription. Has to be called on an HandlerThread
     */
    fun start(realm: Realm,
              filteredEvents: RealmResults<TimelineEventEntity>,
              nonFilteredEvents: RealmResults<TimelineEventEntity>,
              delegate: Delegate) {
        this.filteredEvents = filteredEvents
        this.nonFilteredEvents = nonFilteredEvents
        this.delegate = delegate
        // We are looking for read receipts set on hidden events.
        // We only accept those with a timelineEvent (so coming from pagination/sync).
        hiddenReadMarker = ReadMarkerEntity.where(realm, roomId = roomId)
                .isNotEmpty(ReadMarkerEntityFields.TIMELINE_EVENT)
                .filterReceiptsWithSettings()
                .findAllAsync()
                .also { it.addChangeListener(readMarkerListener) }

    }

    /**
     * Dispose the realm query subscription. Has to be called on an HandlerThread
     */
    fun dispose() {
        this.hiddenReadMarker?.removeAllChangeListeners()
    }

    /**
     * We are looking for readMarker related to filtered events. So, it's the opposite of [DefaultTimeline.filterEventsWithSettings] method.
     */
    private fun RealmQuery<ReadMarkerEntity>.filterReceiptsWithSettings(): RealmQuery<ReadMarkerEntity> {
        beginGroup()
        if (settings.filterTypes) {
            not().`in`("${ReadMarkerEntityFields.TIMELINE_EVENT}.${TimelineEventEntityFields.ROOT.TYPE}", settings.allowedTypes.toTypedArray())
        }
        if (settings.filterTypes && settings.filterEdits) {
            or()
        }
        if (settings.filterEdits) {
            like("${ReadMarkerEntityFields.TIMELINE_EVENT}.${TimelineEventEntityFields.ROOT.CONTENT}", FilterContent.EDIT_TYPE)
        }
        endGroup()
        return this
    }


}