/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.algorithms.megolm

import dagger.Lazy
import org.matrix.android.sdk.api.crypto.MXCryptoConfig
import org.matrix.android.sdk.internal.crypto.MXOlmDevice
import org.matrix.android.sdk.internal.crypto.OutgoingKeyRequestManager
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.StreamEventsManager
import org.matrix.android.sdk.internal.util.time.Clock
import javax.inject.Inject

internal class MXMegolmDecryptionFactory @Inject constructor(
        private val olmDevice: MXOlmDevice,
        @UserId private val myUserId: String,
        private val outgoingKeyRequestManager: OutgoingKeyRequestManager,
        private val cryptoStore: IMXCryptoStore,
        private val eventsManager: Lazy<StreamEventsManager>,
        private val unrequestedForwardManager: UnRequestedForwardManager,
        private val mxCryptoConfig: MXCryptoConfig,
        private val clock: Clock,
) {

    fun create(): MXMegolmDecryption {
        return MXMegolmDecryption(
                olmDevice = olmDevice,
                myUserId = myUserId,
                outgoingKeyRequestManager = outgoingKeyRequestManager,
                cryptoStore = cryptoStore,
                liveEventManager = eventsManager,
                unrequestedForwardManager = unrequestedForwardManager,
                cryptoConfig = mxCryptoConfig,
                clock = clock,
        )
    }
}
