package im.vector.riotredesign.features.notifications

import im.vector.matrix.android.api.pushrules.Action
import im.vector.matrix.android.api.pushrules.PushRuleService
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.Event
import timber.log.Timber


class PushRuleTriggerListener(
        private val resolver: NotifiableEventResolver,
        private val drawerManager: NotificationDrawerManager
) : PushRuleService.PushRuleListener {


    var session: Session? = null

    override fun onMatchRule(event: Event, actions: List<Action>) {
        Timber.v("Push rule match for event ${event.eventId}")
        if (session == null) {
            Timber.e("Called without active session")
            return
        }
        val notificationAction = NotificationAction.extractFrom(actions)
        if (notificationAction.shouldNotify) {
            val resolveEvent = resolver.resolveEvent(event, session!!)
            if (resolveEvent == null) {
                Timber.v("## Failed to resolve event")
                //TODO
            } else {
                resolveEvent.noisy = !notificationAction.soundName.isNullOrBlank()
                Timber.v("New event to notify $resolveEvent tweaks:$notificationAction")
                drawerManager.onNotifiableEventReceived(resolveEvent)
            }
        } else {
            Timber.v("Matched push rule is set to not notify")
        }
    }

    override fun batchFinish() {
        drawerManager.refreshNotificationDrawer()
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
        drawerManager.clearAllEvents()
        drawerManager.refreshNotificationDrawer()
    }

}