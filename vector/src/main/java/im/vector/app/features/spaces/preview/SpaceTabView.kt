/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.spaces.preview

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import im.vector.app.R

class SpaceTabView constructor(context: Context,
                               attrs: AttributeSet? = null,
                               defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr) {

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
