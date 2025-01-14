/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.epoxy

import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R

/**
 * Item of size (0, 0).
 * It can be useful to avoid automatic scroll of RecyclerView with Epoxy controller, when the first valuable item changes.
 */
@EpoxyModelClass
abstract class ZeroItem : VectorEpoxyModel<ZeroItem.Holder>(R.layout.item_zero) {

    class Holder : VectorEpoxyHolder()
}
