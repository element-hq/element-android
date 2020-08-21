/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
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
