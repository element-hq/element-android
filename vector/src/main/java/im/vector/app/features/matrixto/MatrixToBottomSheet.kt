/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.matrixto

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.commitTransaction
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetMatrixToCardBinding
import im.vector.app.features.home.AvatarRenderer
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.reflect.KClass

class MatrixToBottomSheet :
        VectorBaseBottomSheetDialogFragment<BottomSheetMatrixToCardBinding>() {

    @Parcelize
    data class MatrixToArgs(
            val matrixToLink: String
    ) : Parcelable

    @Inject lateinit var avatarRenderer: AvatarRenderer

    @Inject
    lateinit var matrixToBottomSheetViewModelFactory: MatrixToBottomSheetViewModel.Factory

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    private var weakReference = WeakReference<InteractionListener>(null)

    var interactionListener: InteractionListener?
        set(value) {
            weakReference = WeakReference(value)
        }
        get() = weakReference.get()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetMatrixToCardBinding {
        return BottomSheetMatrixToCardBinding.inflate(inflater, container, false)
    }

    private val viewModel by fragmentViewModel(MatrixToBottomSheetViewModel::class)

    interface InteractionListener {
        fun navigateToRoom(roomId: String)
        fun switchToSpace(spaceId: String) {}
    }

    override fun invalidate() = withState(viewModel) { state ->
        super.invalidate()
        when (state.linkType) {
            is PermalinkData.RoomLink -> {
                views.matrixToCardContentLoading.isVisible = state.roomPeekResult is Incomplete
                showFragment(MatrixToRoomSpaceFragment::class, Bundle())
            }
            is PermalinkData.UserLink -> {
                views.matrixToCardContentLoading.isVisible = state.matrixItem is Incomplete
                showFragment(MatrixToUserFragment::class, Bundle())
            }
            is PermalinkData.GroupLink -> {
            }
            is PermalinkData.FallbackLink -> {
            }
        }
    }

    private fun showFragment(fragmentClass: KClass<out Fragment>, bundle: Bundle) {
        if (childFragmentManager.findFragmentByTag(fragmentClass.simpleName) == null) {
            childFragmentManager.commitTransaction {
                replace(views.matrixToCardFragmentContainer.id,
                        fragmentClass.java,
                        bundle,
                        fragmentClass.simpleName
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.observeViewEvents {
            when (it) {
                is MatrixToViewEvents.NavigateToRoom -> {
                    interactionListener?.navigateToRoom(it.roomId)
                    dismiss()
                }
                MatrixToViewEvents.Dismiss -> dismiss()
                is MatrixToViewEvents.NavigateToSpace -> {
                    interactionListener?.switchToSpace(it.spaceId)
                    dismiss()
                }
                is MatrixToViewEvents.ShowModalError -> {
                    MaterialAlertDialogBuilder(requireContext())
                            .setMessage(it.error)
                            .setPositiveButton(getString(R.string.ok), null)
                            .show()
                }
            }
        }
    }

    companion object {
        fun withLink(matrixToLink: String, listener: InteractionListener?): MatrixToBottomSheet {
            return MatrixToBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelable(MvRx.KEY_ARG, MatrixToArgs(
                            matrixToLink = matrixToLink
                    ))
                }
                interactionListener = listener
            }
        }
    }
}
