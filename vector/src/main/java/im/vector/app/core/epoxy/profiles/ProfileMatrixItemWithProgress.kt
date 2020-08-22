/*
 * Copyright 2020 New Vector Ltd
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
 *
 */

package im.vector.app.core.epoxy.profiles

import android.widget.ProgressBar
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R

@EpoxyModelClass(layout = R.layout.item_profile_matrix_item_progress)
abstract class ProfileMatrixItemWithProgress : BaseProfileMatrixItem<ProfileMatrixItemWithProgress.Holder>() {

    @EpoxyAttribute var inProgress: Boolean = true

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.progress.isVisible = inProgress
    }

    class Holder : ProfileMatrixItem.Holder() {
        val progress by bind<ProgressBar>(R.id.matrixItemProgress)
    }
}
