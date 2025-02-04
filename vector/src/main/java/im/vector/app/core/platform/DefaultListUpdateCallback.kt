/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.platform

import androidx.recyclerview.widget.ListUpdateCallback

interface DefaultListUpdateCallback : ListUpdateCallback {

    override fun onChanged(position: Int, count: Int, tag: Any?) {
        // no-op
    }

    override fun onMoved(position: Int, count: Int) {
        // no-op
    }

    override fun onInserted(position: Int, count: Int) {
        // no-op
    }

    override fun onRemoved(position: Int, count: Int) {
        // no-op
    }
}
