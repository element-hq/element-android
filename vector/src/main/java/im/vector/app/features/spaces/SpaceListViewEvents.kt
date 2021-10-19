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

package im.vector.app.features.spaces

import im.vector.app.core.platform.VectorViewEvents

/**
 * Transient events for group list screen
 */
sealed class SpaceListViewEvents : VectorViewEvents {
    data class OpenSpace(val groupingMethodHasChanged: Boolean) : SpaceListViewEvents()
    data class OpenSpaceSummary(val id: String) : SpaceListViewEvents()
    data class OpenSpaceInvite(val id: String) : SpaceListViewEvents()
    object AddSpace : SpaceListViewEvents()
    data class OpenGroup(val groupingMethodHasChanged: Boolean) : SpaceListViewEvents()
}
