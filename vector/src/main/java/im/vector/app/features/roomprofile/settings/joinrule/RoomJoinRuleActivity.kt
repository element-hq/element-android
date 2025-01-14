/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.settings.joinrule

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.extensions.commitTransaction
import im.vector.app.core.extensions.toMvRxBundle
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.utils.toast
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.home.room.detail.upgrade.MigrateRoomBottomSheet
import im.vector.app.features.roomprofile.RoomProfileArgs
import im.vector.app.features.roomprofile.settings.joinrule.advanced.RoomJoinRuleChooseRestrictedActions
import im.vector.app.features.roomprofile.settings.joinrule.advanced.RoomJoinRuleChooseRestrictedEvents
import im.vector.app.features.roomprofile.settings.joinrule.advanced.RoomJoinRuleChooseRestrictedFragment
import im.vector.app.features.roomprofile.settings.joinrule.advanced.RoomJoinRuleChooseRestrictedState
import im.vector.app.features.roomprofile.settings.joinrule.advanced.RoomJoinRuleChooseRestrictedViewModel
import im.vector.lib.core.utils.compat.getParcelableCompat

@AndroidEntryPoint
class RoomJoinRuleActivity : VectorBaseActivity<ActivitySimpleBinding>() {

    override fun getBinding() = ActivitySimpleBinding.inflate(layoutInflater)

    private lateinit var roomProfileArgs: RoomProfileArgs

    val viewModel: RoomJoinRuleChooseRestrictedViewModel by viewModel()

    override fun initUiAndData() {
        roomProfileArgs = intent?.extras?.getParcelableCompat(Mavericks.KEY_ARG) ?: return
        if (isFirstCreation()) {
            addFragment(
                    views.simpleFragmentContainer,
                    RoomJoinRuleFragment::class.java,
                    roomProfileArgs
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.onEach(RoomJoinRuleChooseRestrictedState::updatingStatus) {
            when (it) {
                Uninitialized -> {
                    // nop
                }
                is Loading -> {
                    views.simpleActivityWaitingView.isVisible = true
                }
                is Success -> {
                    withState(viewModel) { state ->
                        if (state.didSwitchToReplacementRoom) {
                            // we should navigate to new room
                            navigator.openRoom(this, state.roomId, null, true)
                        }
                        finish()
                    }
                }
                is Fail -> {
                    views.simpleActivityWaitingView.isVisible = false
                    toast(errorFormatter.toHumanReadable(it.error))
                }
            }
        }

        viewModel.observeViewEvents {
            when (it) {
                RoomJoinRuleChooseRestrictedEvents.NavigateToChooseRestricted -> navigateToChooseRestricted()
                is RoomJoinRuleChooseRestrictedEvents.NavigateToUpgradeRoom -> navigateToUpgradeRoom(it)
            }
        }

        supportFragmentManager.setFragmentResultListener(MigrateRoomBottomSheet.REQUEST_KEY, this) { _, bundle ->
            bundle.getString(MigrateRoomBottomSheet.BUNDLE_KEY_REPLACEMENT_ROOM)?.let { replacementRoomId ->
                viewModel.handle(RoomJoinRuleChooseRestrictedActions.SwitchToRoomAfterMigration(replacementRoomId))
            }
        }
    }

    private fun navigateToUpgradeRoom(events: RoomJoinRuleChooseRestrictedEvents.NavigateToUpgradeRoom) {
        MigrateRoomBottomSheet.newInstance(
                events.roomId,
                events.toVersion,
                MigrateRoomBottomSheet.MigrationReason.FOR_RESTRICTED,
                events.description
        ).show(supportFragmentManager, "migrate")
    }

    private fun navigateToChooseRestricted() {
        supportFragmentManager.commitTransaction {
            setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
            val tag = RoomJoinRuleChooseRestrictedFragment::class.simpleName
            replace(
                    views.simpleFragmentContainer.id,
                    RoomJoinRuleChooseRestrictedFragment::class.java,
                    this@RoomJoinRuleActivity.roomProfileArgs.toMvRxBundle(),
                    tag
            ).addToBackStack(tag)
        }
    }

    companion object {

        fun newIntent(context: Context, roomId: String): Intent {
            val roomProfileArgs = RoomProfileArgs(roomId)
            return Intent(context, RoomJoinRuleActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, roomProfileArgs)
            }
        }
    }
}
