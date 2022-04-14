/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.Window
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.AppStateHandler
import im.vector.app.R
import im.vector.app.RoomGroupingMethod
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.error.fatalError
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.utils.toast
import im.vector.app.features.VectorFeatures
import im.vector.app.features.VectorFeatures.OnboardingVariant
import im.vector.app.features.analytics.ui.consent.AnalyticsOptInActivity
import im.vector.app.features.call.conference.JitsiCallViewModel
import im.vector.app.features.call.conference.VectorJitsiActivity
import im.vector.app.features.call.transfer.CallTransferActivity
import im.vector.app.features.createdirect.CreateDirectRoomActivity
import im.vector.app.features.crypto.keysbackup.settings.KeysBackupManageActivity
import im.vector.app.features.crypto.keysbackup.setup.KeysBackupSetupActivity
import im.vector.app.features.crypto.recover.BootstrapBottomSheet
import im.vector.app.features.crypto.recover.SetupMode
import im.vector.app.features.crypto.verification.SupportedVerificationMethodsProvider
import im.vector.app.features.crypto.verification.VerificationBottomSheet
import im.vector.app.features.debug.DebugMenuActivity
import im.vector.app.features.devtools.RoomDevToolActivity
import im.vector.app.features.home.room.detail.RoomDetailActivity
import im.vector.app.features.home.room.detail.arguments.TimelineArgs
import im.vector.app.features.home.room.detail.search.SearchActivity
import im.vector.app.features.home.room.detail.search.SearchArgs
import im.vector.app.features.home.room.filtered.FilteredRoomsActivity
import im.vector.app.features.home.room.threads.ThreadsActivity
import im.vector.app.features.home.room.threads.arguments.ThreadListArgs
import im.vector.app.features.home.room.threads.arguments.ThreadTimelineArgs
import im.vector.app.features.invite.InviteUsersToRoomActivity
import im.vector.app.features.location.LocationData
import im.vector.app.features.location.LocationSharingActivity
import im.vector.app.features.location.LocationSharingArgs
import im.vector.app.features.location.LocationSharingMode
import im.vector.app.features.login.LoginActivity
import im.vector.app.features.login.LoginConfig
import im.vector.app.features.matrixto.MatrixToBottomSheet
import im.vector.app.features.media.AttachmentData
import im.vector.app.features.media.BigImageViewerActivity
import im.vector.app.features.media.VectorAttachmentViewerActivity
import im.vector.app.features.onboarding.OnboardingActivity
import im.vector.app.features.pin.PinActivity
import im.vector.app.features.pin.PinArgs
import im.vector.app.features.pin.PinMode
import im.vector.app.features.poll.PollMode
import im.vector.app.features.poll.create.CreatePollActivity
import im.vector.app.features.poll.create.CreatePollArgs
import im.vector.app.features.roomdirectory.RoomDirectoryActivity
import im.vector.app.features.roomdirectory.RoomDirectoryData
import im.vector.app.features.roomdirectory.createroom.CreateRoomActivity
import im.vector.app.features.roomdirectory.roompreview.RoomPreviewActivity
import im.vector.app.features.roomdirectory.roompreview.RoomPreviewData
import im.vector.app.features.roommemberprofile.RoomMemberProfileActivity
import im.vector.app.features.roommemberprofile.RoomMemberProfileArgs
import im.vector.app.features.roomprofile.RoomProfileActivity
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.settings.VectorSettingsActivity
import im.vector.app.features.share.SharedData
import im.vector.app.features.signout.soft.SoftLogoutActivity
import im.vector.app.features.spaces.InviteRoomSpaceChooserBottomSheet
import im.vector.app.features.spaces.SpaceExploreActivity
import im.vector.app.features.spaces.SpacePreviewActivity
import im.vector.app.features.spaces.manage.ManageType
import im.vector.app.features.spaces.manage.SpaceManageActivity
import im.vector.app.features.spaces.people.SpacePeopleActivity
import im.vector.app.features.terms.ReviewTermsActivity
import im.vector.app.features.widgets.WidgetActivity
import im.vector.app.features.widgets.WidgetArgsBuilder
import im.vector.app.space
import org.matrix.android.sdk.api.session.crypto.verification.IncomingSasVerificationTransaction
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoom
import org.matrix.android.sdk.api.session.terms.TermsService
import org.matrix.android.sdk.api.session.widgets.model.Widget
import org.matrix.android.sdk.api.session.widgets.model.WidgetType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultNavigator @Inject constructor(
        private val sessionHolder: ActiveSessionHolder,
        private val vectorPreferences: VectorPreferences,
        private val widgetArgsBuilder: WidgetArgsBuilder,
        private val appStateHandler: AppStateHandler,
        private val supportedVerificationMethodsProvider: SupportedVerificationMethodsProvider,
        private val features: VectorFeatures
) : Navigator {

    override fun openLogin(context: Context, loginConfig: LoginConfig?, flags: Int) {
        val intent = when (features.onboardingVariant()) {
            OnboardingVariant.LEGACY    -> LoginActivity.newIntent(context, loginConfig)
            OnboardingVariant.LOGIN_2,
            OnboardingVariant.FTUE_AUTH -> OnboardingActivity.newIntent(context, loginConfig)
        }
        intent.addFlags(flags)
        context.startActivity(intent)
    }

    override fun loginSSORedirect(context: Context, data: Uri?) {
        val intent = when (features.onboardingVariant()) {
            OnboardingVariant.LEGACY    -> LoginActivity.redirectIntent(context, data)
            OnboardingVariant.LOGIN_2,
            OnboardingVariant.FTUE_AUTH -> OnboardingActivity.redirectIntent(context, data)
        }
        context.startActivity(intent)
    }

    override fun softLogout(context: Context) {
        val intent = SoftLogoutActivity.newIntent(context)
        context.startActivity(intent)
    }

    override fun openRoom(
            context: Context,
            roomId: String,
            eventId: String?,
            buildTask: Boolean,
            isInviteAlreadyAccepted: Boolean
    ) {
        if (sessionHolder.getSafeActiveSession()?.getRoom(roomId) == null) {
            fatalError("Trying to open an unknown room $roomId", vectorPreferences.failFast())
            return
        }
        val args = TimelineArgs(roomId = roomId, eventId = eventId, isInviteAlreadyAccepted = isInviteAlreadyAccepted)
        val intent = RoomDetailActivity.newIntent(context, args)
        startActivity(context, intent, buildTask)
    }

    override fun switchToSpace(context: Context, spaceId: String, postSwitchSpaceAction: Navigator.PostSwitchSpaceAction) {
        if (sessionHolder.getSafeActiveSession()?.getRoomSummary(spaceId) == null) {
            fatalError("Trying to open an unknown space $spaceId", vectorPreferences.failFast())
            return
        }
        appStateHandler.setCurrentSpace(spaceId)
        when (postSwitchSpaceAction) {
            Navigator.PostSwitchSpaceAction.None                 -> {
                // go back to home if we are showing room details?
                // This is a bit ugly, but the navigator is supposed to know about the activity stack
                if (context is RoomDetailActivity) {
                    context.finish()
                }
            }
            Navigator.PostSwitchSpaceAction.OpenAddExistingRooms -> {
                startActivity(context, SpaceManageActivity.newIntent(context, spaceId, ManageType.AddRooms), false)
            }
            Navigator.PostSwitchSpaceAction.OpenRoomList -> {
                startActivity(context, SpaceExploreActivity.newIntent(context, spaceId), buildTask = false)
            }
            is Navigator.PostSwitchSpaceAction.OpenDefaultRoom   -> {
                val args = TimelineArgs(
                        postSwitchSpaceAction.roomId,
                        eventId = null,
                        openShareSpaceForId = spaceId.takeIf { postSwitchSpaceAction.showShareSheet }
                )
                val intent = RoomDetailActivity.newIntent(context, args)
                startActivity(context, intent, false)
            }
        }
    }

    override fun openSpacePreview(context: Context, spaceId: String) {
        startActivity(context, SpacePreviewActivity.newIntent(context, spaceId), false)
    }

    override fun performDeviceVerification(context: Context, otherUserId: String, sasTransactionId: String) {
        val session = sessionHolder.getSafeActiveSession() ?: return
        val tx = session.cryptoService().verificationService().getExistingTransaction(otherUserId, sasTransactionId)
                ?: return
        (tx as? IncomingSasVerificationTransaction)?.performAccept()
        if (context is AppCompatActivity) {
            VerificationBottomSheet.withArgs(
                    roomId = null,
                    otherUserId = otherUserId,
                    transactionId = sasTransactionId
            ).show(context.supportFragmentManager, "REQPOP")
        }
    }

    override fun requestSessionVerification(context: Context, otherSessionId: String) {
        val session = sessionHolder.getSafeActiveSession() ?: return
        val pr = session.cryptoService().verificationService().requestKeyVerification(
                supportedVerificationMethodsProvider.provide(),
                session.myUserId,
                listOf(otherSessionId)
        )
        if (context is AppCompatActivity) {
            VerificationBottomSheet.withArgs(
                    roomId = null,
                    otherUserId = session.myUserId,
                    transactionId = pr.transactionId
            ).show(context.supportFragmentManager, VerificationBottomSheet.WAITING_SELF_VERIF_TAG)
        }
    }

    override fun requestSelfSessionVerification(context: Context) {
        val session = sessionHolder.getSafeActiveSession() ?: return
        val otherSessions = session.cryptoService()
                .getCryptoDeviceInfo(session.myUserId)
                .filter { it.deviceId != session.sessionParams.deviceId }
                .map { it.deviceId }
        if (context is AppCompatActivity) {
            if (otherSessions.isNotEmpty()) {
                val pr = session.cryptoService().verificationService().requestKeyVerification(
                        supportedVerificationMethodsProvider.provide(),
                        session.myUserId,
                        otherSessions)
                VerificationBottomSheet.forSelfVerification(session, pr.transactionId ?: pr.localId)
                        .show(context.supportFragmentManager, VerificationBottomSheet.WAITING_SELF_VERIF_TAG)
            } else {
                VerificationBottomSheet.forSelfVerification(session)
                        .show(context.supportFragmentManager, VerificationBottomSheet.WAITING_SELF_VERIF_TAG)
            }
        }
    }

    override fun waitSessionVerification(context: Context) {
        val session = sessionHolder.getSafeActiveSession() ?: return
        if (context is AppCompatActivity) {
            VerificationBottomSheet.forSelfVerification(session)
                    .show(context.supportFragmentManager, VerificationBottomSheet.WAITING_SELF_VERIF_TAG)
        }
    }

    override fun upgradeSessionSecurity(context: Context, initCrossSigningOnly: Boolean) {
        if (context is AppCompatActivity) {
            BootstrapBottomSheet.show(
                    context.supportFragmentManager,
                    if (initCrossSigningOnly) SetupMode.CROSS_SIGNING_ONLY else SetupMode.NORMAL
            )
        }
    }

    override fun openGroupDetail(groupId: String, context: Context, buildTask: Boolean) {
        if (context is VectorBaseActivity<*>) {
            context.notImplemented("Open group detail")
        } else {
            context.toast(R.string.not_implemented)
        }
    }

    override fun openRoomMemberProfile(userId: String, roomId: String?, context: Context, buildTask: Boolean) {
        val args = RoomMemberProfileArgs(userId = userId, roomId = roomId)
        val intent = RoomMemberProfileActivity.newIntent(context, args)
        startActivity(context, intent, buildTask)
    }

    override fun openRoomForSharingAndFinish(activity: Activity, roomId: String, sharedData: SharedData) {
        val args = TimelineArgs(roomId, null, sharedData)
        val intent = RoomDetailActivity.newIntent(activity, args)
        activity.startActivity(intent)
        activity.finish()
    }

    override fun openRoomPreview(context: Context, publicRoom: PublicRoom, roomDirectoryData: RoomDirectoryData) {
        val intent = RoomPreviewActivity.newIntent(context, publicRoom, roomDirectoryData)
        context.startActivity(intent)
    }

    override fun openRoomPreview(context: Context, roomPreviewData: RoomPreviewData, fromEmailInviteLink: PermalinkData.RoomEmailInviteLink?) {
        val intent = RoomPreviewActivity.newIntent(context, roomPreviewData)
        context.startActivity(intent)
    }

    override fun openMatrixToBottomSheet(context: Context, link: String) {
        if (context is AppCompatActivity) {
            if (context !is MatrixToBottomSheet.InteractionListener) {
                fatalError("Caller context should implement MatrixToBottomSheet.InteractionListener", vectorPreferences.failFast())
            }
            // TODO check if there is already one??
            MatrixToBottomSheet.withLink(link)
                    .show(context.supportFragmentManager, "HA#MatrixToBottomSheet")
        }
    }

    override fun openRoomDirectory(context: Context, initialFilter: String) {
        when (val groupingMethod = appStateHandler.getCurrentRoomGroupingMethod()) {
            is RoomGroupingMethod.ByLegacyGroup -> {
                // TODO should open list of rooms of this group
                val intent = RoomDirectoryActivity.getIntent(context, initialFilter)
                context.startActivity(intent)
            }
            is RoomGroupingMethod.BySpace       -> {
                val selectedSpace = groupingMethod.space()
                if (selectedSpace == null) {
                    val intent = RoomDirectoryActivity.getIntent(context, initialFilter)
                    context.startActivity(intent)
                } else {
                    SpaceExploreActivity.newIntent(context, selectedSpace.roomId).let {
                        context.startActivity(it)
                    }
                }
            }
            null                                -> Unit
        }
    }

    override fun openCreateRoom(context: Context, initialName: String, openAfterCreate: Boolean) {
        val intent = CreateRoomActivity.getIntent(context = context, initialName = initialName, openAfterCreate = openAfterCreate)
        context.startActivity(intent)
    }

    override fun openCreateDirectRoom(context: Context) {
        val intent = when (val currentGroupingMethod = appStateHandler.getCurrentRoomGroupingMethod()) {
            is RoomGroupingMethod.ByLegacyGroup -> {
                CreateDirectRoomActivity.getIntent(context)
            }
            is RoomGroupingMethod.BySpace       -> {
                if (currentGroupingMethod.spaceSummary != null) {
                    SpacePeopleActivity.newIntent(context, currentGroupingMethod.spaceSummary.roomId)
                } else {
                    CreateDirectRoomActivity.getIntent(context)
                }
            }
            else                                -> null
        } ?: return
        context.startActivity(intent)
    }

    override fun openInviteUsersToRoom(context: Context, roomId: String) {
        when (val currentGroupingMethod = appStateHandler.getCurrentRoomGroupingMethod()) {
            is RoomGroupingMethod.ByLegacyGroup -> {
                val intent = InviteUsersToRoomActivity.getIntent(context, roomId)
                context.startActivity(intent)
            }
            is RoomGroupingMethod.BySpace       -> {
                if (currentGroupingMethod.spaceSummary != null) {
                    // let user decides if he does it from space or room
                    (context as? AppCompatActivity)?.supportFragmentManager?.let { fm ->
                        InviteRoomSpaceChooserBottomSheet.newInstance(
                                currentGroupingMethod.spaceSummary.roomId,
                                roomId,
                                object : InviteRoomSpaceChooserBottomSheet.InteractionListener {
                                    override fun inviteToSpace(spaceId: String) {
                                        val intent = InviteUsersToRoomActivity.getIntent(context, spaceId)
                                        context.startActivity(intent)
                                    }

                                    override fun inviteToRoom(roomId: String) {
                                        val intent = InviteUsersToRoomActivity.getIntent(context, roomId)
                                        context.startActivity(intent)
                                    }
                                }
                        ).show(fm, InviteRoomSpaceChooserBottomSheet::class.java.name)
                    }
                } else {
                    val intent = InviteUsersToRoomActivity.getIntent(context, roomId)
                    context.startActivity(intent)
                }
            }
            null                                -> Unit
        }
    }

    override fun openRoomsFiltering(context: Context) {
        val intent = FilteredRoomsActivity.newIntent(context)
        context.startActivity(intent)
    }

    override fun openSettings(context: Context, directAccess: Int) {
        val intent = VectorSettingsActivity.getIntent(context, directAccess)
        context.startActivity(intent)
    }

    override fun openSettings(context: Context, payload: SettingsActivityPayload) {
        val intent = VectorSettingsActivity.getIntent(context, payload)
        context.startActivity(intent)
    }

    override fun openDebug(context: Context) {
        context.startActivity(Intent(context, DebugMenuActivity::class.java))
    }

    override fun openKeysBackupSetup(context: Context, showManualExport: Boolean) {
        // if cross signing is enabled and trusted or not set up at all we should propose full 4S
        sessionHolder.getSafeActiveSession()?.let { session ->
            if (session.cryptoService().crossSigningService().getMyCrossSigningKeys() == null ||
                    session.cryptoService().crossSigningService().canCrossSign()) {
                (context as? AppCompatActivity)?.let {
                    BootstrapBottomSheet.show(it.supportFragmentManager, SetupMode.NORMAL)
                }
            } else {
                context.startActivity(KeysBackupSetupActivity.intent(context, showManualExport))
            }
        }
    }

    override fun open4SSetup(context: Context, setupMode: SetupMode) {
        if (context is AppCompatActivity) {
            BootstrapBottomSheet.show(context.supportFragmentManager, setupMode)
        }
    }

    override fun openKeysBackupManager(context: Context) {
        context.startActivity(KeysBackupManageActivity.intent(context))
    }

    override fun openRoomProfile(context: Context, roomId: String, directAccess: Int?) {
        context.startActivity(RoomProfileActivity.newIntent(context, roomId, directAccess))
    }

    override fun openBigImageViewer(activity: Activity, sharedElement: View?, mxcUrl: String?, title: String?) {
        mxcUrl
                ?.takeIf { it.isNotBlank() }
                ?.let { avatarUrl ->
                    val intent = BigImageViewerActivity.newIntent(activity, title, avatarUrl)
                    val options = sharedElement?.let {
                        ActivityOptionsCompat.makeSceneTransitionAnimation(activity, it, ViewCompat.getTransitionName(it) ?: "")
                    }
                    activity.startActivity(intent, options?.toBundle())
                }
    }

    override fun openAnalyticsOptIn(context: Context) {
        context.startActivity(Intent(context, AnalyticsOptInActivity::class.java))
    }

    override fun openTerms(context: Context,
                           activityResultLauncher: ActivityResultLauncher<Intent>,
                           serviceType: TermsService.ServiceType,
                           baseUrl: String,
                           token: String?) {
        val intent = ReviewTermsActivity.intent(context, serviceType, baseUrl, token)
        activityResultLauncher.launch(intent)
    }

    override fun openStickerPicker(context: Context,
                                   activityResultLauncher: ActivityResultLauncher<Intent>,
                                   roomId: String,
                                   widget: Widget) {
        val widgetArgs = widgetArgsBuilder.buildStickerPickerArgs(roomId, widget)
        val intent = WidgetActivity.newIntent(context, widgetArgs)
        activityResultLauncher.launch(intent)
    }

    override fun openIntegrationManager(context: Context,
                                        activityResultLauncher: ActivityResultLauncher<Intent>,
                                        roomId: String,
                                        integId: String?,
                                        screen: String?) {
        val widgetArgs = widgetArgsBuilder.buildIntegrationManagerArgs(roomId, integId, screen)
        val intent = WidgetActivity.newIntent(context, widgetArgs)
        activityResultLauncher.launch(intent)
    }

    override fun openRoomWidget(context: Context, roomId: String, widget: Widget, options: Map<String, Any>?) {
        if (widget.type is WidgetType.Jitsi) {
            // Jitsi SDK is now for API 23+
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.dialog_title_error)
                        .setMessage(R.string.error_jitsi_not_supported_on_old_device)
                        .setPositiveButton(R.string.ok, null)
                        .show()
            } else {
                val enableVideo = options?.get(JitsiCallViewModel.ENABLE_VIDEO_OPTION) == true
                context.startActivity(VectorJitsiActivity.newIntent(context, roomId = roomId, widgetId = widget.widgetId, enableVideo = enableVideo))
            }
        } else {
            val widgetArgs = widgetArgsBuilder.buildRoomWidgetArgs(roomId, widget)
            context.startActivity(WidgetActivity.newIntent(context, widgetArgs))
        }
    }

    override fun openPinCode(context: Context,
                             activityResultLauncher: ActivityResultLauncher<Intent>,
                             pinMode: PinMode) {
        val intent = PinActivity.newIntent(context, PinArgs(pinMode))
        activityResultLauncher.launch(intent)
    }

    override fun openMediaViewer(activity: Activity,
                                 roomId: String,
                                 mediaData: AttachmentData,
                                 view: View,
                                 inMemory: List<AttachmentData>,
                                 options: ((MutableList<Pair<View, String>>) -> Unit)?) {
        VectorAttachmentViewerActivity.newIntent(activity,
                mediaData,
                roomId,
                mediaData.eventId,
                inMemory,
                ViewCompat.getTransitionName(view)).let { intent ->
            val pairs = ArrayList<Pair<View, String>>()
            activity.window.decorView.findViewById<View>(android.R.id.statusBarBackground)?.let {
                pairs.add(Pair(it, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME))
            }
            activity.window.decorView.findViewById<View>(android.R.id.navigationBarBackground)?.let {
                pairs.add(Pair(it, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME))
            }

            pairs.add(Pair(view, ViewCompat.getTransitionName(view) ?: ""))
            options?.invoke(pairs)

            val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, *pairs.toTypedArray()).toBundle()
            activity.startActivity(intent, bundle)
        }
    }

    override fun openSearch(context: Context,
                            roomId: String,
                            roomDisplayName: String?,
                            roomAvatarUrl: String?) {
        val intent = SearchActivity.newIntent(context, SearchArgs(roomId, roomDisplayName, roomAvatarUrl))
        context.startActivity(intent)
    }

    override fun openDevTools(context: Context, roomId: String) {
        context.startActivity(RoomDevToolActivity.intent(context, roomId))
    }

    override fun openCallTransfer(
            context: Context,
            activityResultLauncher: ActivityResultLauncher<Intent>,
            callId: String
    ) {
        val intent = CallTransferActivity.newIntent(context, callId)
        activityResultLauncher.launch(intent)
    }

    override fun openCreatePoll(context: Context, roomId: String, editedEventId: String?, mode: PollMode) {
        val intent = CreatePollActivity.getIntent(
                context,
                CreatePollArgs(roomId = roomId, editedEventId = editedEventId, mode = mode)
        )
        context.startActivity(intent)
    }

    override fun openLocationSharing(context: Context,
                                     roomId: String,
                                     mode: LocationSharingMode,
                                     initialLocationData: LocationData?,
                                     locationOwnerId: String?) {
        val intent = LocationSharingActivity.getIntent(
                context,
                LocationSharingArgs(roomId = roomId, mode = mode, initialLocationData = initialLocationData, locationOwnerId = locationOwnerId)
        )
        context.startActivity(intent)
    }

    private fun startActivity(context: Context, intent: Intent, buildTask: Boolean) {
        if (buildTask) {
            val stackBuilder = TaskStackBuilder.create(context)
            stackBuilder.addNextIntentWithParentStack(intent)
            stackBuilder.startActivities()
        } else {
            context.startActivity(intent)
        }
    }

    override fun openThread(context: Context, threadTimelineArgs: ThreadTimelineArgs, eventIdToNavigate: String?) {
        context.startActivity(ThreadsActivity.newIntent(
                context = context,
                threadTimelineArgs = threadTimelineArgs,
                threadListArgs = null,
                eventIdToNavigate = eventIdToNavigate
        ))
    }

    override fun openThreadList(context: Context, threadTimelineArgs: ThreadTimelineArgs) {
        context.startActivity(ThreadsActivity.newIntent(
                context = context,
                threadTimelineArgs = null,
                threadListArgs = ThreadListArgs(
                        roomId = threadTimelineArgs.roomId,
                        displayName = threadTimelineArgs.displayName,
                        avatarUrl = threadTimelineArgs.avatarUrl,
                        roomEncryptionTrustLevel = threadTimelineArgs.roomEncryptionTrustLevel
                )))
    }
}
