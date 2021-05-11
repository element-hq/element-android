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

package im.vector.app.core.ui.list

import android.graphics.Typeface

enum class ItemStyle {
    BIG_TEXT,
    NORMAL_TEXT,
    TITLE,
    SUBHEADER;

    fun toTypeFace(): Typeface {
        return if (this == TITLE) {
            Typeface.DEFAULT_BOLD
        } else {
            Typeface.DEFAULT
        }
    }

    fun toTextSize(): Float {
        return when (this) {
            BIG_TEXT    -> 18f
            NORMAL_TEXT -> 14f
            TITLE       -> 20f
            SUBHEADER   -> 16f
        }
    }
}
