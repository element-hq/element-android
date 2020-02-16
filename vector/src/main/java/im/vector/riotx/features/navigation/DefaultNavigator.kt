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

package im.vector.riotx.features.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.app.TaskStackBuilder
import im.vector.matrix.android.api.session.crypto.sas.IncomingSasVerificationTransaction
import im.vector.matrix.android.api.session.crypto.sas.VerificationMethod
import im.vector.matrix.android.api.session.room.model.roomdirectory.PublicRoom
import im.vector.riotx.R
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.error.fatalError
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.core.utils.toast
import im.vector.riotx.features.createdirect.CreateDirectRoomActivity
import im.vector.riotx.features.crypto.keysbackup.settings.KeysBackupManageActivity
import im.vector.riotx.features.crypto.keysbackup.setup.KeysBackupSetupActivity
import im.vector.riotx.features.crypto.verification.VerificationBottomSheet
import im.vector.riotx.features.debug.DebugMenuActivity
import im.vector.riotx.features.home.room.detail.RoomDetailActivity
import im.vector.riotx.features.home.room.detail.RoomDetailArgs
import im.vector.riotx.features.home.room.filtered.FilteredRoomsActivity
import im.vector.riotx.features.roomdirectory.RoomDirectoryActivity
import im.vector.riotx.features.roomdirectory.createroom.CreateRoomActivity
import im.vector.riotx.features.roomdirectory.roompreview.RoomPreviewActivity
import im.vector.riotx.features.roommemberprofile.RoomMemberProfileActivity
import im.vector.riotx.features.roommemberprofile.RoomMemberProfileArgs
import im.vector.riotx.features.roomprofile.RoomProfileActivity
import im.vector.riotx.features.settings.VectorPreferences
import im.vector.riotx.features.settings.VectorSettingsActivity
import im.vector.riotx.features.share.SharedData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultNavigator @Inject constructor(
        private val sessionHolder: ActiveSessionHolder,
        private val vectorPreferences: VectorPreferences
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

    override fun performDeviceVerification(context: Context, otherUserId: String, sasTransationId: String) {
        val session = sessionHolder.getSafeActiveSession() ?: return
        val tx = session.cryptoService().verificationService().getExistingTransaction(otherUserId, sasTransationId) ?: return
        (tx as? IncomingSasVerificationTransaction)?.performAccept()
        if (context is VectorBaseActivity) {
            VerificationBottomSheet.withArgs(
                    roomId = null,
                    otherUserId = otherUserId,
                    transactionId = sasTransationId
            ).show(context.supportFragmentManager, "REQPOP")
        }
    }

    override fun requestSessionVerification(context: Context) {
        val session = sessionHolder.getSafeActiveSession() ?: return
        val pr = session.cryptoService().verificationService().requestKeyVerification(
                listOf(VerificationMethod.SAS, VerificationMethod.QR_CODE_SCAN, VerificationMethod.QR_CODE_SHOW),
                session.myUserId,
                session.cryptoService().getUserDevices(session.myUserId).map { it.deviceId })
        if (context is VectorBaseActivity) {
            VerificationBottomSheet.withArgs(
                    roomId = null,
                    otherUserId = session.myUserId,
                    transactionId = pr.transactionId
            ).show(context.supportFragmentManager, "REQPOP")
        }
    }

    override fun waitSessionVerification(context: Context) {
        val session = sessionHolder.getSafeActiveSession() ?: return
        if (context is VectorBaseActivity) {
            VerificationBottomSheet.withArgs(
                    roomId = null,
                    otherUserId = session.myUserId,
                    waitForIncomingRequest = true

            ).show(context.supportFragmentManager, VerificationBottomSheet.WAITING_SELF_VERIF_TAG)
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

    override fun openRoomForSharing(activity: Activity, roomId: String, sharedData: SharedData) {
        val args = RoomDetailArgs(roomId, null, sharedData)
        val intent = RoomDetailActivity.newIntent(activity, args)
        activity.startActivity(intent)
        activity.finish()
    }

    override fun openRoomPreview(publicRoom: PublicRoom, context: Context) {
        val intent = RoomPreviewActivity.getIntent(context, publicRoom)
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
        context.startActivity(KeysBackupSetupActivity.intent(context, showManualExport))
    }

    override fun openKeysBackupManager(context: Context) {
        context.startActivity(KeysBackupManageActivity.intent(context))
    }

    override fun openRoomProfile(context: Context, roomId: String) {
        context.startActivity(RoomProfileActivity.newIntent(context, roomId))
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
