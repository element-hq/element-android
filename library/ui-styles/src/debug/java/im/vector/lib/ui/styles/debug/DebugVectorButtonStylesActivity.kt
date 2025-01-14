/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.ui.styles.debug

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import im.vector.lib.ui.styles.databinding.ActivityDebugButtonStylesBinding

abstract class DebugVectorButtonStylesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val views = ActivityDebugButtonStylesBinding.inflate(layoutInflater)
        setContentView(views.root)
    }
}
