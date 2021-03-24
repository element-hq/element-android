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
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.utils.startSharePlainTextIntent
import im.vector.app.databinding.BottomSheetSpaceInviteBinding
import im.vector.app.features.invite.InviteUsersToRoomActivity
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

class ShareSpaceBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetSpaceInviteBinding>() {

    @Parcelize
    data class Args(
            val spaceId: String
    ) : Parcelable

    override val showExpanded = true

    @Inject
    lateinit var activeSessionHolder: ActiveSessionHolder

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetSpaceInviteBinding {
        return BottomSheetSpaceInviteBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Not going for full view model for now, as it may change

        val args: Args = arguments?.getParcelable(EXTRA_ARGS)
                ?: return Unit.also { dismiss() }
        val summary = activeSessionHolder.getSafeActiveSession()?.spaceService()?.getSpace(args.spaceId)?.spaceSummary()

        val spaceName = summary?.name
        views.descriptionText.text = getString(R.string.invite_people_to_your_space_desc, spaceName)

        // XXX enable back when supported
        views.inviteByMailButton.isVisible = false
        views.inviteByMailButton.debouncedClicks {
        }

        views.inviteByMxidButton.debouncedClicks {
            val intent = InviteUsersToRoomActivity.getIntent(requireContext(), args.spaceId)
            startActivity(intent)
        }

        views.inviteByLinkButton.debouncedClicks {
            activeSessionHolder.getSafeActiveSession()?.permalinkService()?.createRoomPermalink(args.spaceId)?.let { permalink ->
                startSharePlainTextIntent(
                        fragment = this,
                        activityResultLauncher = null,
                        chooserTitle = getString(R.string.share_by_text),
                        text = getString(R.string.share_space_link_message, spaceName, permalink),
                        extraTitle = getString(R.string.share_space_link_message, spaceName, permalink)
                )
            }
        }

//        views.skipButton.debouncedClicks {
//            dismiss()
//        }
    }

    companion object {

        const val EXTRA_ARGS = "EXTRA_ARGS"

        fun show(fragmentManager: FragmentManager, spaceId: String): ShareSpaceBottomSheet {
            return ShareSpaceBottomSheet().apply {
                isCancelable = true
                arguments = Bundle().apply {
                    this.putParcelable(EXTRA_ARGS, ShareSpaceBottomSheet.Args(spaceId = spaceId))
                }
            }.also {
                it.show(fragmentManager, ShareSpaceBottomSheet::class.java.name)
            }
        }
    }
}
