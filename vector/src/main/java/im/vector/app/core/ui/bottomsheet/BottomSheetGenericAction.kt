/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.core.ui.bottomsheet

import androidx.annotation.DrawableRes
import im.vector.app.core.epoxy.bottomsheet.BottomSheetActionItem_
import im.vector.app.core.platform.VectorSharedAction

/**
 * Parent class for a bottom sheet action
 */
open class BottomSheetGenericAction(
        open val title: String,
        @DrawableRes open val iconResId: Int,
        open val isSelected: Boolean,
        open val destructive: Boolean
) : VectorSharedAction {

    fun toBottomSheetItem(): BottomSheetActionItem_ {
        return BottomSheetActionItem_().apply {
            id("action_$title")
            iconRes(iconResId)
            text(title)
            selected(isSelected)
            destructive(destructive)
        }
    }
}
