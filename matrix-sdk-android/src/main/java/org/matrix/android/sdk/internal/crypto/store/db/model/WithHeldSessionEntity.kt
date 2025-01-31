/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.model

import io.realm.RealmObject
import io.realm.annotations.Index
import org.matrix.android.sdk.api.session.events.model.content.WithHeldCode

/**
 * When an encrypted message is sent in a room, the megolm key might not be sent to all devices present in the room.
 * Sometimes this may be inadvertent (for example, if the sending device is not aware of some devices that have joined),
 * but some times, this may be purposeful.
 * For example, the sender may have blacklisted certain devices or users,
 * or may be choosing to not send the megolm key to devices that they have not verified yet.
 */
internal open class WithHeldSessionEntity(
        var roomId: String? = null,
        var algorithm: String? = null,
        @Index var sessionId: String? = null,
        @Index var senderKey: String? = null,
        var codeString: String? = null,
        var reason: String? = null
) : RealmObject() {

    var code: WithHeldCode?
        get() {
            return WithHeldCode.fromCode(codeString)
        }
        set(code) {
            codeString = code?.value
        }

    companion object
}
