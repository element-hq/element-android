/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.people

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.Mavericks
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.GenericIdArgs
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleLoadingBinding
import im.vector.app.features.analytics.plan.ViewRoom
import im.vector.app.features.spaces.share.ShareSpaceBottomSheet
import im.vector.lib.core.utils.compat.getParcelableExtraCompat
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class SpacePeopleActivity : VectorBaseActivity<ActivitySimpleLoadingBinding>() {

    override fun getBinding() = ActivitySimpleLoadingBinding.inflate(layoutInflater)

    private lateinit var sharedActionViewModel: SpacePeopleSharedActionViewModel

    override fun initUiAndData() {
        super.initUiAndData()
        waitingView = views.waitingView.waitingView
    }

    override fun showWaitingView(text: String?) {
        hideKeyboard()
        views.waitingView.waitingStatusText.isGone = views.waitingView.waitingStatusText.text.isNullOrBlank()
        super.showWaitingView(text)
    }

    override fun hideWaitingView() {
        views.waitingView.waitingStatusText.text = null
        views.waitingView.waitingStatusText.isGone = true
        views.waitingView.waitingHorizontalProgress.progress = 0
        views.waitingView.waitingHorizontalProgress.isVisible = false
        super.hideWaitingView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = intent?.getParcelableExtraCompat<GenericIdArgs>(Mavericks.KEY_ARG)
        if (isFirstCreation()) {
            replaceFragment(
                    views.simpleFragmentContainer,
                    SpacePeopleFragment::class.java,
                    args
            )
        }

        sharedActionViewModel = viewModelProvider.get(SpacePeopleSharedActionViewModel::class.java)
        sharedActionViewModel
                .stream()
                .onEach { sharedAction ->
                    when (sharedAction) {
                        SpacePeopleSharedAction.Dismiss -> finish()
                        is SpacePeopleSharedAction.NavigateToRoom -> navigateToRooms(sharedAction)
                        SpacePeopleSharedAction.HideModalLoading -> hideWaitingView()
                        SpacePeopleSharedAction.ShowModalLoading -> {
                            showWaitingView(getString(CommonStrings.please_wait))
                        }
                        is SpacePeopleSharedAction.NavigateToInvite -> {
                            ShareSpaceBottomSheet.show(supportFragmentManager, sharedAction.spaceId)
                        }
                    }
                }.launchIn(lifecycleScope)
    }

    private fun navigateToRooms(action: SpacePeopleSharedAction.NavigateToRoom) {
        navigator.openRoom(
                context = this,
                roomId = action.roomId,
                trigger = ViewRoom.Trigger.MobileSpaceMembers
        )
        finish()
    }

    companion object {
        fun newIntent(context: Context, spaceId: String): Intent {
            return Intent(context, SpacePeopleActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, GenericIdArgs(spaceId))
            }
        }
    }
}
