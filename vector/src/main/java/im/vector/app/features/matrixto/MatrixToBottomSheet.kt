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
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetMatrixToCardBinding
import im.vector.app.features.home.AvatarRenderer
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

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

    private var interactionListener: InteractionListener? = null

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetMatrixToCardBinding {
        return BottomSheetMatrixToCardBinding.inflate(inflater, container, false)
    }

    private val viewModel by fragmentViewModel(MatrixToBottomSheetViewModel::class)

    interface InteractionListener {
        fun navigateToRoom(roomId: String)
    }

    override fun invalidate() = withState(viewModel) { state ->
        super.invalidate()
        when (val item = state.matrixItem) {
            Uninitialized -> {
                views.matrixToCardContentLoading.isVisible = false
                views.matrixToCardUserContentVisibility.isVisible = false
            }
            is Loading -> {
                views.matrixToCardContentLoading.isVisible = true
                views.matrixToCardUserContentVisibility.isVisible = false
            }
            is Success -> {
                views.matrixToCardContentLoading.isVisible = false
                views.matrixToCardUserContentVisibility.isVisible = true
                views.matrixToCardNameText.setTextOrHide(item.invoke().displayName)
                views.matrixToCardUserIdText.setTextOrHide(item.invoke().id)
                avatarRenderer.render(item.invoke(), views.matrixToCardAvatar)
            }
            is Fail -> {
                // TODO display some error copy?
                dismiss()
            }
        }

        when (state.startChattingState) {
            Uninitialized -> {
                views.matrixToCardButtonLoading.isVisible = false
                views.matrixToCardSendMessageButton.isVisible = false
            }
            is Success -> {
                views.matrixToCardButtonLoading.isVisible = false
                views.matrixToCardSendMessageButton.isVisible = true
            }
            is Fail -> {
                views.matrixToCardButtonLoading.isVisible = false
                views.matrixToCardSendMessageButton.isVisible = true
                // TODO display some error copy?
                dismiss()
            }
            is Loading -> {
                views.matrixToCardButtonLoading.isVisible = true
                views.matrixToCardSendMessageButton.isInvisible = true
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.matrixToCardSendMessageButton.debouncedClicks {
            withState(viewModel) {
                it.matrixItem.invoke()?.let { item ->
                    viewModel.handle(MatrixToAction.StartChattingWithUser(item))
                }
            }
        }

        viewModel.observeViewEvents {
            when (it) {
                is MatrixToViewEvents.NavigateToRoom -> {
                    interactionListener?.navigateToRoom(it.roomId)
                    dismiss()
                }
                MatrixToViewEvents.Dismiss -> dismiss()
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
