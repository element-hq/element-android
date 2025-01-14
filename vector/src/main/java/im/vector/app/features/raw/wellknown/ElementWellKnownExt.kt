/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.raw.wellknown

import im.vector.app.features.crypto.keysrequest.OutboundSessionKeySharingStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixPatterns.getServerName
import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.raw.RawService

suspend fun RawService.getElementWellknown(sessionParams: SessionParams): ElementWellKnown? {
    // By default we use the domain of the userId to retrieve the .well-known data
    val domain = sessionParams.userId.getServerName()
    return tryOrNull { getWellknown(domain) }
            ?.let { ElementWellKnownMapper.from(it) }
}

fun ElementWellKnown.isE2EByDefault() = elementE2E?.e2eDefault ?: riotE2E?.e2eDefault ?: true

fun ElementWellKnown?.getOutboundSessionKeySharingStrategyOrDefault(fallback: OutboundSessionKeySharingStrategy): OutboundSessionKeySharingStrategy {
    return when (this?.elementE2E?.outboundsKeyPreSharingMode) {
        "on_room_opening" -> OutboundSessionKeySharingStrategy.WhenEnteringRoom
        "on_typing" -> OutboundSessionKeySharingStrategy.WhenTyping
        "disabled" -> OutboundSessionKeySharingStrategy.WhenSendingEvent
        else -> fallback
    }
}

fun RawService.withElementWellKnown(
        coroutineScope: CoroutineScope,
        sessionParams: SessionParams,
        block: ((ElementWellKnown?) -> Unit)
) = with(coroutineScope) {
    launch(Dispatchers.IO) {
        block(getElementWellknown(sessionParams))
    }
}

fun ElementWellKnown.isSecureBackupRequired() = elementE2E?.secureBackupRequired
        ?: riotE2E?.secureBackupRequired
        ?: false

fun ElementWellKnown?.secureBackupMethod(): SecureBackupMethod {
    val methodList = this?.elementE2E?.secureBackupSetupMethods
            ?: this?.riotE2E?.secureBackupSetupMethods
            ?: listOf("key", "passphrase")
    return if (methodList.contains("key") && methodList.contains("passphrase")) {
        SecureBackupMethod.KEY_OR_PASSPHRASE
    } else if (methodList.contains("key")) {
        SecureBackupMethod.KEY
    } else if (methodList.contains("passphrase")) {
        SecureBackupMethod.PASSPHRASE
    } else {
        SecureBackupMethod.KEY_OR_PASSPHRASE
    }
}
