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

package im.vector.riotx.features.call

import android.content.Context
import android.os.Build
import android.telecom.Connection
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.M) class CallConnection(
        private val context: Context,
        private val roomId: String,
        private val callId: String
) : Connection() {

    /**
     * The telecom subsystem calls this method when you add a new incoming call and your app should show its incoming call UI.
     */
    override fun onShowIncomingCallUi() {
        VectorCallActivity.newIntent(context, roomId).let {
            context.startActivity(it)
        }
    }

    override fun onAnswer() {
        super.onAnswer()
    }
}
