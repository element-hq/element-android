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

package im.vector.app.features.call.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import im.vector.app.core.di.HasVectorInjector
import im.vector.app.features.call.WebRtcPeerConnectionManager
import timber.log.Timber

class CallHeadsUpActionReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_CALL_ACTION_KEY = "EXTRA_CALL_ACTION_KEY"
        const val CALL_ACTION_REJECT = 0
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val peerConnectionManager = (context.applicationContext as? HasVectorInjector)
                ?.injector()
                ?.webRtcPeerConnectionManager()
                ?: return

        when (intent?.getIntExtra(EXTRA_CALL_ACTION_KEY, 0)) {
            CALL_ACTION_REJECT -> onCallRejectClicked(peerConnectionManager)
        }

        // Not sure why this should be needed
//        val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
//        context.sendBroadcast(it)

        // Close the notification after the click action is performed.
//        context.stopService(Intent(context, CallHeadsUpService::class.java))
    }

    private fun onCallRejectClicked(peerConnectionManager: WebRtcPeerConnectionManager) {
        Timber.d("onCallRejectClicked")
        peerConnectionManager.endCall()
    }

//    private fun onCallAnswerClicked(context: Context) {
//        Timber.d("onCallAnswerClicked")
//        peerConnectionManager.answerCall(context)
//    }
}
