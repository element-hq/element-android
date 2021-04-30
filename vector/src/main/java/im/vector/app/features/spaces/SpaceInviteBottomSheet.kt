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

package im.vector.app.features.spaces

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.airbnb.mvrx.args
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.ButtonStateView
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.utils.toast
import im.vector.app.databinding.BottomSheetInvitedToSpaceBinding
import im.vector.app.features.home.AvatarRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class SpaceInviteBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetInvitedToSpaceBinding>() {

    interface InteractionListener {
        fun onAccept(spaceId: String)
        fun onDecline(spaceId: String)
    }

    var interactionListener: InteractionListener? = null

    @Parcelize
    data class Args(
            val spaceId: String
    ) : Parcelable

    @Inject
    lateinit var activeSessionHolder: ActiveSessionHolder

    @Inject
    lateinit var avatarRenderer: AvatarRenderer

    @Inject
    lateinit var errorFormatter: ErrorFormatter

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override val showExpanded = true

    private val inviteArgs: Args by args()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val session = activeSessionHolder.getSafeActiveSession() ?: return

        val summary = session.getRoomSummary(inviteArgs.spaceId) ?: return Unit.also {
            dismiss()
        }

        val inviter = summary.inviterId?.let { session.getUser(it) }
        if (inviter != null) {
            views.inviterAvatarImage.isVisible = true
            views.inviterText.isVisible = true
            views.inviterMxid.isVisible = true
            avatarRenderer.render(inviter.toMatrixItem(), views.inviterAvatarImage)
            views.inviterText.text = getString(R.string.user_invites_you, inviter.getBestName())
            views.inviterMxid.text = inviter.userId
        } else {
            views.inviterAvatarImage.isVisible = false
            views.inviterText.isVisible = false
            views.inviterMxid.isVisible = false
        }

        views.spaceCard.matrixToCardContentVisibility.isVisible = true
        avatarRenderer.renderSpace(summary.toMatrixItem(), views.spaceCard.matrixToCardAvatar)
        views.spaceCard.matrixToCardNameText.text = summary.displayName
        views.spaceCard.matrixToBetaTag.isVisible = true
        views.spaceCard.matrixToCardAliasText.setTextOrHide(summary.canonicalAlias)
        views.spaceCard.matrixToCardDescText.setTextOrHide(summary.topic)

        views.spaceCard.matrixToCardMainButton.render(ButtonStateView.State.Button)
        views.spaceCard.matrixToCardMainButton.button.text = getString(R.string.accept)
        views.spaceCard.matrixToCardMainButton.callback = object : ButtonStateView.Callback {
            override fun onButtonClicked() {
                doJoin()
            }

            override fun onRetryClicked() {
                doJoin()
            }

            private fun doJoin() {
                views.spaceCard.matrixToCardMainButton.render(ButtonStateView.State.Loading)
                views.spaceCard.matrixToCardSecondaryButton.button.isEnabled = false
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        activeSessionHolder.getSafeActiveSession()?.getRoom(inviteArgs.spaceId)?.join()
                        withContext(Dispatchers.Main) {
                            if (!isAdded) return@withContext
                            views.spaceCard.matrixToCardMainButton.render(ButtonStateView.State.Loaded)
                            views.spaceCard.matrixToCardSecondaryButton.isEnabled = true
                            interactionListener?.onAccept(inviteArgs.spaceId)
                            dismiss()
                        }
                    } catch (failure: Throwable) {
                        withContext(Dispatchers.Main) {
                            if (!isAdded) return@withContext
                            requireActivity().toast(errorFormatter.toHumanReadable(failure))
                            views.spaceCard.matrixToCardMainButton.render(ButtonStateView.State.Error)
                            views.spaceCard.matrixToCardSecondaryButton.button.isEnabled = true
                        }
                    }
                }
            }
        }

        views.spaceCard.matrixToCardSecondaryButton.render(ButtonStateView.State.Button)
        views.spaceCard.matrixToCardSecondaryButton.button.text = getString(R.string.reject)
        views.spaceCard.matrixToCardSecondaryButton.callback = object : ButtonStateView.Callback {
            override fun onButtonClicked() {
                doJoin()
            }

            override fun onRetryClicked() {
                doJoin()
            }

            private fun doJoin() {
                views.spaceCard.matrixToCardSecondaryButton.render(ButtonStateView.State.Loading)
                views.spaceCard.matrixToCardSecondaryButton.button.isEnabled = false
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        activeSessionHolder.getSafeActiveSession()?.getRoom(inviteArgs.spaceId)?.leave()
                        withContext(Dispatchers.Main) {
                            if (!isAdded) return@withContext
                            views.spaceCard.matrixToCardSecondaryButton.render(ButtonStateView.State.Loaded)
                            views.spaceCard.matrixToCardMainButton.button.isEnabled = true
                            interactionListener?.onDecline(inviteArgs.spaceId)
                            dismiss()
                        }
                    } catch (failure: Throwable) {
                        withContext(Dispatchers.Main) {
                            if (!isAdded) return@withContext
                            requireActivity().toast(errorFormatter.toHumanReadable(failure))
                            views.spaceCard.matrixToCardSecondaryButton.render(ButtonStateView.State.Error)
                            views.spaceCard.matrixToCardMainButton.button.isEnabled = true
                        }
                    }
                }
            }
        }

        val memberCount = summary.otherMemberIds.size
        if (memberCount != 0) {
            views.spaceCard.matrixToMemberPills.isVisible = true
            views.spaceCard.spaceChildMemberCountText.text = resources.getQuantityString(R.plurals.room_title_members, memberCount, memberCount)
        } else {
            // hide the pill
            views.spaceCard.matrixToMemberPills.isVisible = false
        }

        val knownMembers = summary.otherMemberIds.filter {
            session.getExistingDirectRoomWithUser(it) != null
        }.mapNotNull { session.getUser(it) }
        // put one with avatar first, and take 5
        val peopleYouKnow = (knownMembers.filter { it.avatarUrl != null } + knownMembers.filter { it.avatarUrl == null })
                .take(5)

        val images = listOf(
                views.spaceCard.knownMember1,
                views.spaceCard.knownMember2,
                views.spaceCard.knownMember3,
                views.spaceCard.knownMember4,
                views.spaceCard.knownMember5
        ).onEach { it.isGone = true }

        if (peopleYouKnow.isEmpty()) {
            views.spaceCard.peopleYouMayKnowText.isVisible = false
        } else {
            peopleYouKnow.forEachIndexed { index, item ->
                images[index].isVisible = true
                avatarRenderer.render(item.toMatrixItem(), images[index])
            }
            views.spaceCard.peopleYouMayKnowText.setTextOrHide(
                    resources.getQuantityString(R.plurals.space_people_you_know,
                            peopleYouKnow.count(),
                            peopleYouKnow.count()
                    )
            )
        }
    }

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetInvitedToSpaceBinding {
        return BottomSheetInvitedToSpaceBinding.inflate(inflater, container, false)
    }

    companion object {

        fun newInstance(spaceId: String, interactionListener: InteractionListener)
                : SpaceInviteBottomSheet {
            return SpaceInviteBottomSheet().apply {
                this.interactionListener = interactionListener
                setArguments(Args(spaceId))
            }
        }
    }
}
