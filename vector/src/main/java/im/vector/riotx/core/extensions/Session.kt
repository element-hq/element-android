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

package im.vector.riotx.core.extensions

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.sync.FilterService
import im.vector.riotx.features.notifications.PushRuleTriggerListener
import timber.log.Timber

fun Session.configureAndStart(pushRuleTriggerListener: PushRuleTriggerListener) {
    open()
    setFilter(FilterService.FilterPreset.RiotFilter)
    Timber.i("Configure and start session for ${this.myUserId}")
    val isAtLeastStarted = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    Timber.v("--> is at least started? $isAtLeastStarted")
    startSync(isAtLeastStarted)
    refreshPushers()
    pushRuleTriggerListener.startWithSession(this)
    fetchPushRules()

    // TODO P1 From HomeActivity
    // @Inject lateinit var incomingVerificationRequestHandler: IncomingVerificationRequestHandler
    // @Inject lateinit var keyRequestHandler: KeyRequestHandler
}