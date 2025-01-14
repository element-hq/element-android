/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list

import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel

@EpoxyModelClass
abstract class SpaceDirectoryFilterNoResultsItem : VectorEpoxyModel<SpaceDirectoryFilterNoResultsItem.Holder>(R.layout.item_space_directory_filter_no_results) {
    class Holder : VectorEpoxyHolder()
}
