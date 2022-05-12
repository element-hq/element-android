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

package im.vector.app.features.poll.create

import android.widget.RadioGroup
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import org.matrix.android.sdk.api.session.room.model.message.PollType

@EpoxyModelClass(layout = R.layout.item_poll_type_selection)
abstract class PollTypeSelectionItem : VectorEpoxyModel<PollTypeSelectionItem.Holder>() {

    @EpoxyAttribute
    var pollType: PollType = PollType.DISCLOSED_UNSTABLE

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var pollTypeChangedListener: RadioGroup.OnCheckedChangeListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)

        holder.pollTypeRadioGroup.check(
                when (pollType) {
                    PollType.DISCLOSED_UNSTABLE, PollType.DISCLOSED     -> R.id.openPollTypeRadioButton
                    PollType.UNDISCLOSED_UNSTABLE, PollType.UNDISCLOSED -> R.id.closedPollTypeRadioButton
                }
        )

        holder.pollTypeRadioGroup.setOnCheckedChangeListener(pollTypeChangedListener)
    }

    override fun unbind(holder: Holder) {
        super.unbind(holder)
        holder.pollTypeRadioGroup.setOnCheckedChangeListener(null)
    }

    class Holder : VectorEpoxyHolder() {
        val pollTypeRadioGroup by bind<RadioGroup>(R.id.pollTypeRadioGroup)
    }
}
