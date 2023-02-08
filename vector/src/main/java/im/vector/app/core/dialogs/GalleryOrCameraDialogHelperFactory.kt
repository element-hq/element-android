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

package im.vector.app.core.dialogs

import androidx.fragment.app.Fragment
import im.vector.app.core.resources.ColorProvider
import im.vector.lib.core.utils.timer.Clock
import javax.inject.Inject

/**
 * Factory for [GalleryOrCameraDialogHelper].
 */
class GalleryOrCameraDialogHelperFactory @Inject constructor(
        private val colorProvider: ColorProvider,
        private val clock: Clock,
) {
    fun create(fragment: Fragment): GalleryOrCameraDialogHelper {
        return GalleryOrCameraDialogHelper(fragment, colorProvider, clock)
    }
}
