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
        if (session == null) {
            Timber.e("Called without active session")
            return
        }
        resolver.resolveEvent(event,null,session!!)?.let {
            drawerManager.onNotifiableEventReceived(it)
        }
    }

    override fun batchFinish() {
        drawerManager.refreshNotificationDrawer(null)
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
        drawerManager.refreshNotificationDrawer(null)
    }
}