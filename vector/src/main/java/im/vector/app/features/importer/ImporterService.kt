/*
 * Copyright 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.importer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import androidx.core.os.bundleOf
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.squareup.moshi.Moshi
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.services.VectorAndroidService
import im.vector.app.features.session.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysVersionResult
import org.matrix.android.sdk.api.session.crypto.keysbackup.toKeysVersionResult
import org.matrix.android.sdk.api.session.getUser
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ImporterService : VectorAndroidService() {
    private companion object {
        /**
         * Command to the service to get the data.
         */
        const val MSG_GET_SESSION = 1
        const val MSG_GET_AVATAR = 2

        const val KEY_ERROR_STR = "error"
        const val KEY_USER_ID_STR = "userId"
        const val KEY_HOMESERVER_URL_STR = "homeserverUrl"
        const val KEY_USER_DISPLAY_NAME_STR = "displayName"
        const val KEY_SECRETS_STR = "secrets"
        const val KEY_ROOM_KEYS_VERSION_STR = "roomKeysVersion"
        const val KEY_USER_AVATAR_PARCELABLE = "avatar"
    }

    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    private val signaturePermissionChecker = SignaturePermissionChecker()

    /**
     * Handler of incoming messages from clients.
     */
    private inner class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            Timber.w("ImporterService: handling message ${msg.what}")
            val replyTo = msg.replyTo
            if (replyTo == null) {
                Timber.e("ImporterService: no replyTo in the message, cannot answer")
            } else {
                if (signaturePermissionChecker.check(msg.sendingUid, packageManager)) {
                    Timber.w("ImporterService: Authorized caller")
                    when (msg.what) {
                        MSG_GET_SESSION -> replyTo.sendSession()
                        MSG_GET_AVATAR -> {
                            val userId = msg.data.getString(KEY_USER_ID_STR)
                            replyTo.sendAvatar(userId)
                        }
                        else -> replyTo.sendError(msg.what, "Unknown command ${msg.what}")
                    }
                } else {
                    Timber.w("ImporterService: Unauthorized caller")
                    replyTo.sendError(msg.what, "Unauthorized")
                }
            }
        }
    }

    private fun Messenger.sendSession() {
        val session = activeSessionHolder.getSafeActiveSession()
        val bundle = Bundle()
        if (session == null) {
            // Keep an empty Bundle to indicate that there is no session
            sendResponse(MSG_GET_SESSION, bundle)
        } else {
            bundle.putString(KEY_USER_ID_STR, session.myUserId)
            bundle.putString(KEY_HOMESERVER_URL_STR, session.sessionParams.homeServerUrlBase)
            session.getUser(session.myUserId)?.let { user ->
                bundle.putString(KEY_USER_DISPLAY_NAME_STR, user.displayName)
            }
            val secret = session.cryptoService().exportSecrets()
            secret.fold(
                    onSuccess = { secrets ->
                        Timber.d("ImporterService: the session has the secret, send the userId, displayName and the secrets")
                        bundle.putString(KEY_SECRETS_STR, secrets)
                        session.coroutineScope.launch(Dispatchers.IO) {
                            val roomKeysVersion = runCatching {
                                session.cryptoService().keysBackupService().getCurrentVersion()
                                        ?.toKeysVersionResult()
                                        ?.let {
                                            Moshi.Builder()
                                                    .build()
                                                    .adapter(KeysVersionResult::class.java)
                                                    .toJson(it)
                                        }
                            }
                                    .getOrElse {
                                        Timber.e(it, "ImporterService: Failed to retrieve keys backup version")
                                        null
                                    }
                            // If roomKeysVersion is null, send an empty string so that Element X can ensure that the export is performed with the updated
                            // version of Element Classic.
                            bundle.putString(KEY_ROOM_KEYS_VERSION_STR, roomKeysVersion.orEmpty())
                            sendResponse(MSG_GET_SESSION, bundle)
                        }
                    },
                    onFailure = {
                        Timber.w(it, "ImporterService: Failed to retrieve secrets from session")
                        Timber.d("ImporterService: the session does not have the secret, send the userId and displayName only")
                        sendResponse(MSG_GET_SESSION, bundle)
                    }
            )
        }
    }

    private fun Messenger.sendAvatar(userId: String?) {
        val session = activeSessionHolder.getSafeActiveSession()
        val bundle = Bundle()
        if (session == null) {
            // Keep an empty Bundle to indicate that there is no session
            sendResponse(MSG_GET_AVATAR, bundle)
        } else if (userId != session.myUserId) {
            Timber.w("ImporterService: The userId in the request doesn't match the session userId")
            sendError(MSG_GET_AVATAR, "Invalid userId")
        } else {
            bundle.putString(KEY_USER_ID_STR, userId)
            session.coroutineScope.launch(Dispatchers.IO) {
                session.getUser(session.myUserId)?.let { user ->
                    user.avatarUrl
                            ?.let { avatarUrl ->
                                session.contentUrlResolver().resolveFullSize(avatarUrl)
                            }
                            ?.let { path ->
                                runCatching {
                                    Glide.with(this@ImporterService)
                                            .asBitmap()
                                            .load(path)
                                            .format(DecodeFormat.PREFER_ARGB_8888)
                                            .submit()
                                            .get()
                                }
                                        .onFailure {
                                            Timber.e(it, "ImporterService: Failed to load avatar bitmap")
                                        }
                                        .getOrNull()
                                        ?.let { bitmap ->
                                            bundle.putParcelable(KEY_USER_AVATAR_PARCELABLE, bitmap)
                                        }
                            }
                }
                sendResponse(MSG_GET_AVATAR, bundle)
            }
        }
    }

    private fun Messenger.sendError(what: Int, message: String) {
        val bundle = bundleOf(KEY_ERROR_STR to message)
        sendResponse(what, bundle)
    }

    private fun Messenger.sendResponse(what: Int, bundle: Bundle) {
        Timber.d("ImporterService: send response to client")
        try {
            val message = Message.obtain(null, what).also {
                it.data = bundle
            }
            send(message)
        } catch (e: RemoteException) {
            // The client is dead.
            Timber.e(e, "ImporterService: The client is dead.")
        }
    }

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    override fun onBind(intent: Intent?): IBinder? {
        Timber.w("ImporterService: onBind")
        val messenger = Messenger(IncomingHandler())
        return messenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.w("ImporterService: onUnbind")
        return super.onUnbind(intent)
    }
}
