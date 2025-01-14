/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
        pendingAuth?.let {
            Timber.d("ssoAuthDone, resuming action")
            uiaContinuation?.resume(it)
        } ?: run {
            Timber.d("ssoAuthDone, cannot resume: no pendingAuth")
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
