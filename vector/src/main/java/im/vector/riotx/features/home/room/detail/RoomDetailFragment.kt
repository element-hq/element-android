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

package im.vector.riotx.features.home.room.detail

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.Spannable
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.EpoxyVisibilityTracker
import com.airbnb.epoxy.OnModelBuildFinishedListener
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.ImageLoader
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.matrix.android.api.permalinks.PermalinkFactory
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.message.MessageAudioContent
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageFileContent
import im.vector.matrix.android.api.session.room.model.message.MessageFormat
import im.vector.matrix.android.api.session.room.model.message.MessageImageInfoContent
import im.vector.matrix.android.api.session.room.model.message.MessageTextContent
import im.vector.matrix.android.api.session.room.model.message.MessageVerificationRequestContent
import im.vector.matrix.android.api.session.room.model.message.MessageVideoContent
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.getLastMessageContent
import im.vector.matrix.android.api.util.MatrixItem
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.riotx.R
import im.vector.riotx.core.dialogs.withColoredButton
import im.vector.riotx.core.epoxy.LayoutManagerStateRestorer
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.extensions.hideKeyboard
import im.vector.riotx.core.extensions.setTextOrHide
import im.vector.riotx.core.extensions.showKeyboard
import im.vector.riotx.core.files.addEntryToDownloadManager
import im.vector.riotx.core.glide.GlideApp
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.core.ui.views.JumpToReadMarkerView
import im.vector.riotx.core.ui.views.NotificationAreaView
import im.vector.riotx.core.utils.Debouncer
import im.vector.riotx.core.utils.KeyboardStateUtils
import im.vector.riotx.core.utils.PERMISSIONS_FOR_WRITING_FILES
import im.vector.riotx.core.utils.PERMISSION_REQUEST_CODE_DOWNLOAD_FILE
import im.vector.riotx.core.utils.PERMISSION_REQUEST_CODE_INCOMING_URI
import im.vector.riotx.core.utils.PERMISSION_REQUEST_CODE_PICK_ATTACHMENT
import im.vector.riotx.core.utils.TextUtils
import im.vector.riotx.core.utils.allGranted
import im.vector.riotx.core.utils.checkPermissions
import im.vector.riotx.core.utils.copyToClipboard
import im.vector.riotx.core.utils.createUIHandler
import im.vector.riotx.core.utils.getColorFromUserId
import im.vector.riotx.core.utils.openUrlInExternalBrowser
import im.vector.riotx.core.utils.shareMedia
import im.vector.riotx.core.utils.toast
import im.vector.riotx.features.attachments.AttachmentTypeSelectorView
import im.vector.riotx.features.attachments.AttachmentsHelper
import im.vector.riotx.features.attachments.ContactAttachment
import im.vector.riotx.features.command.Command
import im.vector.riotx.features.crypto.util.toImageRes
import im.vector.riotx.features.crypto.verification.VerificationBottomSheet
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.detail.composer.TextComposerView
import im.vector.riotx.features.home.room.detail.readreceipts.DisplayReadReceiptsBottomSheet
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotx.features.home.room.detail.timeline.action.EventSharedAction
import im.vector.riotx.features.home.room.detail.timeline.action.MessageActionsBottomSheet
import im.vector.riotx.features.home.room.detail.timeline.action.MessageSharedActionViewModel
import im.vector.riotx.features.home.room.detail.timeline.edithistory.ViewEditHistoryBottomSheet
import im.vector.riotx.features.home.room.detail.timeline.item.AbsMessageItem
import im.vector.riotx.features.home.room.detail.timeline.item.MessageFileItem
import im.vector.riotx.features.home.room.detail.timeline.item.MessageImageVideoItem
import im.vector.riotx.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.riotx.features.home.room.detail.timeline.item.MessageTextItem
import im.vector.riotx.features.home.room.detail.timeline.item.ReadReceiptData
import im.vector.riotx.features.home.room.detail.timeline.reactions.ViewReactionsBottomSheet
import im.vector.riotx.features.html.EventHtmlRenderer
import im.vector.riotx.features.html.PillImageSpan
import im.vector.riotx.features.invite.VectorInviteView
import im.vector.riotx.features.media.ImageContentRenderer
import im.vector.riotx.features.media.ImageMediaViewerActivity
import im.vector.riotx.features.media.VideoContentRenderer
import im.vector.riotx.features.media.VideoMediaViewerActivity
import im.vector.riotx.features.notifications.NotificationDrawerManager
import im.vector.riotx.features.permalink.NavigationInterceptor
import im.vector.riotx.features.permalink.PermalinkHandler
import im.vector.riotx.features.reactions.EmojiReactionPickerActivity
import im.vector.riotx.features.settings.VectorPreferences
import im.vector.riotx.features.share.SharedData
import im.vector.riotx.features.themes.ThemeUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_room_detail.*
import kotlinx.android.synthetic.main.merge_composer_layout.view.*
import kotlinx.android.synthetic.main.merge_overlay_waiting_view.*
import org.commonmark.parser.Parser
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Parcelize
data class RoomDetailArgs(
        val roomId: String,
        val eventId: String? = null,
        val sharedData: SharedData? = null
) : Parcelable

private const val REACTION_SELECT_REQUEST_CODE = 0

class RoomDetailFragment @Inject constructor(
        private val session: Session,
        private val avatarRenderer: AvatarRenderer,
        private val timelineEventController: TimelineEventController,
        autoCompleterFactory: AutoCompleter.Factory,
        private val permalinkHandler: PermalinkHandler,
        private val notificationDrawerManager: NotificationDrawerManager,
        val roomDetailViewModelFactory: RoomDetailViewModel.Factory,
        private val eventHtmlRenderer: EventHtmlRenderer,
        private val vectorPreferences: VectorPreferences
) :
        VectorBaseFragment(),
        TimelineEventController.Callback,
        VectorInviteView.Callback,
        JumpToReadMarkerView.Callback,
        AttachmentTypeSelectorView.Callback,
        AttachmentsHelper.Callback {

    companion object {

        /**
         * Sanitize the display name.
         *
         * @param displayName the display name to sanitize
         * @return the sanitized display name
         */
        private fun sanitizeDisplayName(displayName: String): String {
            if (displayName.endsWith(ircPattern)) {
                return displayName.substring(0, displayName.length - ircPattern.length)
            }

            return displayName
        }

        private const val ircPattern = " (IRC)"
    }

    private val roomDetailArgs: RoomDetailArgs by args()
    private val glideRequests by lazy {
        GlideApp.with(this)
    }

    private val autoCompleter: AutoCompleter by lazy {
        autoCompleterFactory.create(roomDetailArgs.roomId)
    }
    private val roomDetailViewModel: RoomDetailViewModel by fragmentViewModel()
    private val debouncer = Debouncer(createUIHandler())

    private lateinit var scrollOnNewMessageCallback: ScrollOnNewMessageCallback
    private lateinit var scrollOnHighlightedEventCallback: ScrollOnHighlightedEventCallback

    override fun getLayoutResId() = R.layout.fragment_room_detail

    override fun getMenuRes() = R.menu.menu_timeline

    private lateinit var sharedActionViewModel: MessageSharedActionViewModel
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var jumpToBottomViewVisibilityManager: JumpToBottomViewVisibilityManager
    private var modelBuildListener: OnModelBuildFinishedListener? = null

    private lateinit var attachmentsHelper: AttachmentsHelper
    private lateinit var keyboardStateUtils: KeyboardStateUtils

    @BindView(R.id.composerLayout)
    lateinit var composerLayout: TextComposerView
    private lateinit var attachmentTypeSelector: AttachmentTypeSelectorView

    private var lockSendButton = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(MessageSharedActionViewModel::class.java)
        attachmentsHelper = AttachmentsHelper.create(this, this).register()
        keyboardStateUtils = KeyboardStateUtils(requireActivity())
        setupToolbar(roomToolbar)
        setupRecyclerView()
        setupComposer()
        setupInviteView()
        setupNotificationView()
        setupJumpToReadMarkerView()
        setupJumpToBottomView()
        roomToolbarContentView.setOnClickListener {
            navigator.openRoomProfile(requireActivity(), roomDetailArgs.roomId)
        }
        roomDetailViewModel.subscribe { renderState(it) }

        sharedActionViewModel
                .observe()
                .subscribe {
                    handleActions(it)
                }
                .disposeOnDestroyView()

        roomDetailViewModel.selectSubscribe(this, RoomDetailViewState::tombstoneEventHandling, uniqueOnly("tombstoneEventHandling")) {
            renderTombstoneEventHandling(it)
        }

        roomDetailViewModel.selectSubscribe(RoomDetailViewState::sendMode) { mode ->
            when (mode) {
                is SendMode.REGULAR -> renderRegularMode(mode.text)
                is SendMode.EDIT    -> renderSpecialMode(mode.timelineEvent, R.drawable.ic_edit, R.string.edit, mode.text)
                is SendMode.QUOTE   -> renderSpecialMode(mode.timelineEvent, R.drawable.ic_quote, R.string.quote, mode.text)
                is SendMode.REPLY   -> renderSpecialMode(mode.timelineEvent, R.drawable.ic_reply, R.string.reply, mode.text)
            }
        }

        roomDetailViewModel.selectSubscribe(RoomDetailViewState::syncState) { syncState ->
            syncStateView.render(syncState)
        }

        roomDetailViewModel.observeViewEvents {
            when (it) {
                is RoomDetailViewEvents.Failure             -> showErrorInSnackbar(it.throwable)
                is RoomDetailViewEvents.OnNewTimelineEvents -> scrollOnNewMessageCallback.addNewTimelineEventIds(it.eventIds)
                is RoomDetailViewEvents.ActionSuccess       -> displayRoomDetailActionSuccess(it)
                is RoomDetailViewEvents.ActionFailure       -> displayRoomDetailActionFailure(it)
                is RoomDetailViewEvents.ShowMessage         -> showSnackWithMessage(it.message, Snackbar.LENGTH_LONG)
                is RoomDetailViewEvents.NavigateToEvent     -> navigateToEvent(it)
                is RoomDetailViewEvents.FileTooBigError     -> displayFileTooBigError(it)
                is RoomDetailViewEvents.DownloadFileState   -> handleDownloadFileState(it)
                is RoomDetailViewEvents.SendMessageResult   -> renderSendMessageResult(it)
            }.exhaustive
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState == null) {
            when (val sharedData = roomDetailArgs.sharedData) {
                is SharedData.Text        -> roomDetailViewModel.handle(RoomDetailAction.SendMessage(sharedData.text, false))
                is SharedData.Attachments -> roomDetailViewModel.handle(RoomDetailAction.SendMedia(sharedData.attachmentData))
                null                      -> Timber.v("No share data to process")
            }
        }
    }

    override fun onDestroyView() {
        timelineEventController.callback = null
        timelineEventController.removeModelBuildListener(modelBuildListener)
        modelBuildListener = null
        debouncer.cancelAll()
        recyclerView.cleanup()
        super.onDestroyView()
    }

    override fun onDestroy() {
        roomDetailViewModel.handle(RoomDetailAction.ExitTrackingUnreadMessagesState)
        super.onDestroy()
    }

    private fun setupJumpToBottomView() {
        jumpToBottomView.visibility = View.INVISIBLE
        jumpToBottomView.setOnClickListener {
            roomDetailViewModel.handle(RoomDetailAction.ExitTrackingUnreadMessagesState)
            jumpToBottomView.visibility = View.INVISIBLE
            if (!roomDetailViewModel.timeline.isLive) {
                scrollOnNewMessageCallback.forceScrollOnNextUpdate()
                roomDetailViewModel.timeline.restartWithEventId(null)
            } else {
                layoutManager.scrollToPosition(0)
            }
        }

        jumpToBottomViewVisibilityManager = JumpToBottomViewVisibilityManager(
                jumpToBottomView,
                debouncer,
                recyclerView,
                layoutManager
        )
    }

    private fun setupJumpToReadMarkerView() {
        jumpToReadMarkerView.callback = this
    }

    private fun navigateToEvent(action: RoomDetailViewEvents.NavigateToEvent) {
        val scrollPosition = timelineEventController.searchPositionOfEvent(action.eventId)
        if (scrollPosition == null) {
            scrollOnHighlightedEventCallback.scheduleScrollTo(action.eventId)
        } else {
            recyclerView.stopScroll()
            layoutManager.scrollToPosition(scrollPosition)
        }
    }

    private fun displayFileTooBigError(action: RoomDetailViewEvents.FileTooBigError) {
        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.dialog_title_error)
                .setMessage(getString(R.string.error_file_too_big,
                        action.filename,
                        TextUtils.formatFileSize(requireContext(), action.fileSizeInBytes),
                        TextUtils.formatFileSize(requireContext(), action.homeServerLimitInBytes)
                ))
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    private fun handleDownloadFileState(action: RoomDetailViewEvents.DownloadFileState) {
        val activity = requireActivity()
        if (action.throwable != null) {
            activity.toast(errorFormatter.toHumanReadable(action.throwable))
        } else if (action.file != null) {
            activity.toast(getString(R.string.downloaded_file, action.file.path))
            addEntryToDownloadManager(activity, action.file, action.mimeType)
        }
    }

    private fun setupNotificationView() {
        notificationAreaView.delegate = object : NotificationAreaView.Delegate {
            override fun onTombstoneEventClicked(tombstoneEvent: Event) {
                roomDetailViewModel.handle(RoomDetailAction.HandleTombstoneEvent(tombstoneEvent))
            }

            override fun resendUnsentEvents() {
                vectorBaseActivity.notImplemented()
            }

            override fun deleteUnsentEvents() {
                vectorBaseActivity.notImplemented()
            }

            override fun closeScreen() {
                vectorBaseActivity.notImplemented()
            }

            override fun jumpToBottom() {
                vectorBaseActivity.notImplemented()
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.forEach {
            it.isVisible = roomDetailViewModel.isMenuItemVisible(it.itemId)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.clear_message_queue) {
            // This a temporary option during dev as it is not super stable
            // Cancel all pending actions in room queue and post a dummy
            // Then mark all sending events as undelivered
            roomDetailViewModel.handle(RoomDetailAction.ClearSendQueue)
            return true
        }
        if (item.itemId == R.id.resend_all) {
            roomDetailViewModel.handle(RoomDetailAction.ResendAll)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun renderRegularMode(text: String) {
        autoCompleter.exitSpecialMode()
        composerLayout.collapse()

        updateComposerText(text)
        composerLayout.sendButton.setContentDescription(getString(R.string.send))
    }

    private fun renderSpecialMode(event: TimelineEvent,
                                  @DrawableRes iconRes: Int,
                                  @StringRes descriptionRes: Int,
                                  defaultContent: String) {
        autoCompleter.enterSpecialMode()
        // switch to expanded bar
        composerLayout.composerRelatedMessageTitle.apply {
            text = event.getDisambiguatedDisplayName()
            setTextColor(ContextCompat.getColor(requireContext(), getColorFromUserId(event.root.senderId)))
        }

        val messageContent: MessageContent? = event.getLastMessageContent()
        val nonFormattedBody = messageContent?.body ?: ""
        var formattedBody: CharSequence? = null
        if (messageContent is MessageTextContent && messageContent.format == MessageFormat.FORMAT_MATRIX_HTML) {
            val parser = Parser.builder().build()
            val document = parser.parse(messageContent.formattedBody ?: messageContent.body)
            formattedBody = eventHtmlRenderer.render(document)
        }
        composerLayout.composerRelatedMessageContent.text = formattedBody ?: nonFormattedBody

        updateComposerText(defaultContent)

        composerLayout.composerRelatedMessageActionIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), iconRes))
        composerLayout.sendButton.contentDescription = getString(descriptionRes)

        avatarRenderer.render(
                MatrixItem.UserItem(event.root.senderId
                        ?: "", event.getDisambiguatedDisplayName(), event.senderAvatar),
                composerLayout.composerRelatedMessageAvatar
        )

        composerLayout.expand {
            if (isAdded) {
                // need to do it here also when not using quick reply
                focusComposerAndShowKeyboard()
            }
        }
        focusComposerAndShowKeyboard()
    }

    private fun updateComposerText(text: String) {
        // Do not update if this is the same text to avoid the cursor to move
        if (text != composerLayout.composerEditText.text.toString()) {
            // Ignore update to avoid saving a draft
            composerLayout.composerEditText.setText(text)
            composerLayout.composerEditText.setSelection(composerLayout.composerEditText.text?.length
                    ?: 0)
        }
    }

    override fun onResume() {
        super.onResume()
        notificationDrawerManager.setCurrentRoom(roomDetailArgs.roomId)
    }

    override fun onPause() {
        super.onPause()

        notificationDrawerManager.setCurrentRoom(null)

        roomDetailViewModel.handle(RoomDetailAction.SaveDraft(composerLayout.composerEditText.text.toString()))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val hasBeenHandled = attachmentsHelper.onActivityResult(requestCode, resultCode, data)
        if (!hasBeenHandled && resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                REACTION_SELECT_REQUEST_CODE -> {
                    val (eventId, reaction) = EmojiReactionPickerActivity.getOutput(data) ?: return
                    roomDetailViewModel.handle(RoomDetailAction.SendReaction(eventId, reaction))
                }
            }
        }
    }

// PRIVATE METHODS *****************************************************************************

    private fun setupRecyclerView() {
        timelineEventController.callback = this
        timelineEventController.timeline = roomDetailViewModel.timeline

        val epoxyVisibilityTracker = EpoxyVisibilityTracker()
        epoxyVisibilityTracker.attach(recyclerView)
        layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
        val stateRestorer = LayoutManagerStateRestorer(layoutManager).register()
        scrollOnNewMessageCallback = ScrollOnNewMessageCallback(layoutManager, timelineEventController)
        scrollOnHighlightedEventCallback = ScrollOnHighlightedEventCallback(recyclerView, layoutManager, timelineEventController)
        recyclerView.layoutManager = layoutManager
        recyclerView.itemAnimator = null
        recyclerView.setHasFixedSize(true)
        modelBuildListener = OnModelBuildFinishedListener {
            it.dispatchTo(stateRestorer)
            it.dispatchTo(scrollOnNewMessageCallback)
            it.dispatchTo(scrollOnHighlightedEventCallback)
            updateJumpToReadMarkerViewVisibility()
            jumpToBottomViewVisibilityManager.maybeShowJumpToBottomViewVisibilityWithDelay()
        }
        timelineEventController.addModelBuildListener(modelBuildListener)
        recyclerView.adapter = timelineEventController.adapter

        if (vectorPreferences.swipeToReplyIsEnabled()) {
            val quickReplyHandler = object : RoomMessageTouchHelperCallback.QuickReplayHandler {
                override fun performQuickReplyOnHolder(model: EpoxyModel<*>) {
                    (model as? AbsMessageItem)?.attributes?.informationData?.let {
                        val eventId = it.eventId
                        roomDetailViewModel.handle(RoomDetailAction.EnterReplyMode(eventId, composerLayout.composerEditText.text.toString()))
                    }
                }

                override fun canSwipeModel(model: EpoxyModel<*>): Boolean {
                    return when (model) {
                        is MessageFileItem,
                        is MessageImageVideoItem,
                        is MessageTextItem -> {
                            return (model as AbsMessageItem).attributes.informationData.sendState == SendState.SYNCED
                        }
                        else               -> false
                    }
                }
            }
            val swipeCallback = RoomMessageTouchHelperCallback(requireContext(), R.drawable.ic_reply, quickReplyHandler)
            val touchHelper = ItemTouchHelper(swipeCallback)
            touchHelper.attachToRecyclerView(recyclerView)
        }
    }

    private fun updateJumpToReadMarkerViewVisibility() {
        jumpToReadMarkerView?.post {
            withState(roomDetailViewModel) {
                val showJumpToUnreadBanner = when (it.unreadState) {
                    UnreadState.Unknown,
                    UnreadState.HasNoUnread            -> false
                    is UnreadState.ReadMarkerNotLoaded -> true
                    is UnreadState.HasUnread           -> {
                        if (it.canShowJumpToReadMarker) {
                            val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                            val positionOfReadMarker = timelineEventController.getPositionOfReadMarker()
                            if (positionOfReadMarker == null) {
                                false
                            } else {
                                positionOfReadMarker > lastVisibleItem
                            }
                        } else {
                            false
                        }
                    }
                }
                jumpToReadMarkerView?.isVisible = showJumpToUnreadBanner
            }
        }
    }

    private fun setupComposer() {
        autoCompleter.setup(composerLayout.composerEditText)

        observerUserTyping()

        composerLayout.callback = object : TextComposerView.Callback {
            override fun onAddAttachment() {
                if (!::attachmentTypeSelector.isInitialized) {
                    attachmentTypeSelector = AttachmentTypeSelectorView(vectorBaseActivity, vectorBaseActivity.layoutInflater, this@RoomDetailFragment)
                }
                attachmentTypeSelector.show(composerLayout.attachmentButton, keyboardStateUtils.isKeyboardShowing)
            }

            override fun onSendMessage(text: CharSequence) {
                if (lockSendButton) {
                    Timber.w("Send button is locked")
                    return
                }
                if (text.isNotBlank()) {
                    lockSendButton = true
                    roomDetailViewModel.handle(RoomDetailAction.SendMessage(text, vectorPreferences.isMarkdownEnabled()))
                }
            }

            override fun onCloseRelatedMessage() {
                roomDetailViewModel.handle(RoomDetailAction.ExitSpecialMode(composerLayout.text.toString()))
            }

            override fun onRichContentSelected(contentUri: Uri): Boolean {
                // We need WRITE_EXTERNAL permission
                return if (checkPermissions(PERMISSIONS_FOR_WRITING_FILES, this@RoomDetailFragment, PERMISSION_REQUEST_CODE_INCOMING_URI)) {
                    sendUri(contentUri)
                } else {
                    roomDetailViewModel.pendingUri = contentUri
                    // Always intercept when we request some permission
                    true
                }
            }
        }
    }

    private fun observerUserTyping() {
        composerLayout.composerEditText.textChanges()
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
                .map { it.isNotEmpty() }
                .subscribe {
                    Timber.d("Typing: User is typing: $it")
                    roomDetailViewModel.handle(RoomDetailAction.UserIsTyping(it))
                }
                .disposeOnDestroyView()
    }

    private fun sendUri(uri: Uri): Boolean {
        val shareIntent = Intent(Intent.ACTION_SEND, uri)
        val isHandled = attachmentsHelper.handleShareIntent(shareIntent)
        if (!isHandled) {
            Toast.makeText(requireContext(), R.string.error_handling_incoming_share, Toast.LENGTH_SHORT).show()
        }
        return isHandled
    }

    private fun setupInviteView() {
        inviteView.callback = this
    }

    private fun renderState(state: RoomDetailViewState) {
        renderRoomSummary(state)
        val summary = state.asyncRoomSummary()
        val inviter = state.asyncInviter()
        if (summary?.membership == Membership.JOIN) {
            scrollOnHighlightedEventCallback.timeline = roomDetailViewModel.timeline
            timelineEventController.update(state)
            inviteView.visibility = View.GONE
            val uid = session.myUserId
            val meMember = state.myRoomMember()
            avatarRenderer.render(MatrixItem.UserItem(uid, meMember?.displayName, meMember?.avatarUrl), composerLayout.composerAvatarImageView)
        } else if (summary?.membership == Membership.INVITE && inviter != null) {
            inviteView.visibility = View.VISIBLE
            inviteView.render(inviter, VectorInviteView.Mode.LARGE)

            // Intercept click event
            inviteView.setOnClickListener { }
        } else if (state.asyncInviter.complete) {
            vectorBaseActivity.finish()
        }
        val isRoomEncrypted = summary?.isEncrypted ?: false
        if (state.tombstoneEvent == null) {
            composerLayout.visibility = View.VISIBLE
            composerLayout.setRoomEncrypted(isRoomEncrypted)
            notificationAreaView.render(NotificationAreaView.State.Hidden)
        } else {
            composerLayout.visibility = View.GONE
            notificationAreaView.render(NotificationAreaView.State.Tombstone(state.tombstoneEvent))
        }
    }

    private fun renderRoomSummary(state: RoomDetailViewState) {
        state.asyncRoomSummary()?.let { roomSummary ->
            if (roomSummary.membership.isLeft()) {
                Timber.w("The room has been left")
                activity?.finish()
            } else {
                roomToolbarTitleView.text = roomSummary.displayName
                avatarRenderer.render(roomSummary.toMatrixItem(), roomToolbarAvatarImageView)

                renderSubTitle(state.typingMessage, roomSummary.topic)
            }
            jumpToBottomView.count = roomSummary.notificationCount
            jumpToBottomView.drawBadge = roomSummary.hasUnreadMessages

            roomToolbarDecorationImageView.let {
                it.setImageResource(roomSummary.roomEncryptionTrustLevel.toImageRes())
                it.isVisible = roomSummary.roomEncryptionTrustLevel != null
            }
        }
    }

    private fun renderSubTitle(typingMessage: String?, topic: String) {
        // TODO Temporary place to put typing data
        roomToolbarSubtitleView.let {
            it.setTextOrHide(typingMessage ?: topic)

            if (typingMessage == null) {
                it.setTextColor(ThemeUtils.getColor(requireContext(), R.attr.vctr_toolbar_secondary_text_color))
                it.setTypeface(null, Typeface.NORMAL)
            } else {
                it.setTextColor(ContextCompat.getColor(requireContext(), R.color.riotx_accent))
                it.setTypeface(null, Typeface.BOLD)
            }
        }
    }

    private fun renderTombstoneEventHandling(async: Async<String>) {
        when (async) {
            is Loading -> {
                // TODO Better handling progress
                vectorBaseActivity.showWaitingView()
                vectorBaseActivity.waiting_view_status_text.visibility = View.VISIBLE
                vectorBaseActivity.waiting_view_status_text.text = getString(R.string.joining_room)
            }
            is Success -> {
                navigator.openRoom(vectorBaseActivity, async())
                vectorBaseActivity.finish()
            }
            is Fail    -> {
                vectorBaseActivity.hideWaitingView()
                vectorBaseActivity.toast(errorFormatter.toHumanReadable(async.error))
            }
        }
    }

    private fun renderSendMessageResult(sendMessageResult: RoomDetailViewEvents.SendMessageResult) {
        when (sendMessageResult) {
            is RoomDetailViewEvents.MessageSent                -> {
                updateComposerText("")
            }
            is RoomDetailViewEvents.SlashCommandHandled        -> {
                sendMessageResult.messageRes?.let { showSnackWithMessage(getString(it)) }
                updateComposerText("")
            }
            is RoomDetailViewEvents.SlashCommandError          -> {
                displayCommandError(getString(R.string.command_problem_with_parameters, sendMessageResult.command.command))
            }
            is RoomDetailViewEvents.SlashCommandUnknown        -> {
                displayCommandError(getString(R.string.unrecognized_command, sendMessageResult.command))
            }
            is RoomDetailViewEvents.SlashCommandResultOk       -> {
                updateComposerText("")
            }
            is RoomDetailViewEvents.SlashCommandResultError    -> {
                displayCommandError(sendMessageResult.throwable.localizedMessage)
            }
            is RoomDetailViewEvents.SlashCommandNotImplemented -> {
                displayCommandError(getString(R.string.not_implemented))
            }
        } // .exhaustive

        lockSendButton = false
    }

    private fun displayCommandError(message: String) {
        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.command_error)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    private fun promptReasonToReportContent(action: EventSharedAction.ReportContentCustom) {
        val inflater = requireActivity().layoutInflater
        val layout = inflater.inflate(R.layout.dialog_report_content, null)

        val input = layout.findViewById<TextInputEditText>(R.id.dialog_report_content_input)

        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.report_content_custom_title)
                .setView(layout)
                .setPositiveButton(R.string.report_content_custom_submit) { _, _ ->
                    val reason = input.text.toString()
                    roomDetailViewModel.handle(RoomDetailAction.ReportContent(action.eventId, action.senderId, reason))
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun promptReasonToRedactEvent(eventId: String) {
        val layout = requireActivity().layoutInflater.inflate(R.layout.dialog_delete_event, null)
        val reasonCheckBox = layout.findViewById<MaterialCheckBox>(R.id.deleteEventReasonCheck)
        val reasonTextInputLayout = layout.findViewById<TextInputLayout>(R.id.deleteEventReasonTextInputLayout)
        val reasonInput = layout.findViewById<TextInputEditText>(R.id.deleteEventReasonInput)

        reasonCheckBox.setOnCheckedChangeListener { _, isChecked -> reasonTextInputLayout.isEnabled = isChecked }

        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.delete_event_dialog_title)
                .setView(layout)
                .setPositiveButton(R.string.remove) { _, _ ->
                    val reason = reasonInput.text.toString()
                            .takeIf { reasonCheckBox.isChecked }
                            ?.takeIf { it.isNotBlank() }
                    roomDetailViewModel.handle(RoomDetailAction.RedactAction(eventId, reason))
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun displayRoomDetailActionFailure(result: RoomDetailViewEvents.ActionFailure) {
        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.dialog_title_error)
                .setMessage(errorFormatter.toHumanReadable(result.throwable))
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    private fun displayRoomDetailActionSuccess(result: RoomDetailViewEvents.ActionSuccess) {
        when (val data = result.action) {
            is RoomDetailAction.ReportContent             -> {
                when {
                    data.spam          -> {
                        AlertDialog.Builder(requireActivity())
                                .setTitle(R.string.content_reported_as_spam_title)
                                .setMessage(R.string.content_reported_as_spam_content)
                                .setPositiveButton(R.string.ok, null)
                                .setNegativeButton(R.string.block_user) { _, _ ->
                                    roomDetailViewModel.handle(RoomDetailAction.IgnoreUser(data.senderId))
                                }
                                .show()
                                .withColoredButton(DialogInterface.BUTTON_NEGATIVE)
                    }
                    data.inappropriate -> {
                        AlertDialog.Builder(requireActivity())
                                .setTitle(R.string.content_reported_as_inappropriate_title)
                                .setMessage(R.string.content_reported_as_inappropriate_content)
                                .setPositiveButton(R.string.ok, null)
                                .setNegativeButton(R.string.block_user) { _, _ ->
                                    roomDetailViewModel.handle(RoomDetailAction.IgnoreUser(data.senderId))
                                }
                                .show()
                                .withColoredButton(DialogInterface.BUTTON_NEGATIVE)
                    }
                    else               -> {
                        AlertDialog.Builder(requireActivity())
                                .setTitle(R.string.content_reported_title)
                                .setMessage(R.string.content_reported_content)
                                .setPositiveButton(R.string.ok, null)
                                .setNegativeButton(R.string.block_user) { _, _ ->
                                    roomDetailViewModel.handle(RoomDetailAction.IgnoreUser(data.senderId))
                                }
                                .show()
                                .withColoredButton(DialogInterface.BUTTON_NEGATIVE)
                    }
                }
            }
            is RoomDetailAction.RequestVerification       -> {
                Timber.v("## SAS RequestVerification action")
                VerificationBottomSheet.withArgs(
                        roomDetailArgs.roomId,
                        data.userId
                ).show(parentFragmentManager, "REQ")
            }
            is RoomDetailAction.AcceptVerificationRequest -> {
                Timber.v("## SAS AcceptVerificationRequest action")
                VerificationBottomSheet.withArgs(
                        roomDetailArgs.roomId,
                        data.otherUserId,
                        data.transactionId
                ).show(parentFragmentManager, "REQ")
            }
            is RoomDetailAction.ResumeVerification        -> {
                val otherUserId = data.otherUserId ?: return
                VerificationBottomSheet().apply {
                    arguments = Bundle().apply {
                        putParcelable(MvRx.KEY_ARG, VerificationBottomSheet.VerificationArgs(
                                otherUserId, data.transactionId, roomId = roomDetailArgs.roomId))
                    }
                }.show(parentFragmentManager, "REQ")
            }
        }
    }

    // TimelineEventController.Callback ************************************************************

    override fun onUrlClicked(url: String): Boolean {
        permalinkHandler
                .launch(requireActivity(), url, object : NavigationInterceptor {
                    override fun navToRoom(roomId: String?, eventId: String?): Boolean {
                        // Same room?
                        if (roomId == roomDetailArgs.roomId) {
                            // Navigation to same room
                            if (eventId == null) {
                                showSnackWithMessage(getString(R.string.navigate_to_room_when_already_in_the_room))
                            } else {
                                // Highlight and scroll to this event
                                roomDetailViewModel.handle(RoomDetailAction.NavigateToEvent(eventId, true))
                            }
                            return true
                        }
                        // Not handled
                        return false
                    }

                    override fun navToMemberProfile(userId: String): Boolean {
                        openRoomMemberProfile(userId)
                        return true
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { managed ->
                    if (!managed) {
                        // Open in external browser, in a new Tab
                        openUrlInExternalBrowser(requireContext(), url)
                    }
                }
                .disposeOnDestroyView()
        // In fact it is always managed
        return true
    }

    override fun onUrlLongClicked(url: String): Boolean {
        if (url != getString(R.string.edited_suffix)) {
            // Copy the url to the clipboard
            copyToClipboard(requireContext(), url, true, R.string.link_copied_to_clipboard)
        }
        return true
    }

    override fun onEventVisible(event: TimelineEvent) {
        roomDetailViewModel.handle(RoomDetailAction.TimelineEventTurnsVisible(event))
    }

    override fun onEventInvisible(event: TimelineEvent) {
        roomDetailViewModel.handle(RoomDetailAction.TimelineEventTurnsInvisible(event))
    }

    override fun onEncryptedMessageClicked(informationData: MessageInformationData, view: View) {
        vectorBaseActivity.notImplemented("encrypted message click")
    }

    override fun onImageMessageClicked(messageImageContent: MessageImageInfoContent, mediaData: ImageContentRenderer.Data, view: View) {
        // TODO Use navigator

        val intent = ImageMediaViewerActivity.newIntent(vectorBaseActivity, mediaData, ViewCompat.getTransitionName(view))
        val pairs = ArrayList<Pair<View, String>>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            requireActivity().window.decorView.findViewById<View>(android.R.id.statusBarBackground)?.let {
                pairs.add(Pair(it, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME))
            }
            requireActivity().window.decorView.findViewById<View>(android.R.id.navigationBarBackground)?.let {
                pairs.add(Pair(it, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME))
            }
        }
        pairs.add(Pair(view, ViewCompat.getTransitionName(view) ?: ""))
        pairs.add(Pair(roomToolbar, ViewCompat.getTransitionName(roomToolbar) ?: ""))
        pairs.add(Pair(composerLayout, ViewCompat.getTransitionName(composerLayout) ?: ""))

        val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                requireActivity(), *pairs.toTypedArray()).toBundle()
        startActivity(intent, bundle)
    }

    override fun onVideoMessageClicked(messageVideoContent: MessageVideoContent, mediaData: VideoContentRenderer.Data, view: View) {
        // TODO Use navigator
        val intent = VideoMediaViewerActivity.newIntent(vectorBaseActivity, mediaData)
        startActivity(intent)
    }

    override fun onFileMessageClicked(eventId: String, messageFileContent: MessageFileContent) {
        val action = RoomDetailAction.DownloadFile(eventId, messageFileContent)
        // We need WRITE_EXTERNAL permission
        if (checkPermissions(PERMISSIONS_FOR_WRITING_FILES, this, PERMISSION_REQUEST_CODE_DOWNLOAD_FILE)) {
            showSnackWithMessage(getString(R.string.downloading_file, messageFileContent.getFileName()))
            roomDetailViewModel.handle(action)
        } else {
            roomDetailViewModel.pendingAction = action
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (allGranted(grantResults)) {
            when (requestCode) {
                PERMISSION_REQUEST_CODE_DOWNLOAD_FILE   -> {
                    val action = roomDetailViewModel.pendingAction
                    if (action != null) {
                        (action as? RoomDetailAction.DownloadFile)
                                ?.messageFileContent
                                ?.getFileName()
                                ?.let { showSnackWithMessage(getString(R.string.downloading_file, it)) }
                        roomDetailViewModel.pendingAction = null
                        roomDetailViewModel.handle(action)
                    }
                }
                PERMISSION_REQUEST_CODE_INCOMING_URI    -> {
                    val pendingUri = roomDetailViewModel.pendingUri
                    if (pendingUri != null) {
                        roomDetailViewModel.pendingUri = null
                        sendUri(pendingUri)
                    }
                }
                PERMISSION_REQUEST_CODE_PICK_ATTACHMENT -> {
                    val pendingType = attachmentsHelper.pendingType
                    if (pendingType != null) {
                        attachmentsHelper.pendingType = null
                        launchAttachmentProcess(pendingType)
                    }
                }
            }
        } else {
            // Reset all pending data
            roomDetailViewModel.pendingAction = null
            roomDetailViewModel.pendingUri = null
            attachmentsHelper.pendingType = null
        }
    }

    override fun onAudioMessageClicked(messageAudioContent: MessageAudioContent) {
        vectorBaseActivity.notImplemented("open audio file")
    }

    override fun onLoadMore(direction: Timeline.Direction) {
        roomDetailViewModel.handle(RoomDetailAction.LoadMoreTimelineEvents(direction))
    }

    override fun onEventCellClicked(informationData: MessageInformationData, messageContent: MessageContent?, view: View) {
        if (messageContent is MessageVerificationRequestContent) {
            roomDetailViewModel.handle(RoomDetailAction.ResumeVerification(informationData.eventId))
        }
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
        // roomDetailViewModel.handle(RoomDetailAction.RequestVerification(informationData.userId))
        openRoomMemberProfile(informationData.senderId)
    }

    private fun openRoomMemberProfile(userId: String) {
        navigator.openRoomMemberProfile(userId = userId, roomId = roomDetailArgs.roomId, context = requireActivity())
    }

    override fun onMemberNameClicked(informationData: MessageInformationData) {
        insertUserDisplayNameInTextEditor(informationData.senderId)
    }

    override fun onClickOnReactionPill(informationData: MessageInformationData, reaction: String, on: Boolean) {
        if (on) {
            // we should test the current real state of reaction on this event
            roomDetailViewModel.handle(RoomDetailAction.SendReaction(informationData.eventId, reaction))
        } else {
            // I need to redact a reaction
            roomDetailViewModel.handle(RoomDetailAction.UndoReaction(informationData.eventId, reaction))
        }
    }

    override fun onLongClickOnReactionPill(informationData: MessageInformationData, reaction: String) {
        ViewReactionsBottomSheet.newInstance(roomDetailArgs.roomId, informationData)
                .show(requireActivity().supportFragmentManager, "DISPLAY_REACTIONS")
    }

    override fun onEditedDecorationClicked(informationData: MessageInformationData) {
        ViewEditHistoryBottomSheet.newInstance(roomDetailArgs.roomId, informationData)
                .show(requireActivity().supportFragmentManager, "DISPLAY_EDITS")
    }

    override fun onTimelineItemAction(itemAction: RoomDetailAction) {
        roomDetailViewModel.handle(itemAction)
    }

    override fun onRoomCreateLinkClicked(url: String) {
        permalinkHandler
                .launch(requireContext(), url, object : NavigationInterceptor {
                    override fun navToRoom(roomId: String?, eventId: String?): Boolean {
                        requireActivity().finish()
                        return false
                    }
                })
                .subscribe()
                .disposeOnDestroyView()
    }

    override fun onReadReceiptsClicked(readReceipts: List<ReadReceiptData>) {
        DisplayReadReceiptsBottomSheet.newInstance(readReceipts)
                .show(requireActivity().supportFragmentManager, "DISPLAY_READ_RECEIPTS")
    }

    override fun onReadMarkerVisible() {
        updateJumpToReadMarkerViewVisibility()
        roomDetailViewModel.handle(RoomDetailAction.EnterTrackingUnreadMessagesState)
    }

    private fun handleActions(action: EventSharedAction) {
        when (action) {
            is EventSharedAction.OpenUserProfile            -> {
                openRoomMemberProfile(action.userId)
            }
            is EventSharedAction.AddReaction                -> {
                startActivityForResult(EmojiReactionPickerActivity.intent(requireContext(), action.eventId), REACTION_SELECT_REQUEST_CODE)
            }
            is EventSharedAction.ViewReactions              -> {
                ViewReactionsBottomSheet.newInstance(roomDetailArgs.roomId, action.messageInformationData)
                        .show(requireActivity().supportFragmentManager, "DISPLAY_REACTIONS")
            }
            is EventSharedAction.Copy                       -> {
                // I need info about the current selected message :/
                copyToClipboard(requireContext(), action.content, false)
                showSnackWithMessage(getString(R.string.copied_to_clipboard), Snackbar.LENGTH_SHORT)
            }
            is EventSharedAction.Redact                     -> {
                promptReasonToRedactEvent(action.eventId)
            }
            is EventSharedAction.Share                      -> {
                // TODO current data communication is too limited
                // Need to now the media type
                // TODO bad, just POC
                BigImageViewer.imageLoader().loadImage(
                        action.hashCode(),
                        Uri.parse(action.imageUrl),
                        object : ImageLoader.Callback {
                            override fun onFinish() {}

                            override fun onSuccess(image: File?) {
                                if (image != null) {
                                    shareMedia(requireContext(), image, "image/*")
                                }
                            }

                            override fun onFail(error: Exception?) {}

                            override fun onCacheHit(imageType: Int, image: File?) {}

                            override fun onCacheMiss(imageType: Int, image: File?) {}

                            override fun onProgress(progress: Int) {}

                            override fun onStart() {}
                        }
                )
            }
            is EventSharedAction.ViewEditHistory            -> {
                onEditedDecorationClicked(action.messageInformationData)
            }
            is EventSharedAction.ViewSource                 -> {
                val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_event_content, null)
                view.findViewById<TextView>(R.id.event_content_text_view)?.let {
                    it.text = action.content
                }

                AlertDialog.Builder(requireActivity())
                        .setView(view)
                        .setPositiveButton(R.string.ok, null)
                        .show()
            }
            is EventSharedAction.ViewDecryptedSource        -> {
                val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_event_content, null)
                view.findViewById<TextView>(R.id.event_content_text_view)?.let {
                    it.text = action.content
                }

                AlertDialog.Builder(requireActivity())
                        .setView(view)
                        .setPositiveButton(R.string.ok, null)
                        .show()
            }
            is EventSharedAction.QuickReact                 -> {
                // eventId,ClickedOn,Add
                roomDetailViewModel.handle(RoomDetailAction.UpdateQuickReactAction(action.eventId, action.clickedOn, action.add))
            }
            is EventSharedAction.Edit                       -> {
                roomDetailViewModel.handle(RoomDetailAction.EnterEditMode(action.eventId, composerLayout.text.toString()))
            }
            is EventSharedAction.Quote                      -> {
                roomDetailViewModel.handle(RoomDetailAction.EnterQuoteMode(action.eventId, composerLayout.text.toString()))
            }
            is EventSharedAction.Reply                      -> {
                roomDetailViewModel.handle(RoomDetailAction.EnterReplyMode(action.eventId, composerLayout.text.toString()))
            }
            is EventSharedAction.CopyPermalink              -> {
                val permalink = PermalinkFactory.createPermalink(roomDetailArgs.roomId, action.eventId)
                copyToClipboard(requireContext(), permalink, false)
                showSnackWithMessage(getString(R.string.copied_to_clipboard), Snackbar.LENGTH_SHORT)
            }
            is EventSharedAction.Resend                     -> {
                roomDetailViewModel.handle(RoomDetailAction.ResendMessage(action.eventId))
            }
            is EventSharedAction.Remove                     -> {
                roomDetailViewModel.handle(RoomDetailAction.RemoveFailedEcho(action.eventId))
            }
            is EventSharedAction.ReportContentSpam          -> {
                roomDetailViewModel.handle(RoomDetailAction.ReportContent(
                        action.eventId, action.senderId, "This message is spam", spam = true))
            }
            is EventSharedAction.ReportContentInappropriate -> {
                roomDetailViewModel.handle(RoomDetailAction.ReportContent(
                        action.eventId, action.senderId, "This message is inappropriate", inappropriate = true))
            }
            is EventSharedAction.ReportContentCustom        -> {
                promptReasonToReportContent(action)
            }
            is EventSharedAction.IgnoreUser                 -> {
                roomDetailViewModel.handle(RoomDetailAction.IgnoreUser(action.senderId))
            }
            is EventSharedAction.OnUrlClicked               -> {
                onUrlClicked(action.url)
            }
            is EventSharedAction.OnUrlLongClicked           -> {
                onUrlLongClicked(action.url)
            }
            else                                            -> {
                Toast.makeText(context, "Action $action is not implemented yet", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Insert a user displayName in the message editor.
     *
     * @param userId the userId.
     */
    @SuppressLint("SetTextI18n")
    private fun insertUserDisplayNameInTextEditor(userId: String) {
        val startToCompose = composerLayout.composerEditText.text.isNullOrBlank()

        if (startToCompose
                && userId == session.myUserId) {
            // Empty composer, current user: start an emote
            composerLayout.composerEditText.setText(Command.EMOTE.command + " ")
            composerLayout.composerEditText.setSelection(Command.EMOTE.length)
        } else {
            val roomMember = roomDetailViewModel.getMember(userId)
            // TODO move logic outside of fragment
            (roomMember?.displayName ?: userId)
                    .let { sanitizeDisplayName(it) }
                    .let { displayName ->
                        buildSpannedString {
                            append(displayName)
                            setSpan(
                                    PillImageSpan(
                                            glideRequests,
                                            avatarRenderer,
                                            requireContext(),
                                            MatrixItem.UserItem(userId, displayName, roomMember?.avatarUrl)
                                    )
                                            .also { it.bind(composerLayout.composerEditText) },
                                    0,
                                    displayName.length,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            append(if (startToCompose) ": " else " ")
                        }.let { pill ->
                            if (startToCompose) {
                                if (displayName.startsWith("/")) {
                                    // Ensure displayName will not be interpreted as a Slash command
                                    composerLayout.composerEditText.append("\\")
                                }
                                composerLayout.composerEditText.append(pill)
                            } else {
                                composerLayout.composerEditText.text?.insert(composerLayout.composerEditText.selectionStart, pill)
                            }
                        }
                    }
        }
        focusComposerAndShowKeyboard()
    }

    private fun focusComposerAndShowKeyboard() {
        composerLayout.composerEditText.showKeyboard(andRequestFocus = true)
    }

    private fun showSnackWithMessage(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(view!!, message, duration).show()
    }

    // VectorInviteView.Callback

    override fun onAcceptInvite() {
        notificationDrawerManager.clearMemberShipNotificationForRoom(roomDetailArgs.roomId)
        roomDetailViewModel.handle(RoomDetailAction.AcceptInvite)
    }

    override fun onRejectInvite() {
        notificationDrawerManager.clearMemberShipNotificationForRoom(roomDetailArgs.roomId)
        roomDetailViewModel.handle(RoomDetailAction.RejectInvite)
    }

    // JumpToReadMarkerView.Callback

    override fun onJumpToReadMarkerClicked() = withState(roomDetailViewModel) {
        jumpToReadMarkerView.isVisible = false
        if (it.unreadState is UnreadState.HasUnread) {
            roomDetailViewModel.handle(RoomDetailAction.NavigateToEvent(it.unreadState.firstUnreadEventId, false))
        }
        if (it.unreadState is UnreadState.ReadMarkerNotLoaded) {
            roomDetailViewModel.handle(RoomDetailAction.NavigateToEvent(it.unreadState.readMarkerId, false))
        }
    }

    override fun onClearReadMarkerClicked() {
        roomDetailViewModel.handle(RoomDetailAction.MarkAllAsRead)
    }

    // AttachmentTypeSelectorView.Callback

    override fun onTypeSelected(type: AttachmentTypeSelectorView.Type) {
        if (checkPermissions(type.permissionsBit, this, PERMISSION_REQUEST_CODE_PICK_ATTACHMENT)) {
            launchAttachmentProcess(type)
        } else {
            attachmentsHelper.pendingType = type
        }
    }

    private fun launchAttachmentProcess(type: AttachmentTypeSelectorView.Type) {
        when (type) {
            AttachmentTypeSelectorView.Type.CAMERA  -> attachmentsHelper.openCamera()
            AttachmentTypeSelectorView.Type.FILE    -> attachmentsHelper.selectFile()
            AttachmentTypeSelectorView.Type.GALLERY -> attachmentsHelper.selectGallery()
            AttachmentTypeSelectorView.Type.AUDIO   -> attachmentsHelper.selectAudio()
            AttachmentTypeSelectorView.Type.CONTACT -> attachmentsHelper.selectContact()
            AttachmentTypeSelectorView.Type.STICKER -> vectorBaseActivity.notImplemented("Adding stickers")
        }.exhaustive
    }

    // AttachmentsHelper.Callback

    override fun onContentAttachmentsReady(attachments: List<ContentAttachmentData>) {
        roomDetailViewModel.handle(RoomDetailAction.SendMedia(attachments))
    }

    override fun onAttachmentsProcessFailed() {
        Toast.makeText(requireContext(), R.string.error_attachment, Toast.LENGTH_SHORT).show()
    }

    override fun onContactAttachmentReady(contactAttachment: ContactAttachment) {
        super.onContactAttachmentReady(contactAttachment)
        val formattedContact = contactAttachment.toHumanReadable()
        roomDetailViewModel.handle(RoomDetailAction.SendMessage(formattedContact, false))
    }
}
