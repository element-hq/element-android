/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.animations

import android.view.View
import com.google.android.material.appbar.AppBarLayout

class MatrixItemAppBarStateChangeListener(private val headerView: View, private val toolbarViews: List<View>) : AppBarStateChangeListener() {

    override fun onStateChanged(appBarLayout: AppBarLayout, state: State) {
        if (state == State.COLLAPSED) {
            headerView.visibility = View.INVISIBLE
            toolbarViews.forEach {
                it.animate().alpha(1f).duration = 150
            }
        } else {
            headerView.visibility = View.VISIBLE
            toolbarViews.forEach {
                it.animate().alpha(0f).duration = 150
            }
        }
    }
}
