/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.mdm

import android.content.Context

class NoOpMdmService : MdmService {
    override fun registerListener(context: Context, onChangedListener: () -> Unit) = Unit
    override fun unregisterListener(context: Context) = Unit
    override fun getData(mdmData: MdmData): String? = null
}
