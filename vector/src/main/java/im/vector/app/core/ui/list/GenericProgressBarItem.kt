/*
 * Copyright 2021 New Vector Ltd
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

import android.widget.ProgressBar
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel

/**
 * A generic progress bar item.
 */
@EpoxyModelClass(layout = R.layout.item_generic_progress)
abstract class GenericProgressBarItem : VectorEpoxyModel<GenericProgressBarItem.Holder>() {

    @EpoxyAttribute
    var progress: Int = 0

    @EpoxyAttribute
    var total: Int = 100

    @EpoxyAttribute
    var indeterminate: Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.progressbar.progress = progress
        holder.progressbar.max = total
        holder.progressbar.isIndeterminate = indeterminate
    }

    class Holder : VectorEpoxyHolder() {
        val progressbar by bind<ProgressBar>(R.id.genericProgressBar)
    }
}
