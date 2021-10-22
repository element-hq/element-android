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

package im.vector.app.features.roomprofile.alias.detail

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetGenericListBinding
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
data class RoomAliasBottomSheetArgs(
        val alias: String,
        val isPublished: Boolean,
        val isMainAlias: Boolean,
        val isLocal: Boolean,
        val canEditCanonicalAlias: Boolean
) : Parcelable

/**
 * Bottom sheet fragment that shows room alias information with list of contextual actions
 */
@AndroidEntryPoint
class RoomAliasBottomSheet :
        VectorBaseBottomSheetDialogFragment<BottomSheetGenericListBinding>(),
        RoomAliasBottomSheetController.Listener {

    private lateinit var sharedActionViewModel: RoomAliasBottomSheetSharedActionViewModel
    @Inject lateinit var sharedViewPool: RecyclerView.RecycledViewPool
    @Inject lateinit var controller: RoomAliasBottomSheetController

    private val viewModel: RoomAliasBottomSheetViewModel by fragmentViewModel(RoomAliasBottomSheetViewModel::class)

    override val showExpanded = true

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetGenericListBinding {
        return BottomSheetGenericListBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(RoomAliasBottomSheetSharedActionViewModel::class.java)
        views.bottomSheetRecyclerView.configureWith(controller, viewPool = sharedViewPool, hasFixedSize = false, disableItemAnimation = true)
        controller.listener = this
    }

    override fun onDestroyView() {
        views.bottomSheetRecyclerView.cleanup()
        controller.listener = null
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) {
        controller.setData(it)
        super.invalidate()
    }

    override fun didSelectMenuAction(quickAction: RoomAliasBottomSheetSharedAction) {
        sharedActionViewModel.post(quickAction)

        dismiss()
    }

    companion object {
        fun newInstance(alias: String,
                        isPublished: Boolean,
                        isMainAlias: Boolean,
                        isLocal: Boolean,
                        canEditCanonicalAlias: Boolean): RoomAliasBottomSheet {
            return RoomAliasBottomSheet().apply {
                setArguments(RoomAliasBottomSheetArgs(
                        alias = alias,
                        isPublished = isPublished,
                        isMainAlias = isMainAlias,
                        isLocal = isLocal,
                        canEditCanonicalAlias = canEditCanonicalAlias
                ))
            }
        }
    }
}
