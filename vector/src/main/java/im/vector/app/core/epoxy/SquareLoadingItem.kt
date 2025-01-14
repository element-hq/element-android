/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.epoxy

import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R

@EpoxyModelClass
abstract class SquareLoadingItem : VectorEpoxyModel<SquareLoadingItem.Holder>(R.layout.item_loading_square) {

    class Holder : VectorEpoxyHolder()
}
