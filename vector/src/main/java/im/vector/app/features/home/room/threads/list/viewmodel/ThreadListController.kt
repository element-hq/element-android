/*
 * Copyright 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.threads.list.viewmodel

import com.airbnb.epoxy.EpoxyController
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.format.DisplayableEventFormatter
import im.vector.app.features.home.room.threads.list.model.threadListItem
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.threads.ThreadNotificationState
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class ThreadListController @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val dateFormatter: VectorDateFormatter,
        private val displayableEventFormatter: DisplayableEventFormatter,
) : EpoxyController() {

    var listener: Listener? = null

    private var viewState: ThreadListViewState? = null

    fun update(viewState: ThreadListViewState) {
        this.viewState = viewState
        requestModelBuild()
    }

    override fun buildModels() {
        val safeViewState = viewState ?: return
        val host = this
        safeViewState.rootThreadEventList.invoke()
                ?.filter {
                    if (safeViewState.shouldFilterThreads) {
                        it.isParticipating
                    } else {
                        true
                    }
                }?.map {
                    it.timelineEvent
                }
                ?.forEach { timelineEvent ->
                    val date = dateFormatter.format(timelineEvent.root.threadDetails?.lastMessageTimestamp, DateFormatKind.ROOM_LIST)
                    val lastRootThreadEdition = timelineEvent.root.threadDetails?.lastRootThreadEdition
                    val lastMessageFormatted = timelineEvent.root.threadDetails?.threadSummaryLatestEvent.let {
                        displayableEventFormatter.formatThreadSummary(
                                event = it,
                        ).toString()
                    }
                    val rootMessageFormatted = timelineEvent.root.let {
                        displayableEventFormatter.formatThreadSummary(
                                event = it,
                                latestEdition = lastRootThreadEdition
                        ).toString()
                    }
                    threadListItem {
                        id(timelineEvent.eventId)
                        avatarRenderer(host.avatarRenderer)
                        matrixItem(timelineEvent.senderInfo.toMatrixItem())
                        title(timelineEvent.senderInfo.displayName.orEmpty())
                        date(date)
                        rootMessageDeleted(timelineEvent.root.isRedacted())
                        threadNotificationState(timelineEvent.root.threadDetails?.threadNotificationState ?: ThreadNotificationState.NO_NEW_MESSAGE)
                        rootMessage(rootMessageFormatted)
                        lastMessage(lastMessageFormatted)
                        lastMessageCounter(timelineEvent.root.threadDetails?.numberOfThreads.toString())
                        lastMessageMatrixItem(timelineEvent.root.threadDetails?.threadSummarySenderInfo?.toMatrixItem())
                        itemClickListener {
                            host.listener?.onThreadListClicked(timelineEvent)
                        }
                    }
                }
    }

    interface Listener {
        fun onThreadListClicked(timelineEvent: TimelineEvent)
    }
}
