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
package im.vector.riotx.features.home.room.detail.timeline.action

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProviders
import butterknife.BindView
import butterknife.ButterKnife
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.detail.timeline.item.MessageInformationData
import kotlinx.android.synthetic.main.bottom_sheet_message_actions.*
import javax.inject.Inject

/**
 * Bottom sheet fragment that shows a message preview with list of contextual actions
 * (Includes fragments for quick reactions and list of actions)
 */
class MessageActionsBottomSheet : VectorBaseBottomSheetDialogFragment() {

    @Inject
    lateinit var messageActionViewModelFactory: MessageActionsViewModel.Factory
    @Inject
    lateinit var avatarRenderer: AvatarRenderer
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

    override fun injectWith(screenComponent: ScreenComponent) {
        screenComponent.inject(this)
    }

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
            menuActionFragment = MessageMenuFragment.newInstance(arguments!!.get(MvRx.KEY_ARG) as TimelineEventFragmentArgs)
            cfm.beginTransaction()
                    .replace(R.id.bottom_sheet_menu_container, menuActionFragment, "MenuActionFragment")
                    .commit()
        }
        menuActionFragment.interactionListener = object : MessageMenuFragment.InteractionListener {
            override fun didSelectMenuAction(simpleAction: SimpleAction) {
                actionHandlerModel.fireAction(simpleAction)
                dismiss()
            }
        }


        var quickReactionFragment = cfm.findFragmentByTag("QuickReaction") as? QuickReactionFragment
        if (quickReactionFragment == null) {
            quickReactionFragment = QuickReactionFragment.newInstance(arguments!!.get(MvRx.KEY_ARG) as TimelineEventFragmentArgs)
            cfm.beginTransaction()
                    .replace(R.id.bottom_sheet_quick_reaction_container, quickReactionFragment, "QuickReaction")
                    .commit()
        }
        quickReactionFragment.interactionListener = object : QuickReactionFragment.InteractionListener {

            override fun didQuickReactWith(clickedOn: String, add: Boolean, eventId: String) {
                actionHandlerModel.fireAction(SimpleAction.QuickReact(eventId, clickedOn, add))
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
        val body = viewModel.resolveBody(it)
        if (body != null) {
            bottom_sheet_message_preview.isVisible = true
            senderNameTextView.text = it.senderName()
            messageBodyTextView.text = body
            messageTimestampText.text = it.time()
            avatarRenderer.render(it.informationData.avatarUrl, it.informationData.senderId, it.senderName(), senderAvatarImageView)
        } else {
            bottom_sheet_message_preview.isVisible = false
        }
        quickReactBottomDivider.isVisible = it.canReact()
        bottom_sheet_quick_reaction_container.isVisible = it.canReact()
        if (it.informationData.sendState.isSending()) {
            messageStatusInfo.isVisible = true
            messageStatusProgress.isVisible = true
            messageStatusText.text = getString(R.string.event_status_sending_message)
            messageStatusText.setCompoundDrawables(null, null, null, null)
        } else if (it.informationData.sendState.hasFailed()) {
            messageStatusInfo.isVisible = true
            messageStatusProgress.isVisible = false
            messageStatusText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_warning_small, 0, 0, 0)
            messageStatusText.text = getString(R.string.unable_to_send_message)
        } else {
            messageStatusInfo.isVisible = false
        }
        return@withState
    }


    companion object {
        fun newInstance(roomId: String, informationData: MessageInformationData): MessageActionsBottomSheet {
            return MessageActionsBottomSheet().apply {
                setArguments(
                        TimelineEventFragmentArgs(
                                informationData.eventId,
                                roomId,
                                informationData
                        )
                )
            }
        }
    }
}