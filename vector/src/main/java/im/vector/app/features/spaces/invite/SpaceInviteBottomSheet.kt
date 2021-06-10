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

package im.vector.app.features.spaces.invite

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.platform.ButtonStateView
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.utils.toast
import im.vector.app.databinding.BottomSheetInvitedToSpaceBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.matrixto.SpaceCardRenderer
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class SpaceInviteBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetInvitedToSpaceBinding>(), SpaceInviteBottomSheetViewModel.Factory {

    interface InteractionListener {
        fun spaceInviteBottomSheetOnAccept(spaceId: String)
        fun spaceInviteBottomSheetOnDecline(spaceId: String)
    }

    var interactionListener: InteractionListener? = null

    @Parcelize
    data class Args(
            val spaceId: String
    ) : Parcelable

    @Inject
    lateinit var avatarRenderer: AvatarRenderer

    @Inject
    lateinit var spaceCardRenderer: SpaceCardRenderer

    private val viewModel: SpaceInviteBottomSheetViewModel by fragmentViewModel(SpaceInviteBottomSheetViewModel::class)

    @Inject lateinit var viewModelFactory: SpaceInviteBottomSheetViewModel.Factory

    override fun create(initialState: SpaceInviteBottomSheetState) = viewModelFactory.create(initialState)

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override val showExpanded = true

    private val inviteArgs: Args by args()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.spaceCard.matrixToCardMainButton.commonClicked = {
            // quick local echo
            views.spaceCard.matrixToCardMainButton.render(ButtonStateView.State.Loading)
            views.spaceCard.matrixToCardSecondaryButton.button.isEnabled = false
            viewModel.handle(SpaceInviteBottomSheetAction.DoJoin)
        }
        views.spaceCard.matrixToCardSecondaryButton.commonClicked = {
            views.spaceCard.matrixToCardMainButton.button.isEnabled = false
            views.spaceCard.matrixToCardSecondaryButton.render(ButtonStateView.State.Loading)
            viewModel.handle(SpaceInviteBottomSheetAction.DoReject)
        }

        viewModel.observeViewEvents {
            when (it) {
                is SpaceInviteBottomSheetEvents.ShowError -> requireActivity().toast(it.message)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is InteractionListener) {
            interactionListener = context
        }
    }

    override fun onDetach() {
        interactionListener = null
        super.onDetach()
    }

    override fun invalidate() = withState(viewModel) { state ->
        super.invalidate()
        val summary = state.summary.invoke()
        val inviter = state.inviterUser.invoke()?.toMatrixItem()
        if (inviter != null) {
            views.inviterAvatarImage.isVisible = true
            views.inviterText.isVisible = true
            views.inviterMxid.isVisible = true
            avatarRenderer.render(inviter, views.inviterAvatarImage)
            views.inviterText.text = getString(R.string.user_invites_you, inviter.getBestName())
            views.inviterMxid.text = inviter.id
        } else {
            views.inviterAvatarImage.isVisible = false
            views.inviterText.isVisible = false
            views.inviterMxid.isVisible = false
        }

        spaceCardRenderer.render(summary, state.peopleYouKnow.invoke().orEmpty(), null, views.spaceCard)

        views.spaceCard.matrixToCardMainButton.button.text = getString(R.string.accept)
        views.spaceCard.matrixToCardSecondaryButton.button.text = getString(R.string.decline)

        when (state.joinActionState) {
            Uninitialized -> {
                views.spaceCard.matrixToCardMainButton.render(ButtonStateView.State.Button)
            }
            is Loading    -> {
                views.spaceCard.matrixToCardMainButton.render(ButtonStateView.State.Loading)
                views.spaceCard.matrixToCardSecondaryButton.button.isEnabled = false
            }
            is Success    -> {
                interactionListener?.spaceInviteBottomSheetOnAccept(inviteArgs.spaceId)
                dismiss()
            }
            is Fail       -> {
                views.spaceCard.matrixToCardMainButton.render(ButtonStateView.State.Error)
                views.spaceCard.matrixToCardSecondaryButton.button.isEnabled = true
            }
        }

        when (state.rejectActionState) {
            Uninitialized -> {
                views.spaceCard.matrixToCardSecondaryButton.render(ButtonStateView.State.Button)
            }
            is Loading    -> {
                views.spaceCard.matrixToCardSecondaryButton.render(ButtonStateView.State.Loading)
                views.spaceCard.matrixToCardMainButton.button.isEnabled = false
            }
            is Success    -> {
                interactionListener?.spaceInviteBottomSheetOnDecline(inviteArgs.spaceId)
                dismiss()
            }
            is Fail       -> {
                views.spaceCard.matrixToCardSecondaryButton.render(ButtonStateView.State.Error)
                views.spaceCard.matrixToCardSecondaryButton.button.isEnabled = true
            }
        }
    }

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetInvitedToSpaceBinding {
        return BottomSheetInvitedToSpaceBinding.inflate(inflater, container, false)
    }

    companion object {

        fun newInstance(spaceId: String)
                : SpaceInviteBottomSheet {
            return SpaceInviteBottomSheet().apply {
                setArguments(Args(spaceId))
            }
        }
    }
}
