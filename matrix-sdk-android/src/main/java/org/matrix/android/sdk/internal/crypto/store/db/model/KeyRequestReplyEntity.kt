/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.model

import io.realm.RealmObject
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.di.MoshiProvider

internal open class KeyRequestReplyEntity(
        var senderId: String? = null,
        var fromDevice: String? = null,
        var eventJson: String? = null
) : RealmObject() {
    companion object

    fun getEvent(): Event? {
        return eventJson?.let {
            MoshiProvider.providesMoshi().adapter(Event::class.java).fromJson(it)
        }
    }
}
