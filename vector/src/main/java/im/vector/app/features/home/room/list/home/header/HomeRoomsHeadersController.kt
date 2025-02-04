/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home.header

import android.content.res.Resources
import android.util.TypedValue
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.epoxy.Carousel
import com.airbnb.epoxy.CarouselModelBuilder
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.carousel
import com.google.android.material.color.MaterialColors
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.FirstItemUpdatedObserver
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.list.RoomListListener
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class HomeRoomsHeadersController @Inject constructor(
        val stringProvider: StringProvider,
        private val avatarRenderer: AvatarRenderer,
        resources: Resources,
        private val analyticsTracker: AnalyticsTracker,
) : EpoxyController() {

    private var data: RoomsHeadersData = RoomsHeadersData()

    var onFilterChangedListener: ((HomeRoomFilter) -> Unit)? = null
    var recentsRoomListener: RoomListListener? = null
    var invitesClickListener: (() -> Unit)? = null

    private var carousel: Carousel? = null

    private var carouselAdapterObserver: FirstItemUpdatedObserver? = null

    private val recentsHPadding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4f,
            resources.displayMetrics
    ).toInt()

    private val recentsTopPadding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            12f,
            resources.displayMetrics
    ).toInt()

    override fun buildModels() {
        val host = this
        if (data.invitesCount != 0) {
            addInviteCounter(host.invitesClickListener, data.invitesCount)
        }

        data.recents?.let {
            addRecents(host, it)
        }

        host.data.filtersList?.let {
            addRoomFilterHeaderItem(
                    filterChangedListener = host.onFilterChangedListener,
                    filtersList = it,
                    currentFilter = host.data.currentFilter,
                    analyticsTracker = analyticsTracker)
        }
    }

    private fun addInviteCounter(invitesClickListener: (() -> Unit)?, invitesCount: Int) {
        inviteCounterItem {
            id("invites_counter")
            invitesCount(invitesCount)
            listener { invitesClickListener?.invoke() }
        }
    }

    private fun addRecents(host: HomeRoomsHeadersController, recents: List<RoomSummary>) {
        carousel {
            id("recents_carousel")
            padding(
                    Carousel.Padding(
                            host.recentsHPadding,
                            host.recentsTopPadding,
                            host.recentsHPadding,
                            0,
                            0,
                    )
            )
            onBind { _, view, _ ->
                host.carousel = view
                host.unsubscribeAdapterObserver()
                host.subscribeAdapterObserver()

                val colorSurface = MaterialColors.getColor(view, im.vector.lib.ui.styles.R.attr.vctr_toolbar_background)
                view.setBackgroundColor(colorSurface)
            }

            onUnbind { _, _ ->
                host.carousel = null
                host.unsubscribeAdapterObserver()
            }

            withModelsFrom(recents) { roomSummary ->
                val onClick = host.recentsRoomListener?.let { it::onRoomClicked }
                val onLongClick = host.recentsRoomListener?.let { it::onRoomLongClicked }

                RecentRoomItem_()
                        .id(roomSummary.roomId)
                        .avatarRenderer(host.avatarRenderer)
                        .matrixItem(roomSummary.toMatrixItem())
                        .unreadNotificationCount(roomSummary.notificationCount)
                        .showHighlighted(roomSummary.highlightCount > 0)
                        .itemLongClickListener { _ -> onLongClick?.invoke(roomSummary) ?: false }
                        .itemClickListener { onClick?.invoke(roomSummary) }
            }
        }
    }

    private fun unsubscribeAdapterObserver() {
        carouselAdapterObserver?.let { observer ->
            try {
                carousel?.adapter?.unregisterAdapterDataObserver(observer)
                carouselAdapterObserver = null
            } catch (e: IllegalStateException) {
                // do nothing
            }
        }
    }

    private fun subscribeAdapterObserver() {
        (carousel?.layoutManager as? LinearLayoutManager)?.let { layoutManager ->
            carouselAdapterObserver = FirstItemUpdatedObserver(layoutManager) {
                carousel?.post {
                    layoutManager.scrollToPosition(0)
                }
            }.also { observer ->
                try {
                    carousel?.adapter?.registerAdapterDataObserver(observer)
                } catch (e: IllegalStateException) {
                    // do nothing
                }
            }
        }
    }

    private fun addRoomFilterHeaderItem(
            filterChangedListener: ((HomeRoomFilter) -> Unit)?,
            filtersList: List<HomeRoomFilter>,
            currentFilter: HomeRoomFilter?,
            analyticsTracker: AnalyticsTracker,
    ) {
        roomFilterHeaderItem {
            id("filter_header")
            filtersData(filtersList)
            selectedFilter(currentFilter)
            onFilterChangedListener(filterChangedListener)
            analyticsTracker(analyticsTracker)
        }
    }

    fun submitData(data: RoomsHeadersData) {
        this.data = data
        requestModelBuild()
    }
}

private inline fun <T> CarouselModelBuilder.withModelsFrom(
        items: List<T>,
        modelBuilder: (T) -> EpoxyModel<*>
) {
    models(items.map { modelBuilder(it) })
}
