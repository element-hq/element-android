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

package im.vector.app.features.matrixto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.ButtonStateView
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentMatrixToRoomSpaceCardBinding
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomType
import javax.inject.Inject

class MatrixToRoomSpaceFragment @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val spaceCardRenderer: SpaceCardRenderer
) : VectorBaseFragment<FragmentMatrixToRoomSpaceCardBinding>() {

    private val sharedViewModel: MatrixToBottomSheetViewModel by parentFragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentMatrixToRoomSpaceCardBinding {
        return FragmentMatrixToRoomSpaceCardBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.matrixToCardMainButton.commonClicked = { mainButtonClicked() }
        views.matrixToCardSecondaryButton.commonClicked = { secondaryButtonClicked() }
    }

    override fun invalidate() = withState(sharedViewModel) { state ->
        when (val item = state.roomPeekResult) {
            Uninitialized -> {
                views.matrixToCardContentVisibility.isVisible = false
            }
            is Loading -> {
                views.matrixToCardContentVisibility.isVisible = false
            }
            is Success -> {
                views.matrixToCardContentVisibility.isVisible = true
                when (val peek = item.invoke()) {
                    is RoomInfoResult.FullInfo     -> {
                        val matrixItem = peek.roomItem
                        avatarRenderer.render(matrixItem, views.matrixToCardAvatar)
                        if (peek.roomType == RoomType.SPACE) {
                            views.matrixToBetaTag.isVisible = true
                            views.matrixToAccessImage.isVisible = true
                            if (peek.isPublic) {
                                views.matrixToAccessText.setTextOrHide(context?.getString(R.string.public_space))
                                views.matrixToAccessImage.setImageResource(R.drawable.ic_public_room)
                            } else {
                                views.matrixToAccessText.setTextOrHide(context?.getString(R.string.private_space))
                                views.matrixToAccessImage.setImageResource(R.drawable.ic_room_private)
                            }
                        } else {
                            views.matrixToBetaTag.isVisible = false
                        }
                        views.matrixToCardNameText.setTextOrHide(peek.name)
                        views.matrixToCardAliasText.setTextOrHide(peek.alias)
                        views.matrixToCardDescText.setTextOrHide(peek.topic)
                        val memberCount = peek.memberCount
                        if (memberCount != null) {
                            views.matrixToMemberPills.isVisible = true
                            views.spaceChildMemberCountText.text = resources.getQuantityString(R.plurals.room_title_members, memberCount, memberCount)
                        } else {
                            // hide the pill
                            views.matrixToMemberPills.isVisible = false
                        }

                        val joinTextRes = if (peek.roomType == RoomType.SPACE) {
                            R.string.join_space
                        } else {
                            R.string.join_room
                        }

                        when (peek.membership) {
                            Membership.LEAVE,
                            Membership.NONE   -> {
                                views.matrixToCardMainButton.isVisible = true
                                views.matrixToCardMainButton.button.text = getString(joinTextRes)
                                views.matrixToCardSecondaryButton.isVisible = false
                            }
                            Membership.INVITE -> {
                                views.matrixToCardMainButton.isVisible = true
                                views.matrixToCardSecondaryButton.isVisible = true
                                views.matrixToCardMainButton.button.text = getString(joinTextRes)
                                views.matrixToCardSecondaryButton.button.text = getString(R.string.decline)
                            }
                            Membership.JOIN   -> {
                                views.matrixToCardMainButton.isVisible = true
                                views.matrixToCardSecondaryButton.isVisible = false
                                views.matrixToCardMainButton.button.text = getString(R.string.action_open)
                            }
                            Membership.KNOCK,
                            Membership.BAN    -> {
                                // What to do here ?
                                views.matrixToCardMainButton.isVisible = false
                                views.matrixToCardSecondaryButton.isVisible = false
                            }
                        }
                    }
                    is RoomInfoResult.PartialInfo  -> {
                        // It may still be possible to join
                        views.matrixToCardNameText.text = peek.roomId
                        views.matrixToCardAliasText.isVisible = false
                        views.matrixToMemberPills.isVisible = false
                        views.matrixToCardDescText.setTextOrHide(getString(R.string.room_preview_no_preview))

                        views.matrixToCardMainButton.button.text = getString(R.string.join_anyway)
                        views.matrixToCardSecondaryButton.isVisible = false
                    }
                    RoomInfoResult.NotFound        -> {
                        // we cannot join :/
                        views.matrixToCardNameText.isVisible = false
                        views.matrixToCardAliasText.isVisible = false
                        views.matrixToMemberPills.isVisible = false
                        views.matrixToCardDescText.setTextOrHide(getString(R.string.room_preview_not_found))

                        views.matrixToCardMainButton.isVisible = false
                        views.matrixToCardSecondaryButton.isVisible = false
                    }
                    is RoomInfoResult.UnknownAlias -> {
                        views.matrixToCardNameText.isVisible = false
                        views.matrixToCardAliasText.isVisible = false
                        views.spaceChildMemberCountText.isVisible = false
                        views.matrixToCardDescText.setTextOrHide(getString(R.string.room_alias_preview_not_found))

                        views.matrixToCardMainButton.isVisible = false
                        views.matrixToCardSecondaryButton.isVisible = false
                    }
                }
            }
            is Fail -> {
                // TODO display some error copy?
                sharedViewModel.handle(MatrixToAction.FailedToResolveUser)
            }
        }

        listOf(views.knownMember1, views.knownMember2, views.knownMember3, views.knownMember4, views.knownMember5)
                .onEach { it.isGone = true }
        when (state.peopleYouKnow) {
            is Success -> {
                val someYouKnow = state.peopleYouKnow.invoke()
                spaceCardRenderer.renderPeopleYouKnow(views, someYouKnow)
            }
            else       -> {
                views.peopleYouMayKnowText.isVisible = false
            }
        }

        when (state.startChattingState) {
            Uninitialized -> {
                views.matrixToCardMainButton.render(ButtonStateView.State.Button)
            }
            is Success -> {
                views.matrixToCardMainButton.render(ButtonStateView.State.Button)
            }
            is Fail -> {
                views.matrixToCardMainButton.render(ButtonStateView.State.Error)
                // TODO display some error copy?
            }
            is Loading -> {
                views.matrixToCardMainButton.render(ButtonStateView.State.Loading)
            }
        }
    }

    private fun mainButtonClicked() = withState(sharedViewModel) { state ->
        when (val info = state.roomPeekResult.invoke()) {
            is RoomInfoResult.FullInfo -> {
                when (info.membership) {
                    Membership.NONE,
                    Membership.INVITE,
                    Membership.LEAVE -> {
                        if (info.roomType == RoomType.SPACE) {
                            sharedViewModel.handle(MatrixToAction.JoinSpace(info.roomItem.id, info.viaServers))
                        } else {
                            sharedViewModel.handle(MatrixToAction.JoinRoom(info.roomItem.id, info.viaServers))
                        }
                    }
                    Membership.JOIN  -> {
                        if (info.roomType == RoomType.SPACE) {
                            sharedViewModel.handle(MatrixToAction.OpenSpace(info.roomItem.id))
                        } else {
                            sharedViewModel.handle(MatrixToAction.OpenRoom(info.roomItem.id))
                        }
                    }
                    else             -> {
                    }
                }
            }
            is RoomInfoResult.PartialInfo -> {
                // we can try to join anyway
                if (info.roomId != null) {
                    sharedViewModel.handle(MatrixToAction.JoinRoom(info.roomId, info.viaServers))
                }
            }
            else                          -> {
            }
        }
    }

    private fun secondaryButtonClicked() = withState(sharedViewModel) { _ ->
    }
}
