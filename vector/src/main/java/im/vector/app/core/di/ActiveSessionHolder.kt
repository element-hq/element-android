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

package im.vector.app.core.di

import arrow.core.Option
import im.vector.app.ActiveSessionDataSource
import im.vector.app.core.services.GuardServiceStarter
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.crypto.keysrequest.KeyRequestHandler
import im.vector.app.features.crypto.verification.IncomingVerificationRequestHandler
import im.vector.app.features.notifications.PushRuleTriggerListener
import im.vector.app.features.session.SessionListener
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveSessionHolder @Inject constructor(private val activeSessionDataSource: ActiveSessionDataSource,
                                              private val keyRequestHandler: KeyRequestHandler,
                                              private val incomingVerificationRequestHandler: IncomingVerificationRequestHandler,
                                              private val callManager: WebRtcCallManager,
                                              private val pushRuleTriggerListener: PushRuleTriggerListener,
                                              private val sessionListener: SessionListener,
                                              private val imageManager: ImageManager,
                                              private val guardServiceStarter: GuardServiceStarter
) {

    private var activeSession: AtomicReference<Session?> = AtomicReference()

    fun setActiveSession(session: Session) {
        Timber.w("setActiveSession of ${session.myUserId}")
        activeSession.set(session)
        activeSessionDataSource.post(Option.just(session))

        keyRequestHandler.start(session)
        incomingVerificationRequestHandler.start(session)
        session.addListener(sessionListener)
        pushRuleTriggerListener.startWithSession(session)
        session.callSignalingService().addCallListener(callManager)
        imageManager.onSessionStarted(session)
        guardServiceStarter.start()
    }

    fun clearActiveSession() {
        // Do some cleanup first
        getSafeActiveSession()?.let {
            Timber.w("clearActiveSession of ${it.myUserId}")
            it.callSignalingService().removeCallListener(callManager)
            it.removeListener(sessionListener)
        }

        activeSession.set(null)
        activeSessionDataSource.post(Option.empty())

        keyRequestHandler.stop()
        incomingVerificationRequestHandler.stop()
        pushRuleTriggerListener.stop()
        guardServiceStarter.stop()
    }

    fun hasActiveSession(): Boolean {
        return activeSession.get() != null
    }

    fun getSafeActiveSession(): Session? {
        return activeSession.get()
    }

    fun getActiveSession(): Session {
        return activeSession.get()
                ?: throw IllegalStateException("You should authenticate before using this")
    }

    // TODO: Stop sync ?
//    fun switchToSession(sessionParams: SessionParams) {
//        val newActiveSession = authenticationService.getSession(sessionParams)
//        activeSession.set(newActiveSession)
//    }
}
