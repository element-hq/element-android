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

package im.vector.app.core.services

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri 
import android.os.Build
import androidx.core.content.getSystemService
import im.vector.app.R
import timber.log.Timber

class CallRingPlayer(
        context: Context
) {

    private val applicationContext = context.applicationContext
    private var r: Ringtone? = null

    fun start() {
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        r = RingtoneManager.getRingtone(applicationContext, notification)
        Timber.v("## VOIP Starting ringing")
        r?.play()
    }

    fun stop() {
        r?.stop()
    }
}
