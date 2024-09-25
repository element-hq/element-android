/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app.features.raw.wellknown

import im.vector.app.features.crypto.keysrequest.OutboundSessionKeySharingStrategy

data class CryptoConfig(
        val fallbackKeySharingStrategy: OutboundSessionKeySharingStrategy
)
