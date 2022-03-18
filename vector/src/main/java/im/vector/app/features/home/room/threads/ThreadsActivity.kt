/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.threads

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentTransaction
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.addFragmentToBackstack
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityThreadsBinding
import im.vector.app.features.analytics.extensions.toAnalyticsInteraction
import im.vector.app.features.analytics.plan.Interaction
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.TimelineFragment
import im.vector.app.features.home.room.detail.arguments.TimelineArgs
import im.vector.app.features.home.room.threads.arguments.ThreadListArgs
import im.vector.app.features.home.room.threads.arguments.ThreadTimelineArgs
import im.vector.app.features.home.room.threads.list.views.ThreadListFragment
import javax.inject.Inject

@AndroidEntryPoint
class ThreadsActivity : VectorBaseActivity<ActivityThreadsBinding>() {

    @Inject
    lateinit var avatarRenderer: AvatarRenderer

//    private val roomThreadDetailFragment: RoomThreadDetailFragment?
//        get() {
//            return supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as? RoomThreadDetailFragment
//        }

    override fun getBinding() = ActivityThreadsBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initFragment()
    }

    private fun initFragment() {
        if (isFirstCreation()) {
            when (val fragment = fragmentToNavigate()) {
                is DisplayFragment.ThreadList     -> {
                    initThreadListFragment(fragment.threadListArgs)
                }
                is DisplayFragment.ThreadTimeLine -> {
                    initThreadTimelineFragment(fragment.threadTimelineArgs)
                }
                is DisplayFragment.ErrorFragment  -> {
                    finish()
                }
            }
        }
    }

    private fun initThreadListFragment(threadListArgs: ThreadListArgs) {
        replaceFragment(
                views.threadsActivityFragmentContainer,
                ThreadListFragment::class.java,
                threadListArgs)
    }

    private fun initThreadTimelineFragment(threadTimelineArgs: ThreadTimelineArgs) =
            replaceFragment(
                    views.threadsActivityFragmentContainer,
                    TimelineFragment::class.java,
                    TimelineArgs(
                            roomId = threadTimelineArgs.roomId,
                            eventId = getEventIdToNavigate(),
                            threadTimelineArgs = threadTimelineArgs
                    ))

    /**
     * This function is used to navigate to the selected thread timeline.
     * One usage of that is from the Threads Activity
     */
    fun navigateToThreadTimeline(threadTimelineArgs: ThreadTimelineArgs) {
        analyticsTracker.capture(Interaction.Name.MobileThreadListThreadItem.toAnalyticsInteraction())
        val commonOption: (FragmentTransaction) -> Unit = {
            it.setCustomAnimations(
                    R.anim.animation_slide_in_right,
                    R.anim.animation_slide_out_left,
                    R.anim.animation_slide_in_left,
                    R.anim.animation_slide_out_right)
        }
        addFragmentToBackstack(
                container = views.threadsActivityFragmentContainer,
                fragmentClass = TimelineFragment::class.java,
                params = TimelineArgs(
                        roomId = threadTimelineArgs.roomId,
                        threadTimelineArgs = threadTimelineArgs
                ),
                option = commonOption
        )
    }

    /**
     * Determine in witch fragment we should navigate
     */
    private fun fragmentToNavigate(): DisplayFragment {
        getThreadTimelineArgs()?.let {
            return DisplayFragment.ThreadTimeLine(it)
        }
        getThreadListArgs()?.let {
            return DisplayFragment.ThreadList(it)
        }
        return DisplayFragment.ErrorFragment
    }

    private fun getThreadTimelineArgs(): ThreadTimelineArgs? = intent?.extras?.getParcelable(THREAD_TIMELINE_ARGS)
    private fun getThreadListArgs(): ThreadListArgs? = intent?.extras?.getParcelable(THREAD_LIST_ARGS)
    private fun getEventIdToNavigate(): String? = intent?.extras?.getString(THREAD_EVENT_ID_TO_NAVIGATE)

    companion object {
        //        private val FRAGMENT_TAG = RoomThreadDetailFragment::class.simpleName
        const val THREAD_TIMELINE_ARGS = "THREAD_TIMELINE_ARGS"
        const val THREAD_EVENT_ID_TO_NAVIGATE = "THREAD_EVENT_ID_TO_NAVIGATE"
        const val THREAD_LIST_ARGS = "THREAD_LIST_ARGS"

        fun newIntent(
                context: Context,
                threadTimelineArgs: ThreadTimelineArgs?,
                threadListArgs: ThreadListArgs?,
                eventIdToNavigate: String? = null
        ): Intent {
            return Intent(context, ThreadsActivity::class.java).apply {
                putExtra(THREAD_TIMELINE_ARGS, threadTimelineArgs)
                putExtra(THREAD_EVENT_ID_TO_NAVIGATE, eventIdToNavigate)
                putExtra(THREAD_LIST_ARGS, threadListArgs)
            }
        }
    }

    sealed class DisplayFragment {
        data class ThreadList(val threadListArgs: ThreadListArgs) : DisplayFragment()
        data class ThreadTimeLine(val threadTimelineArgs: ThreadTimelineArgs) : DisplayFragment()
        object ErrorFragment : DisplayFragment()
    }
}
