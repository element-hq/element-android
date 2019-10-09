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

package im.vector.riotx.features.notifications

import im.vector.matrix.android.api.pushrules.Action
import im.vector.matrix.android.api.pushrules.PushRuleService
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.Event
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class PushRuleTriggerListener @Inject constructor(
        private val resolver: NotifiableEventResolver,
        private val notificationDrawerManager: NotificationDrawerManager
) : PushRuleService.PushRuleListener {


    var session: Session? = null

    override fun onMatchRule(event: Event, actions: List<Action>) {
        Timber.v("Push rule match for event ${event.eventId}")
        if (session == null) {
            Timber.e("Called without active session")
            return
        }
        val notificationAction = actions.toNotificationAction()
        if (notificationAction.shouldNotify) {
            val notifiableEvent = resolver.resolveEvent(event, session!!)
            if (notifiableEvent == null) {
                Timber.v("## Failed to resolve event")
                //TODO
            } else {
                notifiableEvent.noisy = !notificationAction.soundName.isNullOrBlank()
                Timber.v("New event to notify")
                notificationDrawerManager.onNotifiableEventReceived(notifiableEvent)
            }
        } else {
            Timber.v("Matched push rule is set to not notify")
        }
    }

    override fun onRoomLeft(roomId: String) {
        notificationDrawerManager.clearMessageEventOfRoom(roomId)
        notificationDrawerManager.clearMemberShipNotificationForRoom(roomId)
    }

    override fun onRoomJoined(roomId: String) {
        notificationDrawerManager.clearMemberShipNotificationForRoom(roomId)
    }

    override fun onEventRedacted(redactedEventId: String) {
        notificationDrawerManager.onEventRedacted(redactedEventId)
    }

    override fun batchFinish() {
        notificationDrawerManager.refreshNotificationDrawer()
    }

    fun startWithSession(session: Session) {
        if (this.session != null) {
            stop()
        }
        this.session = session
        session.addPushRuleListener(this)
    }

    fun stop() {
        session?.removePushRuleListener(this)
        session = null
        notificationDrawerManager.clearAllEvents()
    }

}