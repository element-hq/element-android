/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.riotx.features.call.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import im.vector.riotx.core.di.HasVectorInjector
import im.vector.riotx.features.call.WebRtcPeerConnectionManager
import im.vector.riotx.features.settings.VectorLocale.context
import timber.log.Timber

class CallHeadsUpActionReceiver : BroadcastReceiver() {

    private lateinit var peerConnectionManager: WebRtcPeerConnectionManager

    init {
        val appContext = context.applicationContext
        if (appContext is HasVectorInjector) {
            peerConnectionManager = appContext.injector().webRtcPeerConnectionManager()
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.getIntExtra(CallHeadsUpService.EXTRA_CALL_ACTION_KEY, 0)) {
            CallHeadsUpService.CALL_ACTION_ANSWER -> onCallAnswerClicked()
            CallHeadsUpService.CALL_ACTION_REJECT -> onCallRejectClicked()
        }
    }

    private fun onCallRejectClicked() {
        Timber.d("onCallRejectClicked")
        peerConnectionManager.endCall()
    }

    private fun onCallAnswerClicked() {
        Timber.d("onCallAnswerClicked")
        peerConnectionManager.answerCall()
    }
}
