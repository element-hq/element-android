/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import im.vector.app.core.extensions.singletonEntryPoint
import im.vector.app.features.call.webrtc.WebRtcCallManager
import timber.log.Timber

class CallHeadsUpActionReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_CALL_ACTION_KEY = "EXTRA_CALL_ACTION_KEY"
        const val EXTRA_CALL_ID = "EXTRA_CALL_ID"
        const val CALL_ACTION_REJECT = 0
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val webRtcCallManager = context.singletonEntryPoint().webRtcCallManager()
        when (intent?.getIntExtra(EXTRA_CALL_ACTION_KEY, 0)) {
            CALL_ACTION_REJECT -> {
                val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: return
                onCallRejectClicked(webRtcCallManager, callId)
            }
        }

        // Not sure why this should be needed
//        val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
//        context.sendBroadcast(it)

        // Close the notification after the click action is performed.
//        context.stopService(Intent(context, CallHeadsUpService::class.java))
    }

    private fun onCallRejectClicked(callManager: WebRtcCallManager, callId: String) {
        Timber.d("onCallRejectClicked")
        callManager.getCallById(callId)?.endCall()
    }

//    private fun onCallAnswerClicked(context: Context) {
//        Timber.d("onCallAnswerClicked")
//        peerConnectionManager.answerCall(context)
//    }
}
