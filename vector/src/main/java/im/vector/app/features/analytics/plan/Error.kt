/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.plan

import im.vector.app.features.analytics.itf.VectorAnalyticsEvent

// GENERATED FILE, DO NOT EDIT. FOR MORE INFORMATION VISIT
// https://github.com/matrix-org/matrix-analytics-events/

/**
 * Triggered when an error occurred.
 */
data class Error(
        /**
         * Context - client defined, can be used for debugging.
         */
        val context: String? = null,
        val domain: Domain,
        val name: Name,
) : VectorAnalyticsEvent {

    enum class Domain {
        E2EE,
        TO_DEVICE,
        VOIP,
    }

    enum class Name {
        OlmIndexError,
        OlmKeysNotSentError,
        OlmUnspecifiedError,
        ToDeviceFailedToDecrypt,
        UnknownError,
        VoipIceFailed,
        VoipIceTimeout,
        VoipInviteTimeout,
        VoipUserHangup,
        VoipUserMediaFailed,
    }

    override fun getName() = "Error"

    override fun getProperties(): Map<String, Any>? {
        return mutableMapOf<String, Any>().apply {
            context?.let { put("context", it) }
            put("domain", domain.name)
            put("name", name.name)
        }.takeIf { it.isNotEmpty() }
    }
}
