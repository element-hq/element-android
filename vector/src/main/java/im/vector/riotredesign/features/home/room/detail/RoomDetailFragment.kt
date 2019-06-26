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

package im.vector.riotredesign.features.home.room.detail

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.Spannable
import android.text.TextUtils
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import com.airbnb.epoxy.EpoxyVisibilityTracker
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.ImageLoader
import com.google.android.material.snackbar.Snackbar
import com.jaiselrahman.filepicker.activity.FilePickerActivity
import com.jaiselrahman.filepicker.config.Configurations
import com.jaiselrahman.filepicker.model.MediaFile
import com.otaliastudios.autocomplete.Autocomplete
import com.otaliastudios.autocomplete.AutocompleteCallback
import com.otaliastudios.autocomplete.CharPolicy
import im.vector.matrix.android.api.permalinks.PermalinkFactory
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.EditAggregatedSummary
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.message.*
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.user.model.User
import im.vector.riotredesign.R
import im.vector.riotredesign.core.dialogs.DialogListItem
import im.vector.riotredesign.core.epoxy.LayoutManagerStateRestorer
import im.vector.riotredesign.core.extensions.hideKeyboard
import im.vector.riotredesign.core.extensions.observeEvent
import im.vector.riotredesign.core.extensions.setTextOrHide
import im.vector.riotredesign.core.glide.GlideApp
import im.vector.riotredesign.core.platform.VectorBaseFragment
import im.vector.riotredesign.core.utils.*
import im.vector.riotredesign.features.autocomplete.command.AutocompleteCommandPresenter
import im.vector.riotredesign.features.autocomplete.command.CommandAutocompletePolicy
import im.vector.riotredesign.features.autocomplete.user.AutocompleteUserPresenter
import im.vector.riotredesign.features.command.Command
import im.vector.riotredesign.features.home.*
import im.vector.riotredesign.features.home.room.detail.composer.TextComposerActions
import im.vector.riotredesign.features.home.room.detail.composer.TextComposerView
import im.vector.riotredesign.features.home.room.detail.composer.TextComposerViewModel
import im.vector.riotredesign.features.home.room.detail.composer.TextComposerViewState
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotredesign.features.home.room.detail.timeline.action.ActionsHandler
import im.vector.riotredesign.features.home.room.detail.timeline.action.MessageActionsBottomSheet
import im.vector.riotredesign.features.home.room.detail.timeline.action.MessageMenuViewModel
import im.vector.riotredesign.features.home.room.detail.timeline.action.ViewReactionBottomSheet
import im.vector.riotredesign.features.home.room.detail.timeline.helper.EndlessRecyclerViewScrollListener
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.riotredesign.features.html.PillImageSpan
import im.vector.riotredesign.features.invite.VectorInviteView
import im.vector.riotredesign.features.media.ImageContentRenderer
import im.vector.riotredesign.features.media.ImageMediaViewerActivity
import im.vector.riotredesign.features.media.VideoContentRenderer
import im.vector.riotredesign.features.media.VideoMediaViewerActivity
import im.vector.riotredesign.features.notifications.NotificationDrawerManager
import im.vector.riotredesign.features.reactions.EmojiReactionPickerActivity
import im.vector.riotredesign.features.settings.PreferencesManager
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_room_detail.*
import kotlinx.android.synthetic.main.merge_composer_layout.view.*
import org.commonmark.parser.Parser
import org.koin.android.ext.android.inject
import org.koin.android.scope.ext.android.bindScope
import org.koin.android.scope.ext.android.getOrCreateScope
import org.koin.core.parameter.parametersOf
import ru.noties.markwon.Markwon
import ru.noties.markwon.html.HtmlPlugin
import timber.log.Timber
import java.io.File


@Parcelize
data class RoomDetailArgs(
        val roomId: String,
        val eventId: String? = null
) : Parcelable


private const val CAMERA_VALUE_TITLE = "attachment"
private const val REQUEST_FILES_REQUEST_CODE = 0
private const val TAKE_IMAGE_REQUEST_CODE = 1
private const val REACTION_SELECT_REQUEST_CODE = 2

class RoomDetailFragment :
        VectorBaseFragment(),
        TimelineEventController.Callback,
        AutocompleteUserPresenter.Callback,
        VectorInviteView.Callback {

    companion object {

        fun newInstance(args: RoomDetailArgs): RoomDetailFragment {
            return RoomDetailFragment().apply {
                setArguments(args)
            }
        }

        /**
         * Sanitize the display name.
         *
         * @param displayName the display name to sanitize
         * @return the sanitized display name
         */
        fun sanitizeDisplayname(displayName: String): String? {
            // sanity checks
            if (!TextUtils.isEmpty(displayName)) {
                val ircPattern = " (IRC)"

                if (displayName.endsWith(ircPattern)) {
                    return displayName.substring(0, displayName.length - ircPattern.length)
                }
            }

            return displayName
        }
    }

    private val roomDetailArgs: RoomDetailArgs by args()
    private val session by inject<Session>()
    private val glideRequests by lazy {
        GlideApp.with(this)
    }

    private val roomDetailViewModel: RoomDetailViewModel by fragmentViewModel()
    private val textComposerViewModel: TextComposerViewModel by fragmentViewModel()
    private val timelineEventController: TimelineEventController by inject { parametersOf(this) }
    private val commandAutocompletePolicy = CommandAutocompletePolicy()
    private val autocompleteCommandPresenter: AutocompleteCommandPresenter by inject { parametersOf(this) }
    private val autocompleteUserPresenter: AutocompleteUserPresenter by inject { parametersOf(this) }
    private val permalinkHandler: PermalinkHandler by inject()

    private val notificationDrawerManager by inject<NotificationDrawerManager>()

    private lateinit var scrollOnNewMessageCallback: ScrollOnNewMessageCallback
    private lateinit var scrollOnHighlightedEventCallback: ScrollOnHighlightedEventCallback

    override fun getLayoutResId() = R.layout.fragment_room_detail

    private lateinit var actionViewModel: ActionsHandler

    @BindView(R.id.composerLayout)
    lateinit var composerLayout: TextComposerView

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        actionViewModel = ViewModelProviders.of(requireActivity()).get(ActionsHandler::class.java)
        bindScope(getOrCreateScope(HomeModule.ROOM_DETAIL_SCOPE))
        setupToolbar(roomToolbar)
        setupRecyclerView()
        setupComposer()
        setupAttachmentButton()
        setupInviteView()
        roomDetailViewModel.subscribe { renderState(it) }
        textComposerViewModel.subscribe { renderTextComposerState(it) }
        roomDetailViewModel.sendMessageResultLiveData.observeEvent(this) { renderSendMessageResult(it) }

        roomDetailViewModel.nonBlockingPopAlert.observeEvent(this) { pair ->
            val message = requireContext().getString(pair.first, *pair.second.toTypedArray())
            showSnackWithMessage(message, Snackbar.LENGTH_LONG)
        }
        actionViewModel.actionCommandEvent.observeEvent(this) {
            handleActions(it)
        }

        roomDetailViewModel.navigateToEvent.observeEvent(this) {
            //
            scrollOnHighlightedEventCallback.scheduleScrollTo(it)
        }

        roomDetailViewModel.selectSubscribe(
                RoomDetailViewState::sendMode,
                RoomDetailViewState::selectedEvent,
                RoomDetailViewState::roomId) { mode, event, roomId ->
            when (mode) {
                SendMode.REGULAR -> {
                    commandAutocompletePolicy.enabled = true
                    val uid = session.sessionParams.credentials.userId
                    val meMember = session.getRoom(roomId)?.getRoomMember(uid)
                    AvatarRenderer.render(meMember?.avatarUrl, uid, meMember?.displayName, composerLayout.composerAvatarImageView)
                    composerLayout.collapse()
                }
                SendMode.EDIT,
                SendMode.QUOTE,
                SendMode.REPLY   -> {
                    commandAutocompletePolicy.enabled = false
                    if (event == null) {
                        //we should ignore? can this happen?
                        Timber.e("Enter edit mode with no event selected")
                        return@selectSubscribe
                    }
                    //switch to expanded bar
                    composerLayout.composerRelatedMessageTitle.apply {
                        text = event.getDisambiguatedDisplayName()
                        setTextColor(ContextCompat.getColor(requireContext(), getColorFromUserId(event.root.senderId)))
                    }

                    //TODO this is used at several places, find way to refactor?
                    val messageContent: MessageContent? =
                            event.annotations?.editSummary?.aggregatedContent?.toModel()
                                    ?: event.root.content.toModel()
                    val nonFormattedBody = messageContent?.body ?: ""
                    var formattedBody: CharSequence? = null
                    if (messageContent is MessageTextContent && messageContent.format == MessageType.FORMAT_MATRIX_HTML) {
                        val parser = Parser.builder().build()
                        val document = parser.parse(messageContent.formattedBody
                                ?: messageContent.body)
                        formattedBody = Markwon.builder(requireContext())
                                .usePlugin(HtmlPlugin.create()).build().render(document)
                    }
                    composerLayout.composerRelatedMessageContent.text = formattedBody
                            ?: nonFormattedBody


                    if (mode == SendMode.EDIT) {
                        //TODO if it's a reply we should trim the top part of message
                        composerLayout.composerEditText.setText(nonFormattedBody)
                        composerLayout.composerRelatedMessageActionIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit))
                    } else if (mode == SendMode.QUOTE) {
                        composerLayout.composerEditText.setText("")
                        composerLayout.composerRelatedMessageActionIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_quote))
                    } else if (mode == SendMode.REPLY) {
                        composerLayout.composerEditText.setText("")
                        composerLayout.composerRelatedMessageActionIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_reply))
                    }

                    AvatarRenderer.render(event.senderAvatar, event.root.senderId
                            ?: "", event.senderName, composerLayout.composerRelatedMessageAvatar)

                    composerLayout.composerEditText.setSelection(composerLayout.composerEditText.text.length)
                    composerLayout.expand {
                        focusComposerAndShowKeyboard()
                    }
                    composerLayout.composerRelatedMessageCloseButton.setOnClickListener {
                        composerLayout.composerEditText.setText("")
                        roomDetailViewModel.resetSendMode()
                    }

                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        notificationDrawerManager.setCurrentRoom(roomDetailArgs.roomId)
    }

    override fun onPause() {
        super.onPause()

        notificationDrawerManager.setCurrentRoom(null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_FILES_REQUEST_CODE, TAKE_IMAGE_REQUEST_CODE -> handleMediaIntent(data)
                REACTION_SELECT_REQUEST_CODE                        -> {
                    val eventId = data.getStringExtra(EmojiReactionPickerActivity.EXTRA_EVENT_ID)
                            ?: return
                    val reaction = data.getStringExtra(EmojiReactionPickerActivity.EXTRA_REACTION_RESULT)
                            ?: return
                    //TODO check if already reacted with that?
                    roomDetailViewModel.process(RoomDetailActions.SendReaction(reaction, eventId))
                }
            }
        }
    }

// PRIVATE METHODS *****************************************************************************

    private fun setupRecyclerView() {
        val epoxyVisibilityTracker = EpoxyVisibilityTracker()
        epoxyVisibilityTracker.attach(recyclerView)
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
        val stateRestorer = LayoutManagerStateRestorer(layoutManager).register()
        scrollOnNewMessageCallback = ScrollOnNewMessageCallback(layoutManager)
        scrollOnHighlightedEventCallback = ScrollOnHighlightedEventCallback(layoutManager, timelineEventController)
        recyclerView.layoutManager = layoutManager
        recyclerView.itemAnimator = null
        recyclerView.setHasFixedSize(true)
        timelineEventController.addModelBuildListener {
            it.dispatchTo(stateRestorer)
            it.dispatchTo(scrollOnNewMessageCallback)
            it.dispatchTo(scrollOnHighlightedEventCallback)
        }

        recyclerView.addOnScrollListener(
                EndlessRecyclerViewScrollListener(layoutManager, RoomDetailViewModel.PAGINATION_COUNT) { direction ->
                    roomDetailViewModel.process(RoomDetailActions.LoadMore(direction))
                })
        recyclerView.setController(timelineEventController)
        timelineEventController.callback = this
    }

    private fun setupComposer() {
        val elevation = 6f
        val backgroundDrawable = ColorDrawable(Color.WHITE)
        Autocomplete.on<Command>(composerLayout.composerEditText)
                .with(commandAutocompletePolicy)
                .with(autocompleteCommandPresenter)
                .with(elevation)
                .with(backgroundDrawable)
                .with(object : AutocompleteCallback<Command> {
                    override fun onPopupItemClicked(editable: Editable, item: Command): Boolean {
                        editable.clear()
                        editable
                                .append(item.command)
                                .append(" ")
                        return true
                    }

                    override fun onPopupVisibilityChanged(shown: Boolean) {
                    }
                })
                .build()

        autocompleteUserPresenter.callback = this
        Autocomplete.on<User>(composerLayout.composerEditText)
                .with(CharPolicy('@', true))
                .with(autocompleteUserPresenter)
                .with(elevation)
                .with(backgroundDrawable)
                .with(object : AutocompleteCallback<User> {
                    override fun onPopupItemClicked(editable: Editable, item: User): Boolean {
                        // Detect last '@' and remove it
                        var startIndex = editable.lastIndexOf("@")
                        if (startIndex == -1) {
                            startIndex = 0
                        }

                        // Detect next word separator
                        var endIndex = editable.indexOf(" ", startIndex)
                        if (endIndex == -1) {
                            endIndex = editable.length
                        }

                        // Replace the word by its completion
                        val displayName = item.displayName ?: item.userId

                        // with a trailing space
                        editable.replace(startIndex, endIndex, "$displayName ")

                        // Add the span
                        val user = session.getUser(item.userId)
                        val span = PillImageSpan(glideRequests, context!!, item.userId, user)
                        span.bind(composerLayout.composerEditText)

                        editable.setSpan(span, startIndex, startIndex + displayName.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        return true
                    }

                    override fun onPopupVisibilityChanged(shown: Boolean) {
                    }
                })
                .build()

        composerLayout.sendButton.setOnClickListener {
            val textMessage = composerLayout.composerEditText.text.toString()
            if (textMessage.isNotBlank()) {
                roomDetailViewModel.process(RoomDetailActions.SendMessage(textMessage, PreferencesManager.isMarkdownEnabled(requireContext())))
            }
        }
    }

    private fun setupAttachmentButton() {
        composerLayout.attachmentButton.setOnClickListener {
            val intent = Intent(requireContext(), FilePickerActivity::class.java)
            intent.putExtra(FilePickerActivity.CONFIGS, Configurations.Builder()
                    .setCheckPermission(true)
                    .setShowFiles(true)
                    .setShowAudios(true)
                    .setSkipZeroSizeFiles(true)
                    .build())
            startActivityForResult(intent, REQUEST_FILES_REQUEST_CODE)
            /*
            val items = ArrayList<DialogListItem>()
            // Send file
            items.add(DialogListItem.SendFile)
            // Send voice

            if (PreferencesManager.isSendVoiceFeatureEnabled(this)) {
                items.add(DialogListItem.SendVoice.INSTANCE)
            }


            // Send sticker
            //items.add(DialogListItem.SendSticker)
            // Camera

            //if (PreferencesManager.useNativeCamera(this)) {
            items.add(DialogListItem.TakePhoto)
            items.add(DialogListItem.TakeVideo)
            //} else {
    //                items.add(DialogListItem.TakePhotoVideo.INSTANCE)
            //          }
            val adapter = DialogSendItemAdapter(requireContext(), items)
            AlertDialog.Builder(requireContext())
                    .setAdapter(adapter) { _, position ->
                        onSendChoiceClicked(items[position])
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
                    */
        }
    }

    private fun setupInviteView() {
        inviteView.callback = this
    }

    private fun onSendChoiceClicked(dialogListItem: DialogListItem) {
        Timber.v("On send choice clicked: $dialogListItem")
        when (dialogListItem) {
            is DialogListItem.SendFile       -> {
                // launchFileIntent
            }
            is DialogListItem.SendVoice      -> {
                //launchAudioRecorderIntent()
            }
            is DialogListItem.SendSticker    -> {
                //startStickerPickerActivity()
            }
            is DialogListItem.TakePhotoVideo ->
                if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, requireActivity(), PERMISSION_REQUEST_CODE_LAUNCH_CAMERA)) {
                    //    launchCamera()
                }
            is DialogListItem.TakePhoto      ->
                if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, requireActivity(), PERMISSION_REQUEST_CODE_LAUNCH_NATIVE_CAMERA)) {
                    openCamera(requireActivity(), CAMERA_VALUE_TITLE, TAKE_IMAGE_REQUEST_CODE)
                }
            is DialogListItem.TakeVideo      ->
                if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, requireActivity(), PERMISSION_REQUEST_CODE_LAUNCH_NATIVE_VIDEO_CAMERA)) {
                    //  launchNativeVideoRecorder()
                }
        }
    }

    private fun handleMediaIntent(data: Intent) {
        val files: ArrayList<MediaFile> = data.getParcelableArrayListExtra(FilePickerActivity.MEDIA_FILES)
        roomDetailViewModel.process(RoomDetailActions.SendMedia(files))
    }

    private fun renderState(state: RoomDetailViewState) {
        renderRoomSummary(state)
        val summary = state.asyncRoomSummary()
        val inviter = state.asyncInviter()
        if (summary?.membership == Membership.JOIN) {
            timelineEventController.setTimeline(state.timeline, state.eventId)
            inviteView.visibility = View.GONE

            val uid = session.sessionParams.credentials.userId
            val meMember = session.getRoom(state.roomId)?.getRoomMember(uid)
            AvatarRenderer.render(meMember?.avatarUrl, uid, meMember?.displayName, composerLayout.composerAvatarImageView)

        } else if (summary?.membership == Membership.INVITE && inviter != null) {
            inviteView.visibility = View.VISIBLE
            inviteView.render(inviter, VectorInviteView.Mode.LARGE)
        } else if (state.asyncInviter.complete) {
            vectorBaseActivity.finish()
        }
    }

    private fun renderRoomSummary(state: RoomDetailViewState) {
        state.asyncRoomSummary()?.let {
            roomToolbarTitleView.text = it.displayName
            AvatarRenderer.render(it, roomToolbarAvatarImageView)
            roomToolbarSubtitleView.setTextOrHide(it.topic)
        }
    }

    private fun renderTextComposerState(state: TextComposerViewState) {
        autocompleteUserPresenter.render(state.asyncUsers)
    }

    private fun renderSendMessageResult(sendMessageResult: SendMessageResult) {
        when (sendMessageResult) {
            is SendMessageResult.MessageSent,
            is SendMessageResult.SlashCommandHandled        -> {
                // Clear composer
                composerLayout.composerEditText.text = null
            }
            is SendMessageResult.SlashCommandError          -> {
                displayCommandError(getString(R.string.command_problem_with_parameters, sendMessageResult.command.command))
            }
            is SendMessageResult.SlashCommandUnknown        -> {
                displayCommandError(getString(R.string.unrecognized_command, sendMessageResult.command))
            }
            is SendMessageResult.SlashCommandResultOk       -> {
                // Ignore
            }
            is SendMessageResult.SlashCommandResultError    -> {
                displayCommandError(sendMessageResult.throwable.localizedMessage)
            }
            is SendMessageResult.SlashCommandNotImplemented -> {
                displayCommandError(getString(R.string.not_implemented))
            }
        }
    }

    private fun displayCommandError(message: String) {
        AlertDialog.Builder(activity!!)
                .setTitle(R.string.command_error)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    // TimelineEventController.Callback ************************************************************

    override fun onUrlClicked(url: String): Boolean {
        return permalinkHandler.launch(requireActivity(), url, object : NavigateToRoomInterceptor {
            override fun navToRoom(roomId: String, eventId: String?): Boolean {
                // Same room?
                if (roomId == roomDetailArgs.roomId) {
                    // Navigation to same room
                    if (eventId == null) {
                        showSnackWithMessage(getString(R.string.navigate_to_room_when_already_in_the_room))
                    } else {
                        // Highlight and scroll to this event
                        roomDetailViewModel.process(RoomDetailActions.NavigateToEvent(eventId, timelineEventController.searchPositionOfEvent(eventId)))
                    }
                    return true
                }

                // Not handled
                return false
            }
        })
    }

    override fun onUrlLongClicked(url: String): Boolean {
        // Copy the url to the clipboard
        copyToClipboard(requireContext(), url)
        return true
    }

    override fun onEventVisible(event: TimelineEvent) {
        roomDetailViewModel.process(RoomDetailActions.EventDisplayed(event))
    }

    override fun onEncryptedMessageClicked(informationData: MessageInformationData, view: View) {
        vectorBaseActivity.notImplemented("encrypted message click")
    }

    override fun onImageMessageClicked(messageImageContent: MessageImageContent, mediaData: ImageContentRenderer.Data, view: View) {
        // TODO Use navigator
        val intent = ImageMediaViewerActivity.newIntent(vectorBaseActivity, mediaData)
        startActivity(intent)
    }

    override fun onVideoMessageClicked(messageVideoContent: MessageVideoContent, mediaData: VideoContentRenderer.Data, view: View) {
        // TODO Use navigator
        val intent = VideoMediaViewerActivity.newIntent(vectorBaseActivity, mediaData)
        startActivity(intent)
    }

    override fun onFileMessageClicked(messageFileContent: MessageFileContent) {
        vectorBaseActivity.notImplemented()
    }

    override fun onAudioMessageClicked(messageAudioContent: MessageAudioContent) {
        vectorBaseActivity.notImplemented()
    }

    override fun onEventCellClicked(informationData: MessageInformationData, messageContent: MessageContent?, view: View) {

    }

    override fun onEventLongClicked(informationData: MessageInformationData, messageContent: MessageContent?, view: View): Boolean {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        val roomId = roomDetailArgs.roomId

        this.view?.hideKeyboard()
        MessageActionsBottomSheet
                .newInstance(roomId, informationData)
                .show(requireActivity().supportFragmentManager, "MESSAGE_CONTEXTUAL_ACTIONS")
        return true
    }

    override fun onAvatarClicked(informationData: MessageInformationData) {
        vectorBaseActivity.notImplemented("Click on user avatar")
    }

    @SuppressLint("SetTextI18n")
    override fun onMemberNameClicked(informationData: MessageInformationData) {
        insertUserDisplayNameInTextEditor(informationData.memberName?.toString())
    }

    override fun onClickOnReactionPill(informationData: MessageInformationData, reaction: String, on: Boolean) {
        if (on) {
            //we should test the current real state of reaction on this event
            roomDetailViewModel.process(RoomDetailActions.SendReaction(reaction, informationData.eventId))
        } else {
            //I need to redact a reaction
            roomDetailViewModel.process(RoomDetailActions.UndoReaction(informationData.eventId, reaction))
        }
    }

    override fun onLongClickOnReactionPill(informationData: MessageInformationData, reaction: String) {
        ViewReactionBottomSheet.newInstance(roomDetailArgs.roomId, informationData)
                .show(requireActivity().supportFragmentManager, "DISPLAY_REACTIONS")
    }

    override fun onEditedDecorationClicked(informationData: MessageInformationData, editAggregatedSummary: EditAggregatedSummary?) {
        editAggregatedSummary?.also {
            roomDetailViewModel.process(RoomDetailActions.ShowEditHistoryAction(informationData.eventId, it))
        }

    }
// AutocompleteUserPresenter.Callback

    override fun onQueryUsers(query: CharSequence?) {
        textComposerViewModel.process(TextComposerActions.QueryUsers(query))
    }

    private fun handleActions(actionData: ActionsHandler.ActionData) {
        when (actionData.actionId) {
            MessageMenuViewModel.ACTION_ADD_REACTION   -> {
                val eventId = actionData.data?.toString() ?: return
                startActivityForResult(EmojiReactionPickerActivity.intent(requireContext(), eventId), REACTION_SELECT_REQUEST_CODE)
            }
            MessageMenuViewModel.ACTION_VIEW_REACTIONS -> {
                val messageInformationData = actionData.data as? MessageInformationData
                        ?: return
                ViewReactionBottomSheet.newInstance(roomDetailArgs.roomId, messageInformationData)
                        .show(requireActivity().supportFragmentManager, "DISPLAY_REACTIONS")
            }
            MessageMenuViewModel.ACTION_COPY           -> {
                //I need info about the current selected message :/
                copyToClipboard(requireContext(), actionData.data?.toString() ?: "", false)
                val msg = requireContext().getString(R.string.copied_to_clipboard)
                showSnackWithMessage(msg, Snackbar.LENGTH_SHORT)
            }
            MessageMenuViewModel.ACTION_DELETE         -> {
                val eventId = actionData.data?.toString() ?: return
                roomDetailViewModel.process(RoomDetailActions.RedactAction(eventId, context?.getString(R.string.event_redacted_by_user_reason)))
            }
            MessageMenuViewModel.ACTION_SHARE          -> {
                //TODO current data communication is too limited
                //Need to now the media type
                actionData.data?.toString()?.let {
                    //TODO bad, just POC
                    BigImageViewer.imageLoader().loadImage(
                            actionData.hashCode(),
                            Uri.parse(it),
                            object : ImageLoader.Callback {
                                override fun onFinish() {}

                                override fun onSuccess(image: File?) {
                                    if (image != null)
                                        shareMedia(requireContext(), image, "image/*")
                                }

                                override fun onFail(error: Exception?) {}

                                override fun onCacheHit(imageType: Int, image: File?) {}

                                override fun onCacheMiss(imageType: Int, image: File?) {}

                                override fun onProgress(progress: Int) {}

                                override fun onStart() {}

                            }

                    )
                }
            }
            MessageMenuViewModel.VIEW_SOURCE,
            MessageMenuViewModel.VIEW_DECRYPTED_SOURCE -> {
                val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_event_content, null)
                view.findViewById<TextView>(R.id.event_content_text_view)?.let {
                    it.text = actionData.data?.toString() ?: ""
                }

                AlertDialog.Builder(requireActivity())
                        .setView(view)
                        .setPositiveButton(R.string.ok) { dialog, id -> dialog.cancel() }
                        .show()
            }
            MessageMenuViewModel.ACTION_QUICK_REACT    -> {
                //eventId,ClickedOn,Add
                (actionData.data as? Triple<String, String, Boolean>)?.let { (eventId, clickedOn, add) ->
                    roomDetailViewModel.process(RoomDetailActions.UpdateQuickReactAction(eventId, clickedOn, add))
                }
            }
            MessageMenuViewModel.ACTION_EDIT           -> {
                val eventId = actionData.data.toString()
                roomDetailViewModel.process(RoomDetailActions.EnterEditMode(eventId))
            }
            MessageMenuViewModel.ACTION_QUOTE          -> {
                val eventId = actionData.data.toString()
                roomDetailViewModel.process(RoomDetailActions.EnterQuoteMode(eventId))
            }
            MessageMenuViewModel.ACTION_REPLY          -> {
                val eventId = actionData.data.toString()
                roomDetailViewModel.process(RoomDetailActions.EnterReplyMode(eventId))
            }
            MessageMenuViewModel.ACTION_COPY_PERMALINK -> {
                val eventId = actionData.data.toString()
                val permalink = PermalinkFactory.createPermalink(roomDetailArgs.roomId, eventId)
                copyToClipboard(requireContext(), permalink, false)
                showSnackWithMessage(requireContext().getString(R.string.copied_to_clipboard), Snackbar.LENGTH_SHORT)

            }
            else                                       -> {
                Toast.makeText(context, "Action ${actionData.actionId} not implemented", Toast.LENGTH_LONG).show()
            }
        }
    }

    //utils
    /**
     * Insert an user displayname  in the message editor.
     *
     * @param text the text to insert.
     */
    //TODO legacy, refactor
    private fun insertUserDisplayNameInTextEditor(text: String?) {
        //TODO move logic outside of fragment
        if (null != text) {
//            var vibrate = false

            val myDisplayName = session.getUser(session.sessionParams.credentials.userId)?.displayName
            if (TextUtils.equals(myDisplayName, text)) {
                // current user
                if (TextUtils.isEmpty(composerLayout.composerEditText.text)) {
                    composerLayout.composerEditText.append(Command.EMOTE.command + " ")
                    composerLayout.composerEditText.setSelection(composerLayout.composerEditText.text.length)
//                    vibrate = true
                }
            } else {
                // another user
                if (TextUtils.isEmpty(composerLayout.composerEditText.text)) {
                    // Ensure displayName will not be interpreted as a Slash command
                    if (text.startsWith("/")) {
                        composerLayout.composerEditText.append("\\")
                    }
                    composerLayout.composerEditText.append(sanitizeDisplayname(text)!! + ": ")
                } else {
                    composerLayout.composerEditText.text.insert(composerLayout.composerEditText.selectionStart, sanitizeDisplayname(text)!! + " ")
                }

//                vibrate = true
            }

//            if (vibrate && PreferencesManager.vibrateWhenMentioning(context)) {
//                val v= context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
//                if (v?.hasVibrator() == true) {
//                    v.vibrate(100)
//                }
//            }
            focusComposerAndShowKeyboard()
        }
    }

    private fun focusComposerAndShowKeyboard() {
        composerLayout.composerEditText.requestFocus()
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(composerLayout.composerEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun showSnackWithMessage(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        val snack = Snackbar.make(view!!, message, duration)
        snack.view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.notification_accent_color))
        snack.show()
    }

    // VectorInviteView.Callback

    override fun onAcceptInvite() {
        roomDetailViewModel.process(RoomDetailActions.AcceptInvite)
    }

    override fun onRejectInvite() {
        roomDetailViewModel.process(RoomDetailActions.RejectInvite)
    }
}
