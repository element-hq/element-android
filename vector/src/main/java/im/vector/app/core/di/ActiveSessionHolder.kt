/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.di

import android.content.Context
import arrow.core.Option
import im.vector.app.ActiveSessionDataSource
import im.vector.app.core.extensions.configureAndStart
import im.vector.app.core.extensions.startSyncing
import im.vector.app.core.extensions.stopSyncing
import im.vector.app.core.pushers.UnifiedPushHelper
import im.vector.app.core.services.GuardServiceStarter
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.crypto.keysrequest.KeyRequestHandler
import im.vector.app.features.crypto.verification.IncomingVerificationRequestHandler
import im.vector.app.features.notifications.PushRuleTriggerListener
import im.vector.app.features.session.SessionListener
import kotlinx.coroutines.runBlocking
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveSessionHolder @Inject constructor(
        private val activeSessionDataSource: ActiveSessionDataSource,
        private val keyRequestHandler: KeyRequestHandler,
        private val incomingVerificationRequestHandler: IncomingVerificationRequestHandler,
        private val callManager: WebRtcCallManager,
        private val pushRuleTriggerListener: PushRuleTriggerListener,
        private val sessionListener: SessionListener,
        private val imageManager: ImageManager,
        private val unifiedPushHelper: UnifiedPushHelper,
        private val guardServiceStarter: GuardServiceStarter,
        private val sessionInitializer: SessionInitializer,
        private val applicationContext: Context,
        private val authenticationService: AuthenticationService,
) {

    private var activeSessionReference: AtomicReference<Session?> = AtomicReference()

    fun setActiveSession(session: Session) {
        Timber.w("setActiveSession of ${session.myUserId}")
        activeSessionReference.set(session)
        activeSessionDataSource.post(Option.just(session))

        keyRequestHandler.start(session)
        incomingVerificationRequestHandler.start(session)
        session.addListener(sessionListener)
        pushRuleTriggerListener.startWithSession(session)
        session.callSignalingService().addCallListener(callManager)
        imageManager.onSessionStarted(session)
        guardServiceStarter.start()
    }

    suspend fun clearActiveSession() {
        // Do some cleanup first
        getSafeActiveSession(startSync = false)?.let {
            Timber.w("clearActiveSession of ${it.myUserId}")
            it.callSignalingService().removeCallListener(callManager)
            it.removeListener(sessionListener)
        }

        activeSessionReference.set(null)
        activeSessionDataSource.post(Option.empty())

        keyRequestHandler.stop()
        incomingVerificationRequestHandler.stop()
        pushRuleTriggerListener.stop()
        // No need to unregister the pusher, the sign out will (should?) do it server side.
        unifiedPushHelper.unregister(pushersManager = null)
        guardServiceStarter.stop()
    }

    fun hasActiveSession(): Boolean {
        return activeSessionReference.get() != null || authenticationService.hasAuthenticatedSessions()
    }

    fun getSafeActiveSession(startSync: Boolean = true): Session? {
        return runBlocking { getOrInitializeSession(startSync = startSync) }
    }

    fun getActiveSession(): Session {
        return getSafeActiveSession()
                ?: throw IllegalStateException("You should authenticate before using this")
    }

    suspend fun getOrInitializeSession(startSync: Boolean): Session? {
        return activeSessionReference.get()
                ?.also {
                    if (startSync && !it.syncService().isSyncThreadAlive()) {
                        it.startSyncing(applicationContext)
                    }
                }
                ?: sessionInitializer.tryInitialize(readCurrentSession = { activeSessionReference.get() }) { session ->
                    setActiveSession(session)
                    session.configureAndStart(applicationContext, startSyncing = startSync)
                }
    }

    fun isWaitingForSessionInitialization() = activeSessionReference.get() == null && authenticationService.hasAuthenticatedSessions()

    // TODO Stop sync ?
    fun stopSync() {
        Timber.i("Sync: Stopping sync")
        activeSessionReference.get()
                ?.also {
                    it.stopSyncing()
                }
    }

//    fun switchToSession(sessionParams: SessionParams) {
//        val newActiveSession = authenticationService.getSession(sessionParams)
//        activeSession.set(newActiveSession)
//    }
}
