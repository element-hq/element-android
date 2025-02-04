/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.threads.list.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asFlow
import androidx.paging.PagedList
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.extensions.toAnalyticsInteraction
import im.vector.app.features.analytics.plan.Interaction
import im.vector.app.features.home.room.threads.list.views.ThreadListFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.threads.FetchThreadsResult
import org.matrix.android.sdk.api.session.room.threads.ThreadFilter
import org.matrix.android.sdk.api.session.room.threads.model.ThreadSummary
import org.matrix.android.sdk.api.session.threads.ThreadTimelineEvent
import org.matrix.android.sdk.flow.flow

class ThreadListViewModel @AssistedInject constructor(
        @Assisted val initialState: ThreadListViewState,
        private val analyticsTracker: AnalyticsTracker,
        private val session: Session,
) : VectorViewModel<ThreadListViewState, ThreadListViewActions, ThreadListViewEvents>(initialState) {

    private val room = session.getRoom(initialState.roomId)

    private val defaultPagedListConfig = PagedList.Config.Builder()
            .setPageSize(20)
            .setInitialLoadSizeHint(40)
            .setEnablePlaceholders(false)
            .setPrefetchDistance(10)
            .build()

    private var nextBatchId: String? = null
    private var hasReachedEnd: Boolean = false
    private var boundariesJob: Job? = null

    private var livePagedList: LiveData<PagedList<ThreadSummary>>? = null
    private val _threadsLivePagedList = MutableLiveData<PagedList<ThreadSummary>>()
    val threadsLivePagedList: LiveData<PagedList<ThreadSummary>> = _threadsLivePagedList
    private val internalPagedListObserver = Observer<PagedList<ThreadSummary>> {
        _threadsLivePagedList.postValue(it)
        setLoading(false)
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: ThreadListViewState): ThreadListViewModel
    }

    companion object : MavericksViewModelFactory<ThreadListViewModel, ThreadListViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: ThreadListViewState): ThreadListViewModel {
            val fragment: ThreadListFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.threadListViewModelFactory.create(state)
        }
    }

    init {
        fetchAndObserveThreads()
    }

    override fun handle(action: ThreadListViewActions) {
        when (action) {
            ThreadListViewActions.TryAgain -> handleTryAgain()
        }
    }

    private fun handleTryAgain() {
        viewModelScope.launch {
            fetchNextPage()
        }
    }

    /**
     * Observing thread list with respect to homeserver capabilities.
     */
    private fun fetchAndObserveThreads() {
        when (session.homeServerCapabilitiesService().getHomeServerCapabilities().canUseThreading) {
            true -> {
                setLoading(true)
                observeThreadSummaries()
            }
            false -> observeThreadsList()
        }
    }

    /**
     * Observing thread summaries when homeserver support threading.
     */
    private fun observeThreadSummaries() = withState { state ->
        viewModelScope.launch {
            nextBatchId = null
            hasReachedEnd = false

            livePagedList?.removeObserver(internalPagedListObserver)

            room?.threadsService()
                    ?.getPagedThreadsList(state.shouldFilterThreads, defaultPagedListConfig)?.let { result ->
                        livePagedList = result.livePagedList

                        livePagedList?.observeForever(internalPagedListObserver)

                        boundariesJob = result.liveBoundaries.asFlow()
                                .onEach {
                                    if (it.endLoaded) {
                                        if (!hasReachedEnd) {
                                            fetchNextPage()
                                        }
                                    }
                                }
                                .launchIn(viewModelScope)
                    }

            setLoading(true)
            fetchNextPage()
        }
    }

    /**
     * Observing thread list when homeserver do not support threading.
     */
    private fun observeThreadsList() {
        room?.flow()
                ?.liveThreadList()
                ?.map { room.threadsLocalService().mapEventsWithEdition(it) }
                ?.map {
                    it.map { threadRootEvent ->
                        val isParticipating = room.threadsLocalService().isUserParticipatingInThread(threadRootEvent.eventId)
                        ThreadTimelineEvent(threadRootEvent, isParticipating)
                    }
                }
                ?.flowOn(room.coroutineDispatchers.io)
                ?.execute { asyncThreads ->
                    copy(rootThreadEventList = asyncThreads)
                }
    }

    private fun setLoading(isLoading: Boolean) {
        setState {
            copy(isLoading = isLoading)
        }
    }

    fun canHomeserverUseThreading() = session.homeServerCapabilitiesService().getHomeServerCapabilities().canUseThreading

    fun applyFiltering(shouldFilterThreads: Boolean) {
        analyticsTracker.capture(Interaction.Name.MobileThreadListFilterItem.toAnalyticsInteraction())
        setState {
            copy(shouldFilterThreads = shouldFilterThreads)
        }

        fetchAndObserveThreads()
    }

    private suspend fun fetchNextPage() {
        val filter = when (awaitState().shouldFilterThreads) {
            true -> ThreadFilter.PARTICIPATED
            false -> ThreadFilter.ALL
        }
        try {
            room?.threadsService()?.fetchThreadList(
                    nextBatchId = nextBatchId,
                    limit = defaultPagedListConfig.pageSize,
                    filter = filter,
            )?.let { result ->
                when (result) {
                    is FetchThreadsResult.ReachedEnd -> {
                        hasReachedEnd = true
                    }
                    is FetchThreadsResult.ShouldFetchMore -> {
                        nextBatchId = result.nextBatch
                    }
                }
            }
        } catch (throwable: Throwable) {
            _viewEvents.post(ThreadListViewEvents.ShowError(throwable))
        }
    }
}
