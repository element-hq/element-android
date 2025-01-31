/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.room.send

import androidx.work.Data
import androidx.work.InputMerger

/**
 * InputMerger which takes only the first input, to ensure an appended work will only have the specified parameters.
 */
internal class NoMerger : InputMerger() {
    override fun merge(inputs: MutableList<Data>): Data {
        return inputs.first()
    }
}
