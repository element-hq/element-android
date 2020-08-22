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
import android.view.View
import android.view.Window
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.error.fatalError
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.utils.toast
import im.vector.app.features.call.conference.JitsiCallViewModel
import im.vector.app.features.call.conference.VectorJitsiActivity
import im.vector.app.features.createdirect.CreateDirectRoomActivity
import im.vector.app.features.crypto.keysbackup.settings.KeysBackupManageActivity
import im.vector.app.features.crypto.keysbackup.setup.KeysBackupSetupActivity
import im.vector.app.features.crypto.recover.BootstrapBottomSheet
import im.vector.app.features.crypto.verification.SupportedVerificationMethodsProvider
import im.vector.app.features.crypto.verification.VerificationBottomSheet
import im.vector.app.features.debug.DebugMenuActivity
import im.vector.app.features.home.room.detail.RoomDetailActivity
import im.vector.app.features.home.room.detail.RoomDetailArgs
import im.vector.app.features.home.room.detail.widget.WidgetRequestCodes
import im.vector.app.features.home.room.filtered.FilteredRoomsActivity
import im.vector.app.features.invite.InviteUsersToRoomActivity
import im.vector.app.features.media.AttachmentData
import im.vector.app.features.media.BigImageViewerActivity
import im.vector.app.features.media.VectorAttachmentViewerActivity
import im.vector.app.features.pin.PinActivity
import im.vector.app.features.pin.PinArgs
import im.vector.app.features.pin.PinMode
import im.vector.app.features.roomdirectory.RoomDirectoryActivity
import im.vector.app.features.roomdirectory.createroom.CreateRoomActivity
import im.vector.app.features.roomdirectory.roompreview.RoomPreviewActivity
import im.vector.app.features.roommemberprofile.RoomMemberProfileActivity
import im.vector.app.features.roommemberprofile.RoomMemberProfileArgs
import im.vector.app.features.roomprofile.RoomProfileActivity
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.settings.VectorSettingsActivity
import im.vector.app.features.share.SharedData
import im.vector.app.features.terms.ReviewTermsActivity
import im.vector.app.features.widgets.WidgetActivity
import im.vector.app.features.widgets.WidgetArgsBuilder
import org.matrix.android.sdk.api.session.crypto.verification.IncomingSasVerificationTransaction
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoom
import org.matrix.android.sdk.api.session.room.model.thirdparty.RoomDirectoryData
import org.matrix.android.sdk.api.session.terms.TermsService
import org.matrix.android.sdk.api.session.widgets.model.Widget
import org.matrix.android.sdk.api.session.widgets.model.WidgetType
import org.matrix.android.sdk.api.util.MatrixItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultNavigator @Inject constructor(
        private val sessionHolder: ActiveSessionHolder,
        private val vectorPreferences: VectorPreferences,
        private val widgetArgsBuilder: WidgetArgsBuilder,
        private val supportedVerificationMethodsProvider: SupportedVerificationMethodsProvider
) : Navigator {

    override fun openRoom(context: Context, roomId: String, eventId: String?, buildTask: Boolean) {
        if (sessionHolder.getSafeActiveSession()?.getRoom(roomId) == null) {
            fatalError("Trying to open an unknown room $roomId", vectorPreferences.failFast())
            return
        }
        val args = RoomDetailArgs(roomId, eventId)
        val intent = RoomDetailActivity.newIntent(context, args)
        startActivity(context, intent, buildTask)
    }

    override fun performDeviceVerification(context: Context, otherUserId: String, sasTransactionId: String) {
        val session = sessionHolder.getSafeActiveSession() ?: return
        val tx = session.cryptoService().verificationService().getExistingTransaction(otherUserId, sasTransactionId)
                ?: return
        (tx as? IncomingSasVerificationTransaction)?.performAccept()
        if (context is VectorBaseActivity) {
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
        if (context is VectorBaseActivity) {
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
        if (context is VectorBaseActivity) {
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
        if (context is VectorBaseActivity) {
            VerificationBottomSheet.forSelfVerification(session)
                    .show(context.supportFragmentManager, VerificationBottomSheet.WAITING_SELF_VERIF_TAG)
        }
    }

    override fun upgradeSessionSecurity(context: Context, initCrossSigningOnly: Boolean) {
        if (context is VectorBaseActivity) {
            BootstrapBottomSheet.show(context.supportFragmentManager, initCrossSigningOnly, false)
        }
    }

    override fun openNotJoinedRoom(context: Context, roomIdOrAlias: String?, eventId: String?, buildTask: Boolean) {
        if (context is VectorBaseActivity) {
            context.notImplemented("Open not joined room")
        } else {
            context.toast(R.string.not_implemented)
        }
    }

    override fun openGroupDetail(groupId: String, context: Context, buildTask: Boolean) {
        if (context is VectorBaseActivity) {
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
        val args = RoomDetailArgs(roomId, null, sharedData)
        val intent = RoomDetailActivity.newIntent(activity, args)
        activity.startActivity(intent)
        activity.finish()
    }

    override fun openRoomPreview(context: Context, publicRoom: PublicRoom, roomDirectoryData: RoomDirectoryData) {
        val intent = RoomPreviewActivity.getIntent(context, publicRoom, roomDirectoryData)
        context.startActivity(intent)
    }

    override fun openRoomDirectory(context: Context, initialFilter: String) {
        val intent = RoomDirectoryActivity.getIntent(context, initialFilter)
        context.startActivity(intent)
    }

    override fun openCreateRoom(context: Context, initialName: String) {
        val intent = CreateRoomActivity.getIntent(context, initialName)
        context.startActivity(intent)
    }

    override fun openCreateDirectRoom(context: Context) {
        val intent = CreateDirectRoomActivity.getIntent(context)
        context.startActivity(intent)
    }

    override fun openInviteUsersToRoom(context: Context, roomId: String) {
        val intent = InviteUsersToRoomActivity.getIntent(context, roomId)
        context.startActivity(intent)
    }

    override fun openRoomsFiltering(context: Context) {
        val intent = FilteredRoomsActivity.newIntent(context)
        context.startActivity(intent)
    }

    override fun openSettings(context: Context, directAccess: Int) {
        val intent = VectorSettingsActivity.getIntent(context, directAccess)
        context.startActivity(intent)
    }

    override fun openDebug(context: Context) {
        context.startActivity(Intent(context, DebugMenuActivity::class.java))
    }

    override fun openKeysBackupSetup(context: Context, showManualExport: Boolean) {
        // if cross signing is enabled we should propose full 4S
        sessionHolder.getSafeActiveSession()?.let { session ->
            if (session.cryptoService().crossSigningService().canCrossSign() && context is VectorBaseActivity) {
                BootstrapBottomSheet.show(context.supportFragmentManager, initCrossSigningOnly = false, forceReset4S = false)
            } else {
                context.startActivity(KeysBackupSetupActivity.intent(context, showManualExport))
            }
        }
    }

    override fun openKeysBackupManager(context: Context) {
        context.startActivity(KeysBackupManageActivity.intent(context))
    }

    override fun openRoomProfile(context: Context, roomId: String) {
        context.startActivity(RoomProfileActivity.newIntent(context, roomId))
    }

    override fun openBigImageViewer(activity: Activity, sharedElement: View?, matrixItem: MatrixItem) {
        matrixItem.avatarUrl
                ?.takeIf { it.isNotBlank() }
                ?.let { avatarUrl ->
                    val intent = BigImageViewerActivity.newIntent(activity, matrixItem.getBestName(), avatarUrl)
                    val options = sharedElement?.let {
                        ActivityOptionsCompat.makeSceneTransitionAnimation(activity, it, ViewCompat.getTransitionName(it)
                                ?: "")
                    }
                    activity.startActivity(intent, options?.toBundle())
                }
    }

    override fun openTerms(fragment: Fragment, serviceType: TermsService.ServiceType, baseUrl: String, token: String?, requestCode: Int) {
        val intent = ReviewTermsActivity.intent(fragment.requireContext(), serviceType, baseUrl, token)
        fragment.startActivityForResult(intent, requestCode)
    }

    override fun openStickerPicker(fragment: Fragment, roomId: String, widget: Widget, requestCode: Int) {
        val widgetArgs = widgetArgsBuilder.buildStickerPickerArgs(roomId, widget)
        val intent = WidgetActivity.newIntent(fragment.requireContext(), widgetArgs)
        fragment.startActivityForResult(intent, WidgetRequestCodes.STICKER_PICKER_REQUEST_CODE)
    }

    override fun openIntegrationManager(fragment: Fragment, roomId: String, integId: String?, screen: String?) {
        val widgetArgs = widgetArgsBuilder.buildIntegrationManagerArgs(roomId, integId, screen)
        val intent = WidgetActivity.newIntent(fragment.requireContext(), widgetArgs)
        fragment.startActivityForResult(intent, WidgetRequestCodes.INTEGRATION_MANAGER_REQUEST_CODE)
    }

    override fun openRoomWidget(context: Context, roomId: String, widget: Widget, options: Map<String, Any>?) {
        if (widget.type is WidgetType.Jitsi) {
            val enableVideo = options?.get(JitsiCallViewModel.ENABLE_VIDEO_OPTION) == true
            context.startActivity(VectorJitsiActivity.newIntent(context, roomId = roomId, widgetId = widget.widgetId, enableVideo = enableVideo))
        } else {
            val widgetArgs = widgetArgsBuilder.buildRoomWidgetArgs(roomId, widget)
            context.startActivity(WidgetActivity.newIntent(context, widgetArgs))
        }
    }

    override fun openPinCode(fragment: Fragment, pinMode: PinMode, requestCode: Int) {
        val intent = PinActivity.newIntent(fragment.requireContext(), PinArgs(pinMode))
        fragment.startActivityForResult(intent, requestCode)
    }

    override fun openPinCode(activity: Activity, pinMode: PinMode, requestCode: Int) {
        val intent = PinActivity.newIntent(activity, PinArgs(pinMode))
        activity.startActivityForResult(intent, requestCode)
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

    private fun startActivity(context: Context, intent: Intent, buildTask: Boolean) {
        if (buildTask) {
            val stackBuilder = TaskStackBuilder.create(context)
            stackBuilder.addNextIntentWithParentStack(intent)
            stackBuilder.startActivities()
        } else {
            context.startActivity(intent)
        }
    }
}
