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
package im.vector.riotredesign.features.home.room.detail.timeline.action

import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProviders
import butterknife.BindView
import butterknife.ButterKnife
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import im.vector.riotredesign.R
import im.vector.riotredesign.core.glide.GlideApp
import im.vector.riotredesign.features.home.AvatarRenderer
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageInformationData
import kotlinx.android.parcel.Parcelize

/**
 * Bottom sheet fragment that shows a message preview with list of contextual actions
 * (Includes fragments for quick reactions and list of actions)
 */
class MessageActionsBottomSheet : BaseMvRxBottomSheetDialog() {

    private val viewModel: MessageActionsViewModel by fragmentViewModel(MessageActionsViewModel::class)

    private lateinit var actionHandlerModel: ActionsHandler

    @BindView(R.id.bottom_sheet_message_preview_avatar)
    lateinit var senderAvatarImageView: ImageView

    @BindView(R.id.bottom_sheet_message_preview_sender)
    lateinit var senderNameTextView: TextView

    @BindView(R.id.bottom_sheet_message_preview_timestamp)
    lateinit var messageTimestampText: TextView

    @BindView(R.id.bottom_sheet_message_preview_body)
    lateinit var messageBodyTextView: TextView


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_message_actions, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        actionHandlerModel = ViewModelProviders.of(requireActivity()).get(ActionsHandler::class.java)

        val cfm = childFragmentManager
        var menuActionFragment = cfm.findFragmentByTag("MenuActionFragment") as? MessageMenuFragment
        if (menuActionFragment == null) {
            menuActionFragment = MessageMenuFragment.newInstance(arguments!!.get(MvRx.KEY_ARG) as ParcelableArgs)
            cfm.beginTransaction()
                    .replace(R.id.bottom_sheet_menu_container, menuActionFragment, "MenuActionFragment")
                    .commit()
        }
        menuActionFragment.interactionListener = object : MessageMenuFragment.InteractionListener {
            override fun didSelectMenuAction(simpleAction: SimpleAction) {
                actionHandlerModel.fireAction(simpleAction.uid, simpleAction.data)
                dismiss()
            }
        }


        var quickReactionFragment = cfm.findFragmentByTag("QuickReaction") as? QuickReactionFragment
        if (quickReactionFragment == null) {
            quickReactionFragment = QuickReactionFragment.newInstance(arguments!!.get(MvRx.KEY_ARG) as ParcelableArgs)
            cfm.beginTransaction()
                    .replace(R.id.bottom_sheet_quick_reaction_container, quickReactionFragment, "QuickReaction")
                    .commit()
        }
        quickReactionFragment.interactionListener = object : QuickReactionFragment.InteractionListener {
            override fun didQuickReactWith(reactions: List<String>) {
                actionHandlerModel.fireAction("Quick React", reactions)
                dismiss()
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        //We want to force the bottom sheet initial state to expanded
        (dialog as? BottomSheetDialog)?.let { bottomSheetDialog ->
            bottomSheetDialog.setOnShowListener { dialog ->
                val d = dialog as BottomSheetDialog
                (d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? FrameLayout)?.let {
                    BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
        }
        return dialog
    }

    override fun invalidate() = withState(viewModel) {
        senderNameTextView.text = it.senderName
        messageBodyTextView.text = it.messageBody
        messageTimestampText.text = it.ts

        GlideApp.with(this).clear(senderAvatarImageView)
        if (it.senderAvatarPath != null) {
            GlideApp.with(this)
                    .load(it.senderAvatarPath)
                    .circleCrop()
                    .placeholder(AvatarRenderer.getPlaceholderDrawable(requireContext(), it.userId, it.senderName))
                    .into(senderAvatarImageView)
        } else {
            senderAvatarImageView.setImageDrawable(AvatarRenderer.getPlaceholderDrawable(requireContext(), it.userId, it.senderName))
        }
        return@withState
    }


    @Parcelize
    data class ParcelableArgs(
            val eventId: String,
            val roomId: String,
            val informationData: MessageInformationData
    ) : Parcelable

    companion object {
        fun newInstance(roomId: String, informationData: MessageInformationData): MessageActionsBottomSheet {
            val args = Bundle()
            val parcelableArgs = ParcelableArgs(
                    informationData.eventId,
                    roomId,
                    informationData
            )
            args.putParcelable(MvRx.KEY_ARG, parcelableArgs)
            return MessageActionsBottomSheet().apply { arguments = args }

        }
    }
}