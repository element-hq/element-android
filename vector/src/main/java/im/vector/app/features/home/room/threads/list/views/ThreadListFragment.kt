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

package im.vector.app.features.home.room.threads.list.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentThreadListBinding
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.animation.TimelineItemAnimator
import im.vector.app.features.home.room.threads.ThreadsActivity
import im.vector.app.features.home.room.threads.arguments.ThreadListArgs
import im.vector.app.features.home.room.threads.arguments.ThreadTimelineArgs
import im.vector.app.features.home.room.threads.list.viewmodel.ThreadListController
import im.vector.app.features.home.room.threads.list.viewmodel.ThreadListViewModel
import im.vector.app.features.home.room.threads.list.viewmodel.ThreadListViewState
import im.vector.app.features.rageshake.BugReporter
import im.vector.app.features.rageshake.ReportType
import org.matrix.android.sdk.api.session.room.threads.model.ThreadSummary
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.util.MatrixItem
import javax.inject.Inject

class ThreadListFragment @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val bugReporter: BugReporter,
        private val threadListController: ThreadListController,
        val threadListViewModelFactory: ThreadListViewModel.Factory
) : VectorBaseFragment<FragmentThreadListBinding>(),
        ThreadListController.Listener {

    private val threadListViewModel: ThreadListViewModel by fragmentViewModel()

    private val threadListArgs: ThreadListArgs by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentThreadListBinding {
        return FragmentThreadListBinding.inflate(inflater, container, false)
    }

    override fun getMenuRes() = R.menu.menu_thread_list

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.ThreadList
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_thread_list_filter -> {
                ThreadListBottomSheet().show(childFragmentManager, "Filtering")
                true
            }
            else                         -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        withState(threadListViewModel) { state ->
            when (threadListViewModel.canHomeserverUseThreading()) {
                true  -> menu.findItem(R.id.menu_thread_list_filter).isVisible = !state.threadSummaryList.invoke().isNullOrEmpty()
                false -> menu.findItem(R.id.menu_thread_list_filter).isVisible = !state.rootThreadEventList.invoke().isNullOrEmpty()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar()
        initTextConstants()
        initBetaFeedback()
        views.threadListRecyclerView.configureWith(threadListController, TimelineItemAnimator(), hasFixedSize = false)
        threadListController.listener = this
    }

    override fun onDestroyView() {
        views.threadListRecyclerView.cleanup()
        threadListController.listener = null
        super.onDestroyView()
    }

    private fun initToolbar() {
        setupToolbar(views.threadListToolbar).allowBack()
        renderToolbar()
    }

    private fun initTextConstants() {
        views.threadListEmptyNoticeTextView.text = String.format(
                resources.getString(R.string.thread_list_empty_notice),
                resources.getString(R.string.reply_in_thread))
    }

    private fun initBetaFeedback() {
        views.threadsFeedBackConstraintLayout.isVisible = resources.getBoolean(R.bool.feature_threads_beta_feedback_enabled)
        views.threadFeedbackDivider.isVisible = resources.getBoolean(R.bool.feature_threads_beta_feedback_enabled)
        views.threadsFeedBackConstraintLayout.debouncedClicks {
            bugReporter.openBugReportScreen(requireActivity(), reportType = ReportType.THREADS_BETA_FEEDBACK)
        }
    }
    override fun invalidate() = withState(threadListViewModel) { state ->
        invalidateOptionsMenu()
        renderEmptyStateIfNeeded(state)
        threadListController.update(state)
        renderLoaderIfNeeded(state)
    }

    private fun renderLoaderIfNeeded(state: ThreadListViewState) {
        views.threadListProgressBar.isVisible = state.isLoading
    }

    private fun renderToolbar() {
        views.includeThreadListToolbar.roomToolbarThreadConstraintLayout.isVisible = true
        val matrixItem = MatrixItem.RoomItem(threadListArgs.roomId, threadListArgs.displayName, threadListArgs.avatarUrl)
        avatarRenderer.render(matrixItem, views.includeThreadListToolbar.roomToolbarThreadImageView)
        views.includeThreadListToolbar.roomToolbarThreadShieldImageView.render(threadListArgs.roomEncryptionTrustLevel)
        views.includeThreadListToolbar.roomToolbarThreadTitleTextView.text = resources.getText(R.string.thread_list_title)
        views.includeThreadListToolbar.roomToolbarThreadSubtitleTextView.text = threadListArgs.displayName
    }

    override fun onThreadSummaryClicked(threadSummary: ThreadSummary) {
        val roomThreadDetailArgs = ThreadTimelineArgs(
                roomId = threadSummary.roomId,
                displayName = threadSummary.rootThreadSenderInfo.displayName,
                avatarUrl = threadSummary.rootThreadSenderInfo.avatarUrl,
                roomEncryptionTrustLevel = null,
                rootThreadEventId = threadSummary.rootEventId)
        (activity as? ThreadsActivity)?.navigateToThreadTimeline(roomThreadDetailArgs)
    }

    override fun onThreadListClicked(timelineEvent: TimelineEvent) {
        val threadTimelineArgs = ThreadTimelineArgs(
                roomId = timelineEvent.roomId,
                displayName = timelineEvent.senderInfo.displayName,
                avatarUrl = timelineEvent.senderInfo.avatarUrl,
                roomEncryptionTrustLevel = null,
                rootThreadEventId = timelineEvent.eventId)
        (activity as? ThreadsActivity)?.navigateToThreadTimeline(threadTimelineArgs)
    }

    private fun renderEmptyStateIfNeeded(state: ThreadListViewState) {
        when (threadListViewModel.canHomeserverUseThreading()) {
            true  -> views.threadListEmptyConstraintLayout.isVisible = state.threadSummaryList.invoke().isNullOrEmpty()
            false -> views.threadListEmptyConstraintLayout.isVisible = state.rootThreadEventList.invoke().isNullOrEmpty()
        }
    }
}
