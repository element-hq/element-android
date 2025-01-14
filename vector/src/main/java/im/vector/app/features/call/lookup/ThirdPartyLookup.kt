/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.lookup

import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.thirdparty.model.ThirdPartyUser

private const val LOOKUP_SUCCESS_FIELD = "lookup_success"

suspend fun Session.pstnLookup(phoneNumber: String, protocol: String?): List<ThirdPartyUser> {
    if (protocol == null) return emptyList()
    return tryOrNull {
        thirdPartyService().getThirdPartyUser(
                protocol = protocol,
                fields = mapOf("m.id.phone" to phoneNumber)
        )
    }.orEmpty()
}

suspend fun Session.sipVirtualLookup(nativeMxid: String): List<ThirdPartyUser> {
    return tryOrNull {
        thirdPartyService().getThirdPartyUser(
                protocol = PROTOCOL_SIP_VIRTUAL,
                fields = mapOf("native_mxid" to nativeMxid)
        )
    }
            .orEmpty()
            .filter {
                (it.fields[LOOKUP_SUCCESS_FIELD] as? Boolean).orFalse()
            }
}

suspend fun Session.sipNativeLookup(virtualMxid: String): List<ThirdPartyUser> {
    return tryOrNull {
        thirdPartyService().getThirdPartyUser(
                protocol = PROTOCOL_SIP_NATIVE,
                fields = mapOf("virtual_mxid" to virtualMxid)
        )
    }
            .orEmpty()
            .filter {
                (it.fields[LOOKUP_SUCCESS_FIELD] as? Boolean).orFalse()
            }
}
