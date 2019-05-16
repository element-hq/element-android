/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.riotredesign.core.ui.list

import androidx.annotation.DrawableRes


/**
 * A generic list item.
 * Displays an item with a title, and optional description.
 * Can display an accessory on the right, that can be an image or an indeterminate progress.
 * If provided with an action, will display a button at the bottom of the list item.
 */
class GenericRecyclerViewItem(val title: String,
                              var description: String? = null,
                              val style: STYLE = STYLE.NORMAL_TEXT) {

    enum class STYLE {
        BIG_TEXT,
        NORMAL_TEXT
    }

    @DrawableRes
    var endIconResourceId: Int = -1

    var hasIndeterminateProcess = false

    var buttonAction: Action? = null

    var itemClickAction: Action? = null

    class Action(var title: String) {
        var perform: Runnable? = null
    }
}