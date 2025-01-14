/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.error

import android.content.ActivityNotFoundException
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.call.dialpad.DialPadLookup
import im.vector.app.features.roomprofile.polls.RoomPollsLoadingError
import im.vector.app.features.voice.VoiceFailure
import im.vector.app.features.voicebroadcast.VoiceBroadcastFailure
import im.vector.app.features.voicebroadcast.VoiceBroadcastFailure.RecordingError
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.failure.MatrixIdFailure
import org.matrix.android.sdk.api.failure.isInvalidPassword
import org.matrix.android.sdk.api.failure.isLimitExceededError
import org.matrix.android.sdk.api.failure.isMissingEmailVerification
import org.matrix.android.sdk.api.session.identity.IdentityServiceError
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.net.ssl.SSLException
import javax.net.ssl.SSLPeerUnverifiedException

interface ErrorFormatter {
    fun toHumanReadable(throwable: Throwable?): String
}

class DefaultErrorFormatter @Inject constructor(
        private val stringProvider: StringProvider
) : ErrorFormatter {

    override fun toHumanReadable(throwable: Throwable?): String {
        return when (throwable) {
            null -> null
            is IdentityServiceError -> identityServerError(throwable)
            is Failure.NetworkConnection -> {
                when (throwable.ioException) {
                    is SocketTimeoutException ->
                        stringProvider.getString(CommonStrings.error_network_timeout)
                    is SSLPeerUnverifiedException ->
                        stringProvider.getString(CommonStrings.login_error_ssl_peer_unverified)
                    is SSLException ->
                        stringProvider.getString(CommonStrings.login_error_ssl_other)
                    else ->
                        // TODO Check network state, airplane mode, etc.
                        stringProvider.getString(CommonStrings.error_no_network)
                }
            }
            is Failure.ServerError -> {
                when {
                    throwable.error.code == MatrixError.M_CONSENT_NOT_GIVEN -> {
                        // Special case for terms and conditions
                        stringProvider.getString(CommonStrings.error_terms_not_accepted)
                    }
                    throwable.isInvalidPassword() -> {
                        stringProvider.getString(CommonStrings.auth_invalid_login_param)
                    }
                    throwable.error.code == MatrixError.M_USER_IN_USE -> {
                        stringProvider.getString(CommonStrings.login_signup_error_user_in_use)
                    }
                    throwable.error.code == MatrixError.M_BAD_JSON -> {
                        stringProvider.getString(CommonStrings.login_error_bad_json)
                    }
                    throwable.error.code == MatrixError.M_NOT_JSON -> {
                        stringProvider.getString(CommonStrings.login_error_not_json)
                    }
                    throwable.error.code == MatrixError.M_THREEPID_DENIED -> {
                        stringProvider.getString(CommonStrings.login_error_threepid_denied)
                    }
                    throwable.isLimitExceededError() -> {
                        limitExceededError(throwable.error)
                    }
                    throwable.error.code == MatrixError.M_TOO_LARGE -> {
                        stringProvider.getString(CommonStrings.error_file_too_big_simple)
                    }
                    throwable.error.code == MatrixError.M_THREEPID_NOT_FOUND -> {
                        stringProvider.getString(CommonStrings.login_reset_password_error_not_found)
                    }
                    throwable.error.code == MatrixError.M_USER_DEACTIVATED -> {
                        stringProvider.getString(CommonStrings.auth_invalid_login_deactivated_account)
                    }
                    throwable.error.code == MatrixError.M_THREEPID_IN_USE &&
                            throwable.error.message == "Email is already in use" -> {
                        stringProvider.getString(CommonStrings.account_email_already_used_error)
                    }
                    throwable.error.code == MatrixError.M_THREEPID_IN_USE &&
                            throwable.error.message == "MSISDN is already in use" -> {
                        stringProvider.getString(CommonStrings.account_phone_number_already_used_error)
                    }
                    throwable.error.code == MatrixError.M_THREEPID_AUTH_FAILED -> {
                        stringProvider.getString(CommonStrings.error_threepid_auth_failed)
                    }
                    throwable.error.code == MatrixError.M_UNKNOWN &&
                            throwable.error.message == "Not allowed to join this room" -> {
                        stringProvider.getString(CommonStrings.room_error_access_unauthorized)
                    }
                    throwable.isMissingEmailVerification() -> {
                        stringProvider.getString(CommonStrings.auth_reset_password_error_unverified)
                    }
                    else -> {
                        throwable.error.message.takeIf { it.isNotEmpty() }
                                ?: throwable.error.code.takeIf { it.isNotEmpty() }
                    }
                }
            }
            is Failure.OtherServerError -> {
                when (throwable.httpCode) {
                    HttpURLConnection.HTTP_NOT_FOUND ->
                        // homeserver not found
                        stringProvider.getString(CommonStrings.login_error_no_homeserver_found)
                    HttpURLConnection.HTTP_UNAUTHORIZED ->
                        // uia errors?
                        stringProvider.getString(CommonStrings.error_unauthorized)
                    else ->
                        throwable.localizedMessage
                }
            }
            is DialPadLookup.Failure.NumberIsYours ->
                stringProvider.getString(CommonStrings.cannot_call_yourself)
            is DialPadLookup.Failure.NoResult ->
                stringProvider.getString(CommonStrings.call_dial_pad_lookup_error)
            is MatrixIdFailure.InvalidMatrixId ->
                stringProvider.getString(CommonStrings.login_signin_matrix_id_error_invalid_matrix_id)
            is VoiceFailure -> voiceMessageError(throwable)
            is VoiceBroadcastFailure -> voiceBroadcastMessageError(throwable)
            is RoomPollsLoadingError -> stringProvider.getString(CommonStrings.room_polls_loading_error)
            is ActivityNotFoundException ->
                stringProvider.getString(CommonStrings.error_no_external_application_found)
            else -> throwable.localizedMessage
        }
                ?: stringProvider.getString(CommonStrings.unknown_error)
    }

    private fun voiceMessageError(throwable: VoiceFailure): String {
        return when (throwable) {
            is VoiceFailure.UnableToPlay -> stringProvider.getString(CommonStrings.error_voice_message_unable_to_play)
            is VoiceFailure.UnableToRecord -> stringProvider.getString(CommonStrings.error_voice_message_unable_to_record)
            is VoiceFailure.VoiceBroadcastInProgress -> stringProvider.getString(CommonStrings.error_voice_message_broadcast_in_progress)
        }
    }

    private fun voiceBroadcastMessageError(throwable: VoiceBroadcastFailure): String {
        return when (throwable) {
            RecordingError.BlockedBySomeoneElse -> stringProvider.getString(CommonStrings.error_voice_broadcast_blocked_by_someone_else_message)
            RecordingError.NoPermission -> stringProvider.getString(CommonStrings.error_voice_broadcast_permission_denied_message)
            RecordingError.UserAlreadyBroadcasting -> stringProvider.getString(CommonStrings.error_voice_broadcast_already_in_progress_message)
            is VoiceBroadcastFailure.ListeningError.UnableToPlay,
            is VoiceBroadcastFailure.ListeningError.PrepareMediaPlayerError -> stringProvider.getString(CommonStrings.error_voice_broadcast_unable_to_play)
            is VoiceBroadcastFailure.ListeningError.UnableToDecrypt ->  stringProvider.getString(CommonStrings.error_voice_broadcast_unable_to_decrypt)
        }
    }

    private fun limitExceededError(error: MatrixError): String {
        val delay = error.retryAfterMillis

        return if (delay == null) {
            stringProvider.getString(CommonStrings.login_error_limit_exceeded)
        } else {
            // Ensure at least 1 second
            val delaySeconds = delay.toInt() / 1000 + 1
            stringProvider.getQuantityString(CommonPlurals.login_error_limit_exceeded_retry_after, delaySeconds, delaySeconds)
        }
    }

    private fun identityServerError(identityServiceError: IdentityServiceError): String {
        return stringProvider.getString(
                when (identityServiceError) {
                    IdentityServiceError.OutdatedIdentityServer -> CommonStrings.identity_server_error_outdated_identity_server
                    IdentityServiceError.OutdatedHomeServer -> CommonStrings.identity_server_error_outdated_home_server
                    IdentityServiceError.NoIdentityServerConfigured -> CommonStrings.identity_server_error_no_identity_server_configured
                    IdentityServiceError.TermsNotSignedException -> CommonStrings.identity_server_error_terms_not_signed
                    IdentityServiceError.BulkLookupSha256NotSupported -> CommonStrings.identity_server_error_bulk_sha256_not_supported
                    IdentityServiceError.BindingError -> CommonStrings.identity_server_error_binding_error
                    IdentityServiceError.NoCurrentBindingError -> CommonStrings.identity_server_error_no_current_binding_error
                    IdentityServiceError.UserConsentNotProvided -> CommonStrings.identity_server_user_consent_not_provided
                }
        )
    }
}
