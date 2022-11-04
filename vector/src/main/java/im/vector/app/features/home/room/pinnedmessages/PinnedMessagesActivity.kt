/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.home.room.pinnedmessages

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityPinnedMessagesBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.TimelineFragment
import im.vector.app.features.home.room.detail.arguments.TimelineArgs
import im.vector.app.features.home.room.pinnedmessages.arguments.PinnedMessagesTimelineArgs
import im.vector.lib.core.utils.compat.getParcelableCompat
import javax.inject.Inject

@AndroidEntryPoint
class PinnedMessagesActivity : VectorBaseActivity<ActivityPinnedMessagesBinding>() {

    @Inject lateinit var avatarRenderer: AvatarRenderer

    override fun getBinding() = ActivityPinnedMessagesBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initFragment()
    }

    private fun initFragment() {
        if (isFirstCreation()) {
            when (val fragment = fragmentToNavigate()) {
                is DisplayFragment.PinnedMessagesTimeLine -> {
                    initPinnedMessagesTimelineFragment(fragment.pinnedMessagesTimelineArgs)
                }
                is DisplayFragment.ErrorFragment -> {
                    finish()
                }
            }
        }
    }

    private fun initPinnedMessagesTimelineFragment(pinnedMessagesTimelineArgs: PinnedMessagesTimelineArgs) =
            replaceFragment(
                    views.pinnedMessagesActivityFragmentContainer,
                    TimelineFragment::class.java,
                    TimelineArgs(
                            roomId = pinnedMessagesTimelineArgs.roomId,
                            pinnedMessagesTimelineArgs = pinnedMessagesTimelineArgs
                    )
            )

    /**
     * Determine in witch fragment we should navigate.
     */
    private fun fragmentToNavigate(): DisplayFragment {
        getPinnedMessagesTimelineArgs()?.let {
            return DisplayFragment.PinnedMessagesTimeLine(it)
        }
        return DisplayFragment.ErrorFragment
    }

    private fun getPinnedMessagesTimelineArgs(): PinnedMessagesTimelineArgs? = intent?.extras?.getParcelableCompat(PINNED_MESSAGES_TIMELINE_ARGS)

    companion object {
        const val PINNED_MESSAGES_TIMELINE_ARGS = "PINNED_MESSAGES_TIMELINE_ARGS"

        fun newIntent(
                context: Context,
                pinnedMessagesTimelineArgs: PinnedMessagesTimelineArgs?,
        ): Intent {
            return Intent(context, PinnedMessagesActivity::class.java).apply {
                putExtra(PINNED_MESSAGES_TIMELINE_ARGS, pinnedMessagesTimelineArgs)
            }
        }
    }

    sealed class DisplayFragment {
        data class PinnedMessagesTimeLine(val pinnedMessagesTimelineArgs: PinnedMessagesTimelineArgs) : DisplayFragment()
        object ErrorFragment : DisplayFragment()
    }
}
