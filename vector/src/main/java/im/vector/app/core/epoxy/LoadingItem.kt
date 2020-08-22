/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.core.epoxy

import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.extensions.setTextOrHide

@EpoxyModelClass(layout = R.layout.item_loading)
abstract class LoadingItem : VectorEpoxyModel<LoadingItem.Holder>() {

    @EpoxyAttribute var loadingText: String? = null
    @EpoxyAttribute var showLoader: Boolean = true

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.progressBar.isVisible = showLoader
        holder.textView.setTextOrHide(loadingText)
    }

    class Holder : VectorEpoxyHolder() {
        val textView by bind<TextView>(R.id.loadingText)
        val progressBar by bind<ProgressBar>(R.id.loadingProgress)
    }
}
