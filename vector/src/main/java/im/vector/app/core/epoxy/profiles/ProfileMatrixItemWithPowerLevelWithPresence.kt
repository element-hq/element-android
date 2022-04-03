/*
 * Copyright 2021 New Vector Ltd
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

import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import org.matrix.android.sdk.api.session.presence.model.UserPresence

@EpoxyModelClass(layout = R.layout.item_profile_matrix_item)
abstract class ProfileMatrixItemWithPowerLevelWithPresence : ProfileMatrixItemWithPowerLevel() {

    @EpoxyAttribute var showPresence: Boolean = true
    @EpoxyAttribute var userPresence: UserPresence? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.presenceImageView.render(showPresence, userPresence)
    }
}
