/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.threads.list.viewmodel

import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.paging.PagedListEpoxyController
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.utils.createUIHandler
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.format.DisplayableEventFormatter
import im.vector.app.features.home.room.threads.list.model.ThreadListItem_
import org.matrix.android.sdk.api.session.room.threads.model.ThreadSummary
import org.matrix.android.sdk.api.session.threads.ThreadNotificationState
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.api.util.toMatrixItemOrNull
import javax.inject.Inject

class ThreadListPagedController @Inject constructor(
        private val avatarRenderer: AvatarRenderer,
        private val dateFormatter: VectorDateFormatter,
        private val displayableEventFormatter: DisplayableEventFormatter,
) : PagedListEpoxyController<ThreadSummary>(
        // Important it must match the PageList builder notify Looper
        modelBuildingHandler = createUIHandler()
) {

    var listener: Listener? = null

    override fun buildItemModel(currentPosition: Int, item: ThreadSummary?): EpoxyModel<*> {
        if (item == null) {
            throw java.lang.NullPointerException()
        }
        val host = this
        val date = dateFormatter.format(item.latestEvent?.originServerTs, DateFormatKind.ROOM_LIST)
        val lastMessageFormatted = item.let {
            displayableEventFormatter.formatThreadSummary(
                    event = it.latestEvent,
                    latestEdition = it.threadEditions.latestThreadEdition
            ).toString()
        }
        val rootMessageFormatted = item.let {
            displayableEventFormatter.formatThreadSummary(
                    event = it.rootEvent,
                    latestEdition = it.threadEditions.rootThreadEdition
            ).toString()
        }

        return ThreadListItem_()
                .id(item.rootEvent?.eventId)
                .avatarRenderer(host.avatarRenderer)
                .matrixItem(item.rootThreadSenderInfo.toMatrixItem())
                .title(item.rootThreadSenderInfo.displayName.orEmpty())
                .date(date)
                .rootMessageDeleted(item.rootEvent?.isRedacted() ?: false)
                // TODO refactor notifications that with the new thread summary
                .threadNotificationState(item.rootEvent?.threadDetails?.threadNotificationState ?: ThreadNotificationState.NO_NEW_MESSAGE)
                .rootMessage(rootMessageFormatted)
                .lastMessage(lastMessageFormatted)
                .lastMessageCounter(item.numberOfThreads.toString())
                .lastMessageMatrixItem(item.latestThreadSenderInfo.toMatrixItemOrNull())
                .itemClickListener {
                    host.listener?.onThreadSummaryClicked(item)
                }
    }

    interface Listener {
        fun onThreadSummaryClicked(threadSummary: ThreadSummary)
    }
}
