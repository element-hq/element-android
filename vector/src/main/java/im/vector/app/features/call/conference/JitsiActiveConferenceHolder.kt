/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.conference

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import org.matrix.android.sdk.api.extensions.orFalse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JitsiActiveConferenceHolder @Inject constructor(context: Context) {

    private var activeConference: String? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(ConferenceEventObserver(context, this::onBroadcastEvent))
    }

    fun isJoined(confId: String?): Boolean {
        return confId != null && activeConference?.endsWith(confId).orFalse()
    }

    private fun onBroadcastEvent(conferenceEvent: ConferenceEvent) {
        when (conferenceEvent) {
            is ConferenceEvent.Joined -> activeConference = conferenceEvent.extractConferenceUrl()
            is ConferenceEvent.Terminated -> activeConference = null
            else -> Unit
        }
    }
}
