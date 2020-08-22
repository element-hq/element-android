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

package im.vector.app.features.home.room.detail

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
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.text.buildSpannedString
import androidx.core.text.toSpannable
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.OnModelBuildFinishedListener
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.app.R
import im.vector.app.core.dialogs.ConfirmationDialogBuilder
import im.vector.app.core.dialogs.withColoredButton
import im.vector.app.core.epoxy.LayoutManagerStateRestorer
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.extensions.showKeyboard
import im.vector.app.core.extensions.trackItemsVisibilityChange
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.intent.getMimeTypeFromUri
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.ui.views.ActiveCallView
import im.vector.app.core.ui.views.ActiveCallViewHolder
import im.vector.app.core.ui.views.ActiveConferenceView
import im.vector.app.core.ui.views.JumpToReadMarkerView
import im.vector.app.core.ui.views.NotificationAreaView
import im.vector.app.core.utils.Debouncer
import im.vector.app.core.utils.KeyboardStateUtils
import im.vector.app.core.utils.PERMISSIONS_FOR_AUDIO_IP_CALL
import im.vector.app.core.utils.PERMISSIONS_FOR_VIDEO_IP_CALL
import im.vector.app.core.utils.PERMISSIONS_FOR_WRITING_FILES
import im.vector.app.core.utils.PERMISSION_REQUEST_CODE_INCOMING_URI
import im.vector.app.core.utils.PERMISSION_REQUEST_CODE_PICK_ATTACHMENT
import im.vector.app.core.utils.TextUtils
import im.vector.app.core.utils.allGranted
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.colorizeMatchingText
import im.vector.app.core.utils.copyToClipboard
import im.vector.app.core.utils.createJSonViewerStyleProvider
import im.vector.app.core.utils.createUIHandler
import im.vector.app.core.utils.getColorFromUserId
import im.vector.app.core.utils.isValidUrl
import im.vector.app.core.utils.onPermissionResultAudioIpCall
import im.vector.app.core.utils.onPermissionResultVideoIpCall
import im.vector.app.core.utils.openUrlInExternalBrowser
import im.vector.app.core.utils.saveMedia
import im.vector.app.core.utils.shareMedia
import im.vector.app.core.utils.toast
import im.vector.app.features.attachments.AttachmentTypeSelectorView
import im.vector.app.features.attachments.AttachmentsHelper
import im.vector.app.features.attachments.ContactAttachment
import im.vector.app.features.attachments.preview.AttachmentsPreviewActivity
import im.vector.app.features.attachments.preview.AttachmentsPreviewArgs
import im.vector.app.features.attachments.toGroupedContentAttachmentData
import im.vector.app.features.call.SharedActiveCallViewModel
import im.vector.app.features.call.VectorCallActivity
import im.vector.app.features.call.WebRtcPeerConnectionManager
import im.vector.app.features.call.conference.JitsiCallViewModel
import im.vector.app.features.command.Command
import im.vector.app.features.crypto.keysbackup.restore.KeysBackupRestoreActivity
import im.vector.app.features.crypto.util.toImageRes
import im.vector.app.features.crypto.verification.VerificationBottomSheet
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.composer.TextComposerView
import im.vector.app.features.home.room.detail.readreceipts.DisplayReadReceiptsBottomSheet
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.action.EventSharedAction
import im.vector.app.features.home.room.detail.timeline.action.MessageActionsBottomSheet
import im.vector.app.features.home.room.detail.timeline.action.MessageSharedActionViewModel
import im.vector.app.features.home.room.detail.timeline.edithistory.ViewEditHistoryBottomSheet
import im.vector.app.features.home.room.detail.timeline.item.AbsMessageItem
import im.vector.app.features.home.room.detail.timeline.item.MessageFileItem
import im.vector.app.features.home.room.detail.timeline.item.MessageImageVideoItem
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.app.features.home.room.detail.timeline.item.MessageTextItem
import im.vector.app.features.home.room.detail.timeline.item.ReadReceiptData
import im.vector.app.features.home.room.detail.timeline.reactions.ViewReactionsBottomSheet
import im.vector.app.features.home.room.detail.widget.RoomWidgetsBottomSheet
import im.vector.app.features.home.room.detail.widget.WidgetRequestCodes
import im.vector.app.features.html.EventHtmlRenderer
import im.vector.app.features.html.PillImageSpan
import im.vector.app.features.invite.VectorInviteView
import im.vector.app.features.media.ImageContentRenderer
import im.vector.app.features.media.VideoContentRenderer
import im.vector.app.features.notifications.NotificationDrawerManager
import im.vector.app.features.notifications.NotificationUtils
import im.vector.app.features.permalink.NavigationInterceptor
import im.vector.app.features.permalink.PermalinkHandler
import im.vector.app.features.reactions.EmojiReactionPickerActivity
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.settings.VectorSettingsActivity
import im.vector.app.features.share.SharedData
import im.vector.app.features.themes.ThemeUtils
import im.vector.app.features.widgets.WidgetActivity
import im.vector.app.features.widgets.WidgetArgs
import im.vector.app.features.widgets.WidgetKind
import im.vector.app.features.widgets.permissions.RoomWidgetPermissionBottomSheet
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_room_detail.*
import kotlinx.android.synthetic.main.merge_composer_layout.view.*
import kotlinx.android.synthetic.main.merge_overlay_waiting_view.*
import org.billcarsonfr.jsonviewer.JSonViewerDialog
import org.commonmark.parser.Parser
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.file.FileService
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageFormat
import org.matrix.android.sdk.api.session.room.model.message.MessageImageInfoContent
import org.matrix.android.sdk.api.session.room.model.message.MessageStickerContent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationRequestContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVideoContent
import org.matrix.android.sdk.api.session.room.model.message.MessageWithAttachmentContent
import org.matrix.android.sdk.api.session.room.model.message.getFileUrl
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.widgets.model.Widget
import org.matrix.android.sdk.api.session.widgets.model.WidgetType
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.internal.crypto.attachments.toElementToDecrypt
import org.matrix.android.sdk.internal.crypto.model.event.EncryptedEventContent
import org.matrix.android.sdk.internal.crypto.model.event.WithHeldCode
import timber.log.Timber
import java.io.File
import java.net.URL
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
        private val vectorPreferences: VectorPreferences,
        private val colorProvider: ColorProvider,
        private val notificationUtils: NotificationUtils,
        private val webRtcPeerConnectionManager: WebRtcPeerConnectionManager) :
        VectorBaseFragment(),
        TimelineEventController.Callback,
        VectorInviteView.Callback,
        JumpToReadMarkerView.Callback,
        AttachmentTypeSelectorView.Callback,
        AttachmentsHelper.Callback,
//        RoomWidgetsBannerView.Callback,
        ActiveCallView.Callback {

    companion object {

        private const val AUDIO_CALL_PERMISSION_REQUEST_CODE = 1
        private const val VIDEO_CALL_PERMISSION_REQUEST_CODE = 2
        private const val SAVE_ATTACHEMENT_REQUEST_CODE = 3

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
    private lateinit var sharedCallActionViewModel: SharedActiveCallViewModel

    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var jumpToBottomViewVisibilityManager: JumpToBottomViewVisibilityManager
    private var modelBuildListener: OnModelBuildFinishedListener? = null

    private lateinit var attachmentsHelper: AttachmentsHelper
    private lateinit var keyboardStateUtils: KeyboardStateUtils

    @BindView(R.id.composerLayout)
    lateinit var composerLayout: TextComposerView
    private lateinit var attachmentTypeSelector: AttachmentTypeSelectorView

    private var lockSendButton = false
    private val activeCallViewHolder = ActiveCallViewHolder()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(MessageSharedActionViewModel::class.java)
        sharedCallActionViewModel = activityViewModelProvider.get(SharedActiveCallViewModel::class.java)
        attachmentsHelper = AttachmentsHelper(requireContext(), this).register()
        keyboardStateUtils = KeyboardStateUtils(requireActivity())
        setupToolbar(roomToolbar)
        setupRecyclerView()
        setupComposer()
        setupInviteView()
        setupNotificationView()
        setupJumpToReadMarkerView()
        setupActiveCallView()
        setupJumpToBottomView()
        setupConfBannerView()

        roomToolbarContentView.debouncedClicks {
            navigator.openRoomProfile(requireActivity(), roomDetailArgs.roomId)
        }

        sharedActionViewModel
                .observe()
                .subscribe {
                    handleActions(it)
                }
                .disposeOnDestroyView()

        sharedCallActionViewModel
                .activeCall
                .observe(viewLifecycleOwner, Observer {
                    activeCallViewHolder.updateCall(it, webRtcPeerConnectionManager)
                    invalidateOptionsMenu()
                })

        roomDetailViewModel.selectSubscribe(this, RoomDetailViewState::tombstoneEventHandling, uniqueOnly("tombstoneEventHandling")) {
            renderTombstoneEventHandling(it)
        }

        roomDetailViewModel.selectSubscribe(RoomDetailViewState::sendMode, RoomDetailViewState::canSendMessage) { mode, canSend ->
            if (!canSend) {
                return@selectSubscribe
            }
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
                is RoomDetailViewEvents.Failure                          -> showErrorInSnackbar(it.throwable)
                is RoomDetailViewEvents.OnNewTimelineEvents              -> scrollOnNewMessageCallback.addNewTimelineEventIds(it.eventIds)
                is RoomDetailViewEvents.ActionSuccess                    -> displayRoomDetailActionSuccess(it)
                is RoomDetailViewEvents.ActionFailure                    -> displayRoomDetailActionFailure(it)
                is RoomDetailViewEvents.ShowMessage                      -> showSnackWithMessage(it.message, Snackbar.LENGTH_LONG)
                is RoomDetailViewEvents.NavigateToEvent                  -> navigateToEvent(it)
                is RoomDetailViewEvents.FileTooBigError                  -> displayFileTooBigError(it)
                is RoomDetailViewEvents.DownloadFileState                -> handleDownloadFileState(it)
                is RoomDetailViewEvents.JoinRoomCommandSuccess           -> handleJoinedToAnotherRoom(it)
                is RoomDetailViewEvents.SendMessageResult                -> renderSendMessageResult(it)
                is RoomDetailViewEvents.ShowE2EErrorMessage              -> displayE2eError(it.withHeldCode)
                RoomDetailViewEvents.DisplayPromptForIntegrationManager  -> displayPromptForIntegrationManager()
                is RoomDetailViewEvents.OpenStickerPicker                -> openStickerPicker(it)
                is RoomDetailViewEvents.DisplayEnableIntegrationsWarning -> displayDisabledIntegrationDialog()
                is RoomDetailViewEvents.OpenIntegrationManager           -> openIntegrationManager()
                is RoomDetailViewEvents.OpenFile                         -> startOpenFileIntent(it)
                RoomDetailViewEvents.OpenActiveWidgetBottomSheet         -> onViewWidgetsClicked()
                is RoomDetailViewEvents.ShowInfoOkDialog                 -> showDialogWithMessage(it.message)
                is RoomDetailViewEvents.JoinJitsiConference              -> joinJitsiRoom(it.widget, it.withVideo)
                RoomDetailViewEvents.ShowWaitingView                     -> vectorBaseActivity.showWaitingView()
                RoomDetailViewEvents.HideWaitingView                     -> vectorBaseActivity.hideWaitingView()
                is RoomDetailViewEvents.RequestNativeWidgetPermission    -> requestNativeWidgetPermission(it)
            }.exhaustive
        }
    }

    private fun requestNativeWidgetPermission(it: RoomDetailViewEvents.RequestNativeWidgetPermission) {
        val tag = RoomWidgetPermissionBottomSheet::class.java.name
        val dFrag = childFragmentManager.findFragmentByTag(tag) as? RoomWidgetPermissionBottomSheet
        if (dFrag != null && dFrag.dialog?.isShowing == true && !dFrag.isRemoving) {
            return
        } else {
            RoomWidgetPermissionBottomSheet.newInstance(
                    WidgetArgs(
                            baseUrl = it.domain,
                            kind = WidgetKind.ROOM,
                            roomId = roomDetailArgs.roomId,
                            widgetId = it.widget.widgetId
                    )
            ).apply {
                directListener = { granted ->
                    if (granted) {
                        roomDetailViewModel.handle(RoomDetailAction.EnsureNativeWidgetAllowed(
                                widget = it.widget,
                                userJustAccepted = true,
                                grantedEvents = it.grantedEvents
                        ))
                    }
                }
            }
                    .show(childFragmentManager, tag)
        }
    }

    private fun openIntegrationManager(screen: String? = null) {
        navigator.openIntegrationManager(
                fragment = this,
                roomId = roomDetailArgs.roomId,
                integId = null,
                screen = screen
        )
    }

    private fun setupConfBannerView() {
        activeConferenceView.callback = object : ActiveConferenceView.Callback {
            override fun onTapJoinAudio(jitsiWidget: Widget) {
                // need to check if allowed first
                roomDetailViewModel.handle(RoomDetailAction.EnsureNativeWidgetAllowed(
                        widget = jitsiWidget,
                        userJustAccepted = false,
                        grantedEvents = RoomDetailViewEvents.JoinJitsiConference(jitsiWidget, false))
                )
            }

            override fun onTapJoinVideo(jitsiWidget: Widget) {
                roomDetailViewModel.handle(RoomDetailAction.EnsureNativeWidgetAllowed(
                        widget = jitsiWidget,
                        userJustAccepted = false,
                        grantedEvents = RoomDetailViewEvents.JoinJitsiConference(jitsiWidget, true))
                )
            }

            override fun onDelete(jitsiWidget: Widget) {
                roomDetailViewModel.handle(RoomDetailAction.RemoveWidget(jitsiWidget.widgetId))
            }
        }
    }

    private fun joinJitsiRoom(jitsiWidget: Widget, enableVideo: Boolean) {
        navigator.openRoomWidget(requireContext(), roomDetailArgs.roomId, jitsiWidget, mapOf(JitsiCallViewModel.ENABLE_VIDEO_OPTION to enableVideo))
    }

    private fun openStickerPicker(event: RoomDetailViewEvents.OpenStickerPicker) {
        navigator.openStickerPicker(this, roomDetailArgs.roomId, event.widget)
    }

    private fun startOpenFileIntent(action: RoomDetailViewEvents.OpenFile) {
        if (action.uri != null) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndTypeAndNormalize(action.uri, action.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                requireActivity().startActivity(intent)
            } else {
                requireActivity().toast(R.string.error_no_external_application_found)
            }
        }
    }

    private fun displayPromptForIntegrationManager() {
        // The Sticker picker widget is not installed yet. Propose the user to install it
        val builder = AlertDialog.Builder(requireContext())
        val v: View = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_no_sticker_pack, null)
        builder
                .setView(v)
                .setPositiveButton(R.string.yes) { _, _ ->
                    // Open integration manager, to the sticker installation page
                    openIntegrationManager(
                            screen = WidgetType.StickerPicker.preferred
                    )
                }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    private fun handleJoinedToAnotherRoom(action: RoomDetailViewEvents.JoinRoomCommandSuccess) {
        updateComposerText("")
        lockSendButton = false
        navigator.openRoom(vectorBaseActivity, action.roomId)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState == null) {
            when (val sharedData = roomDetailArgs.sharedData) {
                is SharedData.Text        -> {
                    // Save a draft to set the shared text to the composer
                    roomDetailViewModel.handle(RoomDetailAction.SaveDraft(sharedData.text))
                }
                is SharedData.Attachments -> {
                    // open share edition
                    onContentAttachmentsReady(sharedData.attachmentData)
                }
                null                      -> Timber.v("No share data to process")
            }.exhaustive
        }
    }

    override fun onDestroyView() {
        timelineEventController.callback = null
        timelineEventController.removeModelBuildListener(modelBuildListener)
        activeCallView.callback = null
        modelBuildListener = null
        autoCompleter.clear()
        debouncer.cancelAll()
        recyclerView.cleanup()

        super.onDestroyView()
    }

    override fun onDestroy() {
        activeCallViewHolder.unBind(webRtcPeerConnectionManager)
        roomDetailViewModel.handle(RoomDetailAction.ExitTrackingUnreadMessagesState)
        super.onDestroy()
    }

    private fun setupJumpToBottomView() {
        jumpToBottomView.visibility = View.INVISIBLE
        jumpToBottomView.debouncedClicks {
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

    private fun setupActiveCallView() {
        activeCallViewHolder.bind(
                activeCallPiP,
                activeCallView,
                activeCallPiPWrap,
                this
        )
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
        }
//        else if (action.file != null) {
//            addEntryToDownloadManager(activity, action.file, action.mimeType ?: "application/octet-stream")?.let {
//                // This is a temporary solution to help users find downloaded files
//                // there is a better way to do that
//                // On android Q+ this method returns the file URI, on older
//                // it returns null, and the download manager handles the notification
//                notificationUtils.buildDownloadFileNotification(
//                        it,
//                        action.file.name ?: "file",
//                        action.mimeType ?: "application/octet-stream"
//                ).let { notification ->
//                    notificationUtils.showNotificationMessage("DL", action.file.absolutePath.hashCode(), notification)
//                }
//            }
//        }
    }

    private fun setupNotificationView() {
        notificationAreaView.delegate = object : NotificationAreaView.Delegate {
            override fun onTombstoneEventClicked(tombstoneEvent: Event) {
                roomDetailViewModel.handle(RoomDetailAction.HandleTombstoneEvent(tombstoneEvent))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        // We use a custom layout for this menu item, so we need to set a ClickListener
        menu.findItem(R.id.open_matrix_apps)?.let { menuItem ->
            menuItem.actionView.setOnClickListener {
                onOptionsItemSelected(menuItem)
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.forEach {
            it.isVisible = roomDetailViewModel.isMenuItemVisible(it.itemId)
        }
        withState(roomDetailViewModel) { state ->
            val matrixAppsMenuItem = menu.findItem(R.id.open_matrix_apps)
            val widgetsCount = state.activeRoomWidgets.invoke()?.size ?: 0
            if (widgetsCount > 0) {
                val actionView = matrixAppsMenuItem.actionView
                actionView
                        .findViewById<ImageView>(R.id.action_view_icon_image)
                        .setColorFilter(ContextCompat.getColor(requireContext(), R.color.riotx_accent))
                actionView.findViewById<TextView>(R.id.cart_badge).setTextOrHide("$widgetsCount")
                matrixAppsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            } else {
                // icon should be default color no badge
                val actionView = matrixAppsMenuItem.actionView
                actionView
                        .findViewById<ImageView>(R.id.action_view_icon_image)
                        .setColorFilter(ThemeUtils.getColor(requireContext(), R.attr.riotx_text_secondary))
                actionView.findViewById<TextView>(R.id.cart_badge).isVisible = false
                matrixAppsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.clear_message_queue -> {
                // This a temporary option during dev as it is not super stable
                // Cancel all pending actions in room queue and post a dummy
                // Then mark all sending events as undelivered
                roomDetailViewModel.handle(RoomDetailAction.ClearSendQueue)
                true
            }
            R.id.resend_all          -> {
                roomDetailViewModel.handle(RoomDetailAction.ResendAll)
                true
            }
            R.id.open_matrix_apps    -> {
                roomDetailViewModel.handle(RoomDetailAction.ManageIntegrations)
                true
            }
            R.id.voice_call,
            R.id.video_call          -> {
                handleCallRequest(item)
                true
            }
            R.id.hangup_call         -> {
                roomDetailViewModel.handle(RoomDetailAction.EndCall)
                true
            }
            else                     -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleCallRequest(item: MenuItem) = withState(roomDetailViewModel) { state ->
        val roomSummary = state.asyncRoomSummary.invoke() ?: return@withState
        val isVideoCall = item.itemId == R.id.video_call
        when (roomSummary.joinedMembersCount) {
            1    -> {
                val pendingInvite = roomSummary.invitedMembersCount ?: 0 > 0
                if (pendingInvite) {
                    // wait for other to join
                    showDialogWithMessage(getString(R.string.cannot_call_yourself_with_invite))
                } else {
                    // You cannot place a call with yourself.
                    showDialogWithMessage(getString(R.string.cannot_call_yourself))
                }
            }
            2    -> {
                val activeCall = sharedCallActionViewModel.activeCall.value
                if (activeCall != null) {
                    // resume existing if same room, if not prompt to kill and then restart new call?
                    if (activeCall.roomId == roomDetailArgs.roomId) {
                        onTapToReturnToCall()
                    }
                    //                        else {
                    // TODO might not work well, and should prompt
                    //                            webRtcPeerConnectionManager.endCall()
                    //                            safeStartCall(it, isVideoCall)
                    //                        }
                } else {
                    safeStartCall(isVideoCall)
                }
            }
            else -> {
                // it's jitsi call
                // can you add widgets??
                if (!state.isAllowedToManageWidgets) {
                    // You do not have permission to start a conference call in this room
                    showDialogWithMessage(getString(R.string.no_permissions_to_start_conf_call))
                } else {
                    if (state.activeRoomWidgets()?.filter { it.type == WidgetType.Jitsi }?.any() == true) {
                        // A conference is already in progress!
                        showDialogWithMessage(getString(R.string.conference_call_in_progress))
                    } else {
                        AlertDialog.Builder(requireContext())
                                .setTitle(if (isVideoCall) R.string.video_meeting else R.string.audio_meeting)
                                .setMessage(R.string.audio_video_meeting_description)
                                .setPositiveButton(getString(R.string.create)) { _, _ ->
                                    // create the widget, then navigate to it..
                                    roomDetailViewModel.handle(RoomDetailAction.AddJitsiWidget(isVideoCall))
                                }
                                .setNegativeButton(getString(R.string.cancel), null)
                                .show()
                    }
                }
            }
        }
    }

    private fun displayDisabledIntegrationDialog() {
        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.disabled_integration_dialog_title)
                .setMessage(R.string.disabled_integration_dialog_content)
                .setPositiveButton(R.string.settings) { _, _ ->
                    navigator.openSettings(requireActivity(), VectorSettingsActivity.EXTRA_DIRECT_ACCESS_GENERAL)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun safeStartCall(isVideoCall: Boolean) {
        if (vectorPreferences.preventAccidentalCall()) {
            AlertDialog.Builder(requireActivity())
                    .setMessage(if (isVideoCall) R.string.start_video_call_prompt_msg else R.string.start_voice_call_prompt_msg)
                    .setPositiveButton(if (isVideoCall) R.string.start_video_call else R.string.start_voice_call) { _, _ ->
                        safeStartCall2(isVideoCall)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        } else {
            safeStartCall2(isVideoCall)
        }
    }

    private fun safeStartCall2(isVideoCall: Boolean) {
        val startCallAction = RoomDetailAction.StartCall(isVideoCall)
        roomDetailViewModel.pendingAction = startCallAction
        if (isVideoCall) {
            if (checkPermissions(PERMISSIONS_FOR_VIDEO_IP_CALL,
                            this, VIDEO_CALL_PERMISSION_REQUEST_CODE,
                            R.string.permissions_rationale_msg_camera_and_audio)) {
                roomDetailViewModel.pendingAction = null
                roomDetailViewModel.handle(startCallAction)
            }
        } else {
            if (checkPermissions(PERMISSIONS_FOR_AUDIO_IP_CALL,
                            this, AUDIO_CALL_PERMISSION_REQUEST_CODE,
                            R.string.permissions_rationale_msg_record_audio)) {
                roomDetailViewModel.pendingAction = null
                roomDetailViewModel.handle(startCallAction)
            }
        }
    }

    private fun renderRegularMode(text: String) {
        autoCompleter.exitSpecialMode()
        composerLayout.collapse()

        updateComposerText(text)
        composerLayout.sendButton.contentDescription = getString(R.string.send)
    }

    private fun renderSpecialMode(event: TimelineEvent,
                                  @DrawableRes iconRes: Int,
                                  @StringRes descriptionRes: Int,
                                  defaultContent: String) {
        autoCompleter.enterSpecialMode()
        // switch to expanded bar
        composerLayout.composerRelatedMessageTitle.apply {
            text = event.senderInfo.disambiguatedDisplayName
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
        composerLayout.composerRelatedMessageContent.text = (formattedBody ?: nonFormattedBody)

        updateComposerText(defaultContent)

        composerLayout.composerRelatedMessageActionIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), iconRes))
        composerLayout.sendButton.contentDescription = getString(descriptionRes)

        avatarRenderer.render(event.senderInfo.toMatrixItem(), composerLayout.composerRelatedMessageAvatar)

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
                AttachmentsPreviewActivity.REQUEST_CODE        -> {
                    val sendData = AttachmentsPreviewActivity.getOutput(data)
                    val keepOriginalSize = AttachmentsPreviewActivity.getKeepOriginalSize(data)
                    roomDetailViewModel.handle(RoomDetailAction.SendMedia(sendData, !keepOriginalSize))
                }
                REACTION_SELECT_REQUEST_CODE                   -> {
                    val (eventId, reaction) = EmojiReactionPickerActivity.getOutput(data) ?: return
                    roomDetailViewModel.handle(RoomDetailAction.SendReaction(eventId, reaction))
                }
                WidgetRequestCodes.STICKER_PICKER_REQUEST_CODE -> {
                    val content = WidgetActivity.getOutput(data).toModel<MessageStickerContent>() ?: return
                    roomDetailViewModel.handle(RoomDetailAction.SendSticker(content))
                }
            }
        }
        // TODO why don't we call super here?
        // super.onActivityResult(requestCode, resultCode, data)
    }

// PRIVATE METHODS *****************************************************************************

    private fun setupRecyclerView() {
        timelineEventController.callback = this
        timelineEventController.timeline = roomDetailViewModel.timeline

        recyclerView.trackItemsVisibilityChange()
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
                    val canSendMessage = withState(roomDetailViewModel) {
                        it.canSendMessage
                    }
                    if (!canSendMessage) {
                        return false
                    }
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
                    // We collapse ASAP, if not there will be a slight anoying delay
                    composerLayout.collapse(true)
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
        roomDetailViewModel.preventAttachmentPreview = true
        val shareIntent = Intent(Intent.ACTION_SEND, uri)
        val isHandled = attachmentsHelper.handleShareIntent(requireContext(), shareIntent)
        if (!isHandled) {
            roomDetailViewModel.preventAttachmentPreview = false
            Toast.makeText(requireContext(), R.string.error_handling_incoming_share, Toast.LENGTH_SHORT).show()
        }
        return isHandled
    }

    private fun setupInviteView() {
        inviteView.callback = this
    }

    override fun invalidate() = withState(roomDetailViewModel) { state ->
        invalidateOptionsMenu()
        val summary = state.asyncRoomSummary()
        renderToolbar(summary, state.typingMessage)
        activeConferenceView.render(state)
        val inviter = state.asyncInviter()
        if (summary?.membership == Membership.JOIN) {
            jumpToBottomView.count = summary.notificationCount
            jumpToBottomView.drawBadge = summary.hasUnreadMessages
            scrollOnHighlightedEventCallback.timeline = roomDetailViewModel.timeline
            timelineEventController.update(state)
            inviteView.visibility = View.GONE
            val uid = session.myUserId
            val meMember = state.myRoomMember()
            avatarRenderer.render(MatrixItem.UserItem(uid, meMember?.displayName, meMember?.avatarUrl), composerLayout.composerAvatarImageView)
            if (state.tombstoneEvent == null) {
                if (state.canSendMessage) {
                    composerLayout.visibility = View.VISIBLE
                    composerLayout.setRoomEncrypted(summary.isEncrypted, summary.roomEncryptionTrustLevel)
                    notificationAreaView.render(NotificationAreaView.State.Hidden)
                } else {
                    composerLayout.visibility = View.GONE
                    notificationAreaView.render(NotificationAreaView.State.NoPermissionToPost)
                }
            } else {
                composerLayout.visibility = View.GONE
                notificationAreaView.render(NotificationAreaView.State.Tombstone(state.tombstoneEvent))
            }
        } else if (summary?.membership == Membership.INVITE && inviter != null) {
            inviteView.visibility = View.VISIBLE
            inviteView.render(inviter, VectorInviteView.Mode.LARGE, state.changeMembershipState)
            // Intercept click event
            inviteView.setOnClickListener { }
        } else if (state.asyncInviter.complete) {
            vectorBaseActivity.finish()
        }
    }

    private fun renderToolbar(roomSummary: RoomSummary?, typingMessage: String?) {
        if (roomSummary == null) {
            roomToolbarContentView.isClickable = false
        } else {
            roomToolbarContentView.isClickable = roomSummary.membership == Membership.JOIN
            roomToolbarTitleView.text = roomSummary.displayName
            avatarRenderer.render(roomSummary.toMatrixItem(), roomToolbarAvatarImageView)

            renderSubTitle(typingMessage, roomSummary.topic)
            roomToolbarDecorationImageView.let {
                it.setImageResource(roomSummary.roomEncryptionTrustLevel.toImageRes())
                it.isVisible = roomSummary.roomEncryptionTrustLevel != null
            }
        }
    }

    private fun renderSubTitle(typingMessage: String?, topic: String) {
        // TODO Temporary place to put typing data
        val subtitle = typingMessage?.takeIf { it.isNotBlank() } ?: topic
        roomToolbarSubtitleView.apply {
            setTextOrHide(subtitle)
            if (typingMessage.isNullOrBlank()) {
                setTextColor(ThemeUtils.getColor(requireContext(), R.attr.vctr_toolbar_secondary_text_color))
                setTypeface(null, Typeface.NORMAL)
            } else {
                setTextColor(ContextCompat.getColor(requireContext(), R.color.riotx_accent))
                setTypeface(null, Typeface.BOLD)
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
                displayCommandError(errorFormatter.toHumanReadable(sendMessageResult.throwable))
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

    private fun displayE2eError(withHeldCode: WithHeldCode?) {
        val msgId = when (withHeldCode) {
            WithHeldCode.BLACKLISTED -> R.string.crypto_error_withheld_blacklisted
            WithHeldCode.UNVERIFIED  -> R.string.crypto_error_withheld_unverified
            WithHeldCode.UNAUTHORISED,
            WithHeldCode.UNAVAILABLE -> R.string.crypto_error_withheld_generic
            else                     -> R.string.notice_crypto_unable_to_decrypt_friendly_desc
        }
        AlertDialog.Builder(requireActivity())
                .setMessage(msgId)
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

    private fun promptConfirmationToRedactEvent(action: EventSharedAction.Redact) {
        ConfirmationDialogBuilder
                .show(
                        activity = requireActivity(),
                        askForReason = action.askForReason,
                        confirmationRes = R.string.delete_event_dialog_content,
                        positiveRes = R.string.remove,
                        reasonHintRes = R.string.delete_event_dialog_reason_hint,
                        titleRes = R.string.delete_event_dialog_title
                ) { reason ->
                    roomDetailViewModel.handle(RoomDetailAction.RedactAction(action.eventId, reason))
                }
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

    override fun onUrlClicked(url: String, title: String): Boolean {
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
                        if (title.isValidUrl() && url.isValidUrl() && URL(title).host != URL(url).host) {
                            AlertDialog.Builder(requireActivity())
                                    .setTitle(R.string.external_link_confirmation_title)
                                    .setMessage(
                                            getString(R.string.external_link_confirmation_message, title, url)
                                                    .toSpannable()
                                                    .colorizeMatchingText(url, colorProvider.getColorFromAttribute(R.attr.riotx_text_primary_body_contrast))
                                                    .colorizeMatchingText(title, colorProvider.getColorFromAttribute(R.attr.riotx_text_primary_body_contrast))
                                    )
                                    .setPositiveButton(R.string._continue) { _, _ ->
                                        openUrlInExternalBrowser(requireContext(), url)
                                    }
                                    .setNegativeButton(R.string.cancel, null)
                                    .show()
                                    .withColoredButton(DialogInterface.BUTTON_NEGATIVE)
                        } else {
                            // Open in external browser, in a new Tab
                            openUrlInExternalBrowser(requireContext(), url)
                        }
                    }
                }
                .disposeOnDestroyView()
        // In fact it is always managed
        return true
    }

    override fun onUrlLongClicked(url: String): Boolean {
        if (url != getString(R.string.edited_suffix) && url.isValidUrl()) {
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
        navigator.openMediaViewer(
                activity = requireActivity(),
                roomId = roomDetailArgs.roomId,
                mediaData = mediaData,
                view = view
        ) { pairs ->
            pairs.add(Pair(roomToolbar, ViewCompat.getTransitionName(roomToolbar) ?: ""))
            pairs.add(Pair(composerLayout, ViewCompat.getTransitionName(composerLayout) ?: ""))
        }
    }

    override fun onVideoMessageClicked(messageVideoContent: MessageVideoContent, mediaData: VideoContentRenderer.Data, view: View) {
        navigator.openMediaViewer(
                activity = requireActivity(),
                roomId = roomDetailArgs.roomId,
                mediaData = mediaData,
                view = view
        ) { pairs ->
            pairs.add(Pair(roomToolbar, ViewCompat.getTransitionName(roomToolbar) ?: ""))
            pairs.add(Pair(composerLayout, ViewCompat.getTransitionName(composerLayout) ?: ""))
        }
    }

//    override fun onFileMessageClicked(eventId: String, messageFileContent: MessageFileContent) {
//        val isEncrypted = messageFileContent.encryptedFileInfo != null
//        val action = RoomDetailAction.DownloadOrOpen(eventId, messageFileContent, isEncrypted)
//        // We need WRITE_EXTERNAL permission
// //        if (!isEncrypted || checkPermissions(PERMISSIONS_FOR_WRITING_FILES, this, PERMISSION_REQUEST_CODE_DOWNLOAD_FILE)) {
//            showSnackWithMessage(getString(R.string.downloading_file, messageFileContent.getFileName()))
//            roomDetailViewModel.handle(action)
// //        } else {
// //            roomDetailViewModel.pendingAction = action
// //        }
//    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (allGranted(grantResults)) {
            when (requestCode) {
                SAVE_ATTACHEMENT_REQUEST_CODE           -> {
                    sharedActionViewModel.pendingAction?.let {
                        handleActions(it)
                        sharedActionViewModel.pendingAction = null
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
                AUDIO_CALL_PERMISSION_REQUEST_CODE      -> {
                    if (onPermissionResultAudioIpCall(requireContext(), grantResults)) {
                        (roomDetailViewModel.pendingAction as? RoomDetailAction.StartCall)?.let {
                            roomDetailViewModel.pendingAction = null
                            roomDetailViewModel.handle(it)
                        }
                    }
                }
                VIDEO_CALL_PERMISSION_REQUEST_CODE      -> {
                    if (onPermissionResultVideoIpCall(requireContext(), grantResults)) {
                        (roomDetailViewModel.pendingAction as? RoomDetailAction.StartCall)?.let {
                            roomDetailViewModel.pendingAction = null
                            roomDetailViewModel.handle(it)
                        }
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

//    override fun onAudioMessageClicked(messageAudioContent: MessageAudioContent) {
//        vectorBaseActivity.notImplemented("open audio file")
//    }

    override fun onLoadMore(direction: Timeline.Direction) {
        roomDetailViewModel.handle(RoomDetailAction.LoadMoreTimelineEvents(direction))
    }

    override fun onEventCellClicked(informationData: MessageInformationData, messageContent: Any?, view: View) {
        when (messageContent) {
            is MessageVerificationRequestContent -> {
                roomDetailViewModel.handle(RoomDetailAction.ResumeVerification(informationData.eventId, null))
            }
            is MessageWithAttachmentContent      -> {
                val action = RoomDetailAction.DownloadOrOpen(informationData.eventId, messageContent)
                roomDetailViewModel.handle(action)
            }
            is EncryptedEventContent             -> {
                roomDetailViewModel.handle(RoomDetailAction.TapOnFailedToDecrypt(informationData.eventId))
            }
        }
    }

    override fun onEventLongClicked(informationData: MessageInformationData, messageContent: Any?, view: View): Boolean {
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

    private fun onShareActionClicked(action: EventSharedAction.Share) {
        session.fileService().downloadFile(
                downloadMode = FileService.DownloadMode.FOR_EXTERNAL_SHARE,
                id = action.eventId,
                fileName = action.messageContent.body,
                mimeType = action.messageContent.mimeType,
                url = action.messageContent.getFileUrl(),
                elementToDecrypt = action.messageContent.encryptedFileInfo?.toElementToDecrypt(),
                callback = object : MatrixCallback<File> {
                    override fun onSuccess(data: File) {
                        if (isAdded) {
                            shareMedia(requireContext(), data, getMimeTypeFromUri(requireContext(), data.toUri()))
                        }
                    }
                }
        )
    }

    private fun onSaveActionClicked(action: EventSharedAction.Save) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && !checkPermissions(PERMISSIONS_FOR_WRITING_FILES, this, SAVE_ATTACHEMENT_REQUEST_CODE)) {
            sharedActionViewModel.pendingAction = action
            return
        }
        session.fileService().downloadFile(
                downloadMode = FileService.DownloadMode.FOR_EXTERNAL_SHARE,
                id = action.eventId,
                fileName = action.messageContent.body,
                mimeType = action.messageContent.mimeType,
                url = action.messageContent.getFileUrl(),
                elementToDecrypt = action.messageContent.encryptedFileInfo?.toElementToDecrypt(),
                callback = object : MatrixCallback<File> {
                    override fun onSuccess(data: File) {
                        if (isAdded) {
                            saveMedia(
                                    context = requireContext(),
                                    file = data,
                                    title = action.messageContent.body,
                                    mediaMimeType = action.messageContent.mimeType ?: getMimeTypeFromUri(requireContext(), data.toUri()),
                                    notificationUtils = notificationUtils
                            )
                        }
                    }
                }
        )
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
                promptConfirmationToRedactEvent(action)
            }
            is EventSharedAction.Share                      -> {
                onShareActionClicked(action)
            }
            is EventSharedAction.Save                       -> {
                onSaveActionClicked(action)
            }
            is EventSharedAction.ViewEditHistory            -> {
                onEditedDecorationClicked(action.messageInformationData)
            }
            is EventSharedAction.ViewSource                 -> {
                JSonViewerDialog.newInstance(
                        action.content,
                        -1,
                        createJSonViewerStyleProvider(colorProvider)
                ).show(childFragmentManager, "JSON_VIEWER")
            }
            is EventSharedAction.ViewDecryptedSource        -> {
                JSonViewerDialog.newInstance(
                        action.content,
                        -1,
                        createJSonViewerStyleProvider(colorProvider)
                ).show(childFragmentManager, "JSON_VIEWER")
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
                val permalink = session.permalinkService().createPermalink(roomDetailArgs.roomId, action.eventId)
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
                action.senderId?.let { askConfirmationToIgnoreUser(it) }
            }
            is EventSharedAction.OnUrlClicked               -> {
                onUrlClicked(action.url, action.title)
            }
            is EventSharedAction.OnUrlLongClicked           -> {
                onUrlLongClicked(action.url)
            }
            is EventSharedAction.ReRequestKey               -> {
                roomDetailViewModel.handle(RoomDetailAction.ReRequestKeys(action.eventId))
            }
            is EventSharedAction.UseKeyBackup               -> {
                context?.let {
                    startActivity(KeysBackupRestoreActivity.intent(it))
                }
            }
        }
    }

    private fun askConfirmationToIgnoreUser(senderId: String) {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.room_participants_action_ignore_title)
                .setMessage(R.string.room_participants_action_ignore_prompt_msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.room_participants_action_ignore) { _, _ ->
                    roomDetailViewModel.handle(RoomDetailAction.IgnoreUser(senderId))
                }
                .show()
                .withColoredButton(DialogInterface.BUTTON_POSITIVE)
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
        if (composerLayout.isVisible) {
            composerLayout.composerEditText.showKeyboard(andRequestFocus = true)
        }
    }

    private fun showSnackWithMessage(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(requireView(), message, duration).show()
    }

    private fun showDialogWithMessage(message: String) {
        AlertDialog.Builder(requireContext())
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), null)
                .show()
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
            AttachmentTypeSelectorView.Type.CAMERA  -> attachmentsHelper.openCamera(this)
            AttachmentTypeSelectorView.Type.FILE    -> attachmentsHelper.selectFile(this)
            AttachmentTypeSelectorView.Type.GALLERY -> attachmentsHelper.selectGallery(this)
            AttachmentTypeSelectorView.Type.AUDIO   -> attachmentsHelper.selectAudio(this)
            AttachmentTypeSelectorView.Type.CONTACT -> attachmentsHelper.selectContact(this)
            AttachmentTypeSelectorView.Type.STICKER -> roomDetailViewModel.handle(RoomDetailAction.SelectStickerAttachment)
        }.exhaustive
    }

// AttachmentsHelper.Callback

    override fun onContentAttachmentsReady(attachments: List<ContentAttachmentData>) {
        if (roomDetailViewModel.preventAttachmentPreview) {
            roomDetailViewModel.preventAttachmentPreview = false
            roomDetailViewModel.handle(RoomDetailAction.SendMedia(attachments, false))
        } else {
            val grouped = attachments.toGroupedContentAttachmentData()
            if (grouped.notPreviewables.isNotEmpty()) {
                // Send the not previewable attachments right now (?)
                roomDetailViewModel.handle(RoomDetailAction.SendMedia(grouped.notPreviewables, false))
            }
            if (grouped.previewables.isNotEmpty()) {
                val intent = AttachmentsPreviewActivity.newIntent(requireContext(), AttachmentsPreviewArgs(grouped.previewables))
                startActivityForResult(intent, AttachmentsPreviewActivity.REQUEST_CODE)
            }
        }
    }

    override fun onAttachmentsProcessFailed() {
        roomDetailViewModel.preventAttachmentPreview = false
        Toast.makeText(requireContext(), R.string.error_attachment, Toast.LENGTH_SHORT).show()
    }

    override fun onContactAttachmentReady(contactAttachment: ContactAttachment) {
        super.onContactAttachmentReady(contactAttachment)
        val formattedContact = contactAttachment.toHumanReadable()
        roomDetailViewModel.handle(RoomDetailAction.SendMessage(formattedContact, false))
    }

    private fun onViewWidgetsClicked() {
        RoomWidgetsBottomSheet.newInstance()
                .show(childFragmentManager, "ROOM_WIDGETS_BOTTOM_SHEET")
    }

    override fun onTapToReturnToCall() {
        sharedCallActionViewModel.activeCall.value?.let { call ->
            VectorCallActivity.newIntent(
                    context = requireContext(),
                    callId = call.callId,
                    roomId = call.roomId,
                    otherUserId = call.otherUserId,
                    isIncomingCall = !call.isOutgoing,
                    isVideoCall = call.isVideoCall,
                    mode = null
            ).let {
                startActivity(it)
            }
        }
    }
}
