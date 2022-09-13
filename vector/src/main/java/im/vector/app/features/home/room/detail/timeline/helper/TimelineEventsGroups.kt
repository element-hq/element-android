/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.helper

import im.vector.app.core.utils.TextUtils
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.call.CallInviteContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.widgets.model.WidgetContent
import org.threeten.bp.Duration

class TimelineEventsGroup(val groupId: String) {

    val events: Set<TimelineEvent>
        get() = _events

    private val _events = HashSet<TimelineEvent>()

    fun add(timelineEvent: TimelineEvent) {
        _events.add(timelineEvent)
    }
}

class TimelineEventsGroups {

    private val groups = HashMap<String, TimelineEventsGroup>()

    fun addOrIgnore(event: TimelineEvent) {
        val groupId = event.getGroupIdOrNull() ?: return
        groups.getOrPut(groupId) { TimelineEventsGroup(groupId) }.add(event)
    }

    fun getOrNull(event: TimelineEvent): TimelineEventsGroup? {
        val groupId = event.getGroupIdOrNull() ?: return null
        return groups[groupId]
    }

    private fun TimelineEvent.getGroupIdOrNull(): String? {
        val type = root.getClearType()
        val content = root.getClearContent()
        return if (EventType.isCallEvent(type)) {
            (content?.get("call_id") as? String)
        } else if (type == EventType.STATE_ROOM_WIDGET || type == EventType.STATE_ROOM_WIDGET_LEGACY) {
            root.stateKey
        } else {
            null
        }
    }

    fun clear() {
        groups.clear()
    }
}

class JitsiWidgetEventsGroup(private val group: TimelineEventsGroup) {

    val callId: String = group.groupId

    fun isStillActive(): Boolean {
        return group.events.none {
            it.root.getClearContent().toModel<WidgetContent>()?.isActive() == false
        }
    }
}

class CallSignalingEventsGroup(private val group: TimelineEventsGroup) {

    val callId: String = group.groupId

    fun isVideo(): Boolean {
        val invite = getInvite() ?: return false
        return invite.root.getClearContent().toModel<CallInviteContent>()?.isVideo().orFalse()
    }

    fun isRinging(): Boolean {
        return getAnswer() == null && getHangup() == null && getReject() == null
    }

    fun isInCall(): Boolean {
        return getHangup() == null && getReject() == null
    }

    fun formattedDuration(): String {
        val start = getAnswer()?.root?.originServerTs
        val end = getHangup()?.root?.originServerTs
        return if (start == null || end == null) {
            ""
        } else {
            val durationInMillis = (end - start).coerceAtLeast(0L)
            val duration = Duration.ofMillis(durationInMillis)
            TextUtils.formatDuration(duration)
        }
    }

    fun callWasAnswered(): Boolean {
        return getAnswer() != null
    }

    private fun getAnswer(): TimelineEvent? {
        return group.events.firstOrNull { it.root.getClearType() == EventType.CALL_ANSWER }
    }

    private fun getInvite(): TimelineEvent? {
        return group.events.firstOrNull { it.root.getClearType() == EventType.CALL_INVITE }
    }

    private fun getHangup(): TimelineEvent? {
        return group.events.firstOrNull { it.root.getClearType() == EventType.CALL_HANGUP }
    }

    private fun getReject(): TimelineEvent? {
        return group.events.firstOrNull { it.root.getClearType() == EventType.CALL_REJECT }
    }
}
