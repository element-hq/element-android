/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.ui.list

import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel

/**
 * A generic list item header left aligned with notice color.
 */
@EpoxyModelClass
abstract class GenericLoaderItem : VectorEpoxyModel<GenericLoaderItem.Holder>(R.layout.item_generic_loader) {

    // Maybe/Later add some style configuration, SMALL/BIG ?

    class Holder : VectorEpoxyHolder()
}
