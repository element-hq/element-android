/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.preview

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import im.vector.app.R

class SpaceTabView constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) :
        LinearLayout(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {}
    constructor(context: Context) : this(context, null, 0) {}

    var tabDepth = 0
        set(value) {
            if (field != value) {
                field = value
                setUpView()
            }
        }

    init {
        setUpView()
    }

    private fun setUpView() {
        // remove children
        removeAllViews()
        for (i in 0 until tabDepth) {
            inflate(context, R.layout.item_space_tab, this)
        }
    }
}
