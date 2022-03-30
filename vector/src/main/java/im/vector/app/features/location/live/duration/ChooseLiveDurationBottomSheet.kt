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

package im.vector.app.features.location.live.duration

import android.view.LayoutInflater
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetChooseLiveLocationShareDurationBinding
import im.vector.app.features.home.room.detail.timeline.action.MessageSharedActionViewModel

/**
 * Bottom sheet displaying list of options to choose the duration of the live sharing.
 */
@AndroidEntryPoint
class ChooseLiveDurationBottomSheet :
        VectorBaseBottomSheetDialogFragment<BottomSheetChooseLiveLocationShareDurationBinding>() {

    // TODO show same UI as in Figma
    // TODO handle choice of user

    var durationChoiceListener: DurationChoiceListener? = null

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetChooseLiveLocationShareDurationBinding {
        return BottomSheetChooseLiveLocationShareDurationBinding.inflate(inflater, container, false)
    }

    override fun onDestroyView() {
        durationChoiceListener = null
        super.onDestroyView()
    }

    // we are not using state for this one as it's static, so no need to override invalidate()

    companion object {
        fun newInstance(durationChoiceListener: DurationChoiceListener): ChooseLiveDurationBottomSheet {
            val bottomSheet = ChooseLiveDurationBottomSheet()
            bottomSheet.durationChoiceListener = durationChoiceListener
            return bottomSheet
        }
    }

    interface DurationChoiceListener {
        fun onDurationChoice(durationMillis: Long)
    }
}
