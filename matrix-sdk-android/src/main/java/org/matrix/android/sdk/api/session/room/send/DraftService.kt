/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.send

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.util.Optional

interface DraftService {

    /**
     * Save or update a draft to the room.
     */
    suspend fun saveDraft(draft: UserDraft)

    /**
     * Delete the last draft, basically just after sending the message.
     */
    suspend fun deleteDraft()

    /**
     * Return the current draft or null.
     */
    fun getDraft(): UserDraft?

    /**
     * Return the current draft if any, as a live data.
     */
    fun getDraftLive(): LiveData<Optional<UserDraft>>
}
