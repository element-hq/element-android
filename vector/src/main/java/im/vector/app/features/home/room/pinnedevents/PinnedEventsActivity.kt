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
import im.vector.app.databinding.ActivityPinnedEventsBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.TimelineFragment
import im.vector.app.features.home.room.detail.arguments.TimelineArgs
import im.vector.app.features.home.room.pinnedmessages.arguments.PinnedEventsTimelineArgs
import im.vector.lib.core.utils.compat.getParcelableCompat
import javax.inject.Inject

@AndroidEntryPoint
class PinnedEventsActivity : VectorBaseActivity<ActivityPinnedEventsBinding>() {

    @Inject lateinit var avatarRenderer: AvatarRenderer

    override fun getBinding() = ActivityPinnedEventsBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initFragment()
    }

    private fun initFragment() {
        if (isFirstCreation()) {
            val args = getPinnedEventsTimelineArgs()
            if (args == null) {
                finish()
            } else {
                initPinnedEventsTimelineFragment(args)
            }
        }
    }

    private fun initPinnedEventsTimelineFragment(pinnedEventsTimelineArgs: PinnedEventsTimelineArgs) =
            replaceFragment(
                    views.pinnedEventsActivityFragmentContainer,
                    TimelineFragment::class.java,
                    TimelineArgs(
                            roomId = pinnedEventsTimelineArgs.roomId,
                            pinnedEventsTimelineArgs = pinnedEventsTimelineArgs
                    )
            )

    private fun getPinnedEventsTimelineArgs(): PinnedEventsTimelineArgs? = intent?.extras?.getParcelableCompat(PINNED_EVENTS_TIMELINE_ARGS)

    companion object {
        const val PINNED_EVENTS_TIMELINE_ARGS = "PINNED_EVENTS_TIMELINE_ARGS"

        fun newIntent(
                context: Context,
                pinnedEventsTimelineArgs: PinnedEventsTimelineArgs?,
        ): Intent {
            return Intent(context, PinnedEventsActivity::class.java).apply {
                putExtra(PINNED_EVENTS_TIMELINE_ARGS, pinnedEventsTimelineArgs)
            }
        }
    }
}
