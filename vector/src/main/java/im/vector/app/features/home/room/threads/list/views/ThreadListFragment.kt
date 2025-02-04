/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.databinding.FragmentThreadListBinding
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.animation.TimelineItemAnimator
import im.vector.app.features.home.room.threads.ThreadsActivity
import im.vector.app.features.home.room.threads.arguments.ThreadListArgs
import im.vector.app.features.home.room.threads.arguments.ThreadTimelineArgs
import im.vector.app.features.home.room.threads.list.viewmodel.ThreadListController
import im.vector.app.features.home.room.threads.list.viewmodel.ThreadListPagedController
import im.vector.app.features.home.room.threads.list.viewmodel.ThreadListViewActions
import im.vector.app.features.home.room.threads.list.viewmodel.ThreadListViewEvents
import im.vector.app.features.home.room.threads.list.viewmodel.ThreadListViewModel
import im.vector.app.features.home.room.threads.list.viewmodel.ThreadListViewState
import im.vector.app.features.rageshake.BugReporter
import im.vector.app.features.rageshake.ReportType
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.failure.is400
import org.matrix.android.sdk.api.failure.is404
import org.matrix.android.sdk.api.session.room.threads.model.ThreadSummary
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.util.MatrixItem
import javax.inject.Inject

@AndroidEntryPoint
class ThreadListFragment :
        VectorBaseFragment<FragmentThreadListBinding>(),
        ThreadListPagedController.Listener,
        ThreadListController.Listener,
        VectorMenuProvider {

    @Inject lateinit var avatarRenderer: AvatarRenderer
    @Inject lateinit var bugReporter: BugReporter
    @Inject lateinit var threadListController: ThreadListPagedController
    @Inject lateinit var legacyThreadListController: ThreadListController
    @Inject lateinit var threadListViewModelFactory: ThreadListViewModel.Factory

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

    override fun handlePostCreateMenu(menu: Menu) {
        // We use a custom layout for this menu item, so we need to set a ClickListener
        menu.findItem(R.id.menu_thread_list_filter)?.let { menuItem ->
            menuItem.actionView?.debouncedClicks {
                handleMenuItemSelected(menuItem)
            }
        }
    }

    override fun handleMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_thread_list_filter -> {
                ThreadListBottomSheet().show(childFragmentManager, "Filtering")
                true
            }
            else -> false
        }
    }

    override fun handlePrepareMenu(menu: Menu) {
        withState(threadListViewModel) { state ->
            val filterIcon = menu.findItem(R.id.menu_thread_list_filter).actionView ?: return@withState
            val filterBadge = filterIcon.findViewById<View>(R.id.threadListFilterBadge)
            filterBadge.isVisible = state.shouldFilterThreads
            when (threadListViewModel.canHomeserverUseThreading()) {
                true -> menu.findItem(R.id.menu_thread_list_filter).isVisible = true
                false -> menu.findItem(R.id.menu_thread_list_filter).isVisible = !state.rootThreadEventList.invoke().isNullOrEmpty()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar()
        initTextConstants()
        initBetaFeedback()

        if (threadListViewModel.canHomeserverUseThreading()) {
            views.threadListRecyclerView.configureWith(threadListController, TimelineItemAnimator(), hasFixedSize = false)
            threadListController.listener = this

            threadListViewModel.threadsLivePagedList.observe(viewLifecycleOwner) { threadsList ->
                threadListController.submitList(threadsList)
            }
        } else {
            views.threadListRecyclerView.configureWith(legacyThreadListController, TimelineItemAnimator(), hasFixedSize = false)
            legacyThreadListController.listener = this
        }
        observeViewEvents()
    }

    private fun observeViewEvents() {
        threadListViewModel.observeViewEvents {
            when (it) {
                is ThreadListViewEvents.ShowError -> handleShowError(it)
            }
        }
    }

    private fun handleShowError(event: ThreadListViewEvents.ShowError) {
        val error = event.throwable
        MaterialAlertDialogBuilder(requireActivity())
                .setTitle(CommonStrings.dialog_title_error)
                .also {
                    if (error.is400() || error.is404()) {
                        // Outdated Homeserver
                        it.setMessage(CommonStrings.thread_list_not_available)
                        it.setPositiveButton(CommonStrings.ok) { _, _ ->
                            requireActivity().finish()
                        }
                    } else {
                        // Other error, can retry
                        // (Can happen on first request or on pagination request)
                        it.setMessage(errorFormatter.toHumanReadable(error))
                        it.setPositiveButton(CommonStrings.ok, null)
                        it.setNegativeButton(CommonStrings.global_retry) { _, _ ->
                            threadListViewModel.handle(ThreadListViewActions.TryAgain)
                        }
                    }
                }
                .show()
    }

    override fun onDestroyView() {
        views.threadListRecyclerView.cleanup()
        threadListController.listener = null
        legacyThreadListController.listener = null
        super.onDestroyView()
    }

    private fun initToolbar() {
        setupToolbar(views.threadListToolbar).allowBack()
        renderToolbar()
    }

    private fun initTextConstants() {
        views.threadListEmptyNoticeTextView.text = String.format(
                resources.getString(CommonStrings.thread_list_empty_notice),
                resources.getString(CommonStrings.reply_in_thread)
        )
    }

    private fun initBetaFeedback() {
        views.threadsFeedBackConstraintLayout.isVisible = resources.getBoolean(im.vector.app.config.R.bool.feature_threads_beta_feedback_enabled)
        views.threadFeedbackDivider.isVisible = resources.getBoolean(im.vector.app.config.R.bool.feature_threads_beta_feedback_enabled)
        views.threadsFeedBackConstraintLayout.debouncedClicks {
            bugReporter.openBugReportScreen(requireActivity(), reportType = ReportType.THREADS_BETA_FEEDBACK)
        }
    }

    override fun invalidate() = withState(threadListViewModel) { state ->
        invalidateOptionsMenu()
        renderEmptyStateIfNeeded(state)
        if (!threadListViewModel.canHomeserverUseThreading()) {
            legacyThreadListController.update(state)
        }
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
        views.includeThreadListToolbar.roomToolbarThreadTitleTextView.text = resources.getText(CommonStrings.thread_list_title)
        views.includeThreadListToolbar.roomToolbarThreadSubtitleTextView.text = threadListArgs.displayName
    }

    override fun onThreadSummaryClicked(threadSummary: ThreadSummary) {
        val roomThreadDetailArgs = ThreadTimelineArgs(
                roomId = threadListArgs.roomId,
                displayName = threadListArgs.displayName,
                avatarUrl = threadListArgs.avatarUrl,
                roomEncryptionTrustLevel = null,
                rootThreadEventId = threadSummary.rootEventId
        )
        (activity as? ThreadsActivity)?.navigateToThreadTimeline(roomThreadDetailArgs)
    }

    override fun onThreadListClicked(timelineEvent: TimelineEvent) {
        val threadTimelineArgs = ThreadTimelineArgs(
                roomId = threadListArgs.roomId,
                displayName = threadListArgs.displayName,
                avatarUrl = threadListArgs.avatarUrl,
                roomEncryptionTrustLevel = null,
                rootThreadEventId = timelineEvent.eventId
        )
        (activity as? ThreadsActivity)?.navigateToThreadTimeline(threadTimelineArgs)
    }

    private fun renderEmptyStateIfNeeded(state: ThreadListViewState) {
        when (threadListViewModel.canHomeserverUseThreading()) {
            true -> views.threadListEmptyConstraintLayout.isVisible = false
            false -> views.threadListEmptyConstraintLayout.isVisible = state.rootThreadEventList.invoke().isNullOrEmpty()
        }
    }
}
