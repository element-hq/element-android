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

package im.vector.riotx.features.login

import im.vector.matrix.android.internal.crypto.model.rest.UserPasswordAuth
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import javax.inject.Singleton

const val THREE_MINUTES = 3 * 60_000L

@Singleton
class ReAuthHelper @Inject constructor() {

    private var timer: Timer? = null

    private var rememberedInfo: UserPasswordAuth? = null

    fun rememberAuth(password: UserPasswordAuth?) {
        timer?.cancel()
        timer = null
        rememberedInfo = password
        timer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    rememberedInfo = null
                }
            }, THREE_MINUTES)
        }
    }

    fun rememberedAuth() = rememberedInfo
}
