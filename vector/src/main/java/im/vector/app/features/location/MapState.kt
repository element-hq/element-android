/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.location

import android.graphics.drawable.Drawable
import androidx.annotation.Px

data class MapState(
        val zoomOnlyOnce: Boolean,
        val userLocationData: LocationData? = null,
        val pinId: String,
        val pinDrawable: Drawable? = null,
        val showPin: Boolean = true,
        @Px val logoMarginBottom: Int = 0
)
