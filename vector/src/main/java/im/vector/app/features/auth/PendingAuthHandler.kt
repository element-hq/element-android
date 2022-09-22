/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.auth

import im.vector.app.core.di.ActiveSessionHolder
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.auth.UIABaseAuth
import org.matrix.android.sdk.api.auth.UserPasswordAuth
import org.matrix.android.sdk.api.session.uia.exceptions.UiaCancelledException
import org.matrix.android.sdk.api.util.fromBase64
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PendingAuthHandler @Inject constructor(
        private val matrix: Matrix,
        private val activeSessionHolder: ActiveSessionHolder,
) {

    var uiaContinuation: Continuation<UIABaseAuth>? = null
    var pendingAuth: UIABaseAuth? = null

    fun ssoAuthDone() {
        Timber.d("ssoAuthDone $pendingAuth , continuation: $uiaContinuation")
        pendingAuth?.let {
            uiaContinuation?.resume(it)
        } ?: run {
            uiaContinuation?.resumeWithException(IllegalArgumentException())
        }
    }

    fun passwordAuthDone(password: String) {
        Timber.d("passwordAuthDone")
        val decryptedPass = matrix.secureStorageService()
                .loadSecureSecret<String>(
                        inputStream = password.fromBase64().inputStream(),
                        keyAlias = ReAuthActivity.DEFAULT_RESULT_KEYSTORE_ALIAS
                )
        uiaContinuation?.resume(
                UserPasswordAuth(
                        session = pendingAuth?.session,
                        password = decryptedPass,
                        user = activeSessionHolder.getActiveSession().myUserId
                )
        )
    }

    fun reAuthCancelled() {
        Timber.d("reAuthCancelled")
        uiaContinuation?.resumeWithException(UiaCancelledException())
        uiaContinuation = null
        pendingAuth = null
    }
}
