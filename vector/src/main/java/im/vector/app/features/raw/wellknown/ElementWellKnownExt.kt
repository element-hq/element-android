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
