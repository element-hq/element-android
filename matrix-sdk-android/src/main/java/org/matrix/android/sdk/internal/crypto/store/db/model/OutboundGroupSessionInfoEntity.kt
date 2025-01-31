/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.model

import io.realm.RealmObject
import org.matrix.android.sdk.internal.crypto.store.db.deserializeFromRealm
import org.matrix.android.sdk.internal.crypto.store.db.serializeForRealm
import org.matrix.olm.OlmOutboundGroupSession
import timber.log.Timber

internal open class OutboundGroupSessionInfoEntity(
        var serializedOutboundSessionData: String? = null,
        var creationTime: Long? = null,
        var shouldShareHistory: Boolean = false
) : RealmObject() {

    fun getOutboundGroupSession(): OlmOutboundGroupSession? {
        return try {
            deserializeFromRealm(serializedOutboundSessionData)
        } catch (failure: Throwable) {
            Timber.e(failure, "## getOutboundGroupSession() Deserialization failure")
            return null
        }
    }

    fun putOutboundGroupSession(olmOutboundGroupSession: OlmOutboundGroupSession?) {
        serializedOutboundSessionData = serializeForRealm(olmOutboundGroupSession)
    }

    companion object
}
