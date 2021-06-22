/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.detail.upgrade

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import com.airbnb.epoxy.OnModelBuildFinishedListener
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetGenericListBinding
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

class MigrateRoomBottomSheet :
        VectorBaseBottomSheetDialogFragment<BottomSheetGenericListBinding>(),
        MigrateRoomViewModel.Factory, MigrateRoomController.InteractionListener {

    @Parcelize
    data class Args(
            val roomId: String,
            val newVersion: String
    ) : Parcelable

    @Inject
    lateinit var viewModelFactory: MigrateRoomViewModel.Factory

    override val showExpanded = true

    @Inject
    lateinit var epoxyController: MigrateRoomController

    val viewModel: MigrateRoomViewModel by fragmentViewModel()

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
            BottomSheetGenericListBinding.inflate(inflater, container, false)

    override fun invalidate() = withState(viewModel) { state ->
        epoxyController.setData(state)
        super.invalidate()

        when (val result = state.upgradingStatus) {
            is Success -> {
                val result = result.invoke()
                if (result is UpgradeRoomViewModelTask.Result.Success) {
                    setFragmentResult(REQUEST_KEY, Bundle().apply {
                        putString(BUNDLE_KEY_REPLACEMENT_ROOM, result.replacementRoomId)
                    })
                    dismiss()
                }
            }
        }
    }

    val postBuild = OnModelBuildFinishedListener {
        view?.post { forceExpandState() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        epoxyController.callback = this
        views.bottomSheetRecyclerView.configureWith(epoxyController)
        epoxyController.addModelBuildListener(postBuild)
    }

    override fun onDestroyView() {
        views.bottomSheetRecyclerView.cleanup()
        epoxyController.removeModelBuildListener(postBuild)
        super.onDestroyView()
    }

    override fun create(initialState: MigrateRoomViewState): MigrateRoomViewModel {
        return viewModelFactory.create(initialState)
    }

    companion object {

        const val REQUEST_KEY = "MigrateRoomBottomSheetRequest"
        const val BUNDLE_KEY_REPLACEMENT_ROOM = "BUNDLE_KEY_REPLACEMENT_ROOM"

        fun newInstance(roomId: String, newVersion: String)
                : MigrateRoomBottomSheet {
            return MigrateRoomBottomSheet().apply {
                setArguments(Args(roomId, newVersion))
            }
        }
    }

    override fun onAutoInvite(autoInvite: Boolean) {
        viewModel.handle(MigrateRoomAction.SetAutoInvite(autoInvite))
    }

    override fun onAutoUpdateParent(update: Boolean) {
        viewModel.handle(MigrateRoomAction.SetUpdateKnownParentSpace(update))
    }

    override fun onConfirmUpgrade() {
        viewModel.handle(MigrateRoomAction.UpgradeRoom)
    }
}
