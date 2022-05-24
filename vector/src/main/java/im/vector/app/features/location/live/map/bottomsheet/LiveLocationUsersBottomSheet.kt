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

package im.vector.app.features.location.live.map.bottomsheet

import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.epoxy.SimpleEpoxyController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import im.vector.app.core.epoxy.BottomSheetDividerItem_
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.databinding.BottomSheetLiveLocationUsersBinding
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.util.MatrixItem

class LiveLocationUsersBottomSheet(
        private var avatarRenderer: AvatarRenderer
) : VectorBaseBottomSheetDialogFragment<BottomSheetLiveLocationUsersBinding>() {

    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private val controller = SimpleEpoxyController()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutParams = view.layoutParams
        layoutParams.height = Resources.getSystem().displayMetrics.heightPixels
        view.layoutParams = layoutParams
        view.requestLayout()

        views.bottomSheetRecyclerView.configureWith(controller, hasFixedSize = false)

        val models = dummyData.mapIndexed { index, item ->
            listOf(
                    LiveLocationUserItem_().apply {
                        id(index)
                        matrixItem(item)
                        avatarRenderer(this@LiveLocationUsersBottomSheet.avatarRenderer)
                        remainingTime("9min left")
                        lastUpdatedAt("Updated 2min ago")
                        showStopSharingButton(index == 1)
                    },
                    BottomSheetDividerItem_().apply {
                        id("divider_$index")
                    }
            )
        }

        controller.setModels(models.flatten())
    }

    override fun onStart() {
        super.onStart()
        val bottomSheet = (view?.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        bottomSheetBehavior = BottomSheetBehavior.from(view?.parent as View)
        bottomSheetBehavior?.setPeekHeight(DimensionConverter(resources).dpToPx(200), false)
    }

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetLiveLocationUsersBinding {
        return BottomSheetLiveLocationUsersBinding.inflate(inflater, container, false)
    }

    companion object {
        // Waiting for other PR to be merged
        private val dummyData = mutableListOf<MatrixItem>().apply {
            repeat(5) {
                add(
                        MatrixItem.UserItem(
                                "@test_$it:matrix.org",
                                "Test_$it",
                                "https://matrix-client.matrix.org/_matrix/media/r0/download/matrix.org/vQMHeiAfRxrEENOFnIMccZoI"
                        )
                )
            }
        }
    }
}
