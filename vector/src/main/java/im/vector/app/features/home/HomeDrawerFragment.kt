/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.observeK
import im.vector.app.core.extensions.replaceChildFragment
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.BuildMeta
import im.vector.app.core.utils.startSharePlainTextIntent
import im.vector.app.databinding.FragmentHomeDrawerBinding
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.permalink.PermalinkFactory
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.settings.VectorSettingsActivity
import im.vector.app.features.spaces.SpaceListFragment
import im.vector.app.features.usercode.UserCodeActivity
import im.vector.app.features.workers.signout.SignOutUiWorker
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

@AndroidEntryPoint
class HomeDrawerFragment :
        VectorBaseFragment<FragmentHomeDrawerBinding>() {

    @Inject lateinit var session: Session
    @Inject lateinit var vectorPreferences: VectorPreferences
    @Inject lateinit var avatarRenderer: AvatarRenderer
    @Inject lateinit var buildMeta: BuildMeta
    @Inject lateinit var permalinkFactory: PermalinkFactory

    private lateinit var sharedActionViewModel: HomeSharedActionViewModel

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeDrawerBinding {
        return FragmentHomeDrawerBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedActionViewModel = activityViewModelProvider.get(HomeSharedActionViewModel::class.java)

        if (savedInstanceState == null) {
            replaceChildFragment(R.id.homeDrawerGroupListContainer, SpaceListFragment::class.java)
        }
        session.userService().getUserLive(session.myUserId).observeK(viewLifecycleOwner) { optionalUser ->
            val user = optionalUser?.getOrNull()
            if (user != null) {
                avatarRenderer.render(user.toMatrixItem(), views.homeDrawerHeaderAvatarView)
                views.homeDrawerUsernameView.text = user.displayName
                views.homeDrawerUserIdView.text = user.userId
            }
        }
        // Profile
        views.homeDrawerHeader.debouncedClicks {
            sharedActionViewModel.post(HomeActivitySharedAction.CloseDrawer)
            navigator.openSettings(requireActivity(), directAccess = VectorSettingsActivity.EXTRA_DIRECT_ACCESS_GENERAL)
        }
        // Settings
        views.homeDrawerHeaderSettingsView.debouncedClicks {
            sharedActionViewModel.post(HomeActivitySharedAction.CloseDrawer)
            navigator.openSettings(requireActivity())
        }
        // Sign out
        views.homeDrawerHeaderSignoutView.debouncedClicks {
            sharedActionViewModel.post(HomeActivitySharedAction.CloseDrawer)
            SignOutUiWorker(requireActivity()).perform()
        }

        views.homeDrawerQRCodeButton.debouncedClicks {
            UserCodeActivity.newIntent(requireContext(), sharedActionViewModel.session.myUserId).let {
                val options =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                                requireActivity(),
                                views.homeDrawerHeaderAvatarView,
                                ViewCompat.getTransitionName(views.homeDrawerHeaderAvatarView) ?: ""
                        )
                startActivity(it, options.toBundle())
            }
        }

        views.homeDrawerInviteFriendButton.debouncedClicks {
            permalinkFactory.createPermalinkOfCurrentUser()?.let { permalink ->
                analyticsTracker.screen(MobileScreen(screenName = MobileScreen.ScreenName.InviteFriends))
                val text = getString(CommonStrings.invite_friends_text, permalink)

                startSharePlainTextIntent(
                        context = requireContext(),
                        activityResultLauncher = null,
                        chooserTitle = getString(CommonStrings.invite_friends),
                        text = text,
                        extraTitle = getString(CommonStrings.invite_friends_rich_title)
                )
            }
        }

        // Debug menu
        views.homeDrawerHeaderDebugView.debouncedClicks {
            sharedActionViewModel.post(HomeActivitySharedAction.CloseDrawer)
            navigator.openDebug(requireActivity())
        }
    }

    override fun onResume() {
        super.onResume()
        views.homeDrawerHeaderDebugView.isVisible = buildMeta.isDebug && vectorPreferences.developerMode()
    }
}
