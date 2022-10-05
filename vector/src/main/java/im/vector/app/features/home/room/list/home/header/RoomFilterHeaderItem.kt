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

package im.vector.app.features.home.room.list.home.header

import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.google.android.material.tabs.TabLayout
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.plan.Interaction

@EpoxyModelClass
abstract class RoomFilterHeaderItem : VectorEpoxyModel<RoomFilterHeaderItem.Holder>(R.layout.item_home_filter_tabs) {

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var onFilterChangedListener: ((HomeRoomFilter) -> Unit)? = null

    @EpoxyAttribute
    var filtersData: List<HomeRoomFilter>? = null

    @EpoxyAttribute
    var selectedFilter: HomeRoomFilter? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var analyticsTracker: AnalyticsTracker? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        with(holder.tabLayout) {
            removeAllTabs()
            clearOnTabSelectedListeners()

            filtersData?.forEach { filter ->
                addTab(
                        newTab().setText(filter.titleRes).setTag(filter),
                        filter == (selectedFilter ?: HomeRoomFilter.ALL)
                )
            }

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    (tab?.tag as? HomeRoomFilter)?.let { filter ->
                        trackFilterChangeEvent(filter)
                        onFilterChangedListener?.invoke(filter)
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
                override fun onTabReselected(tab: TabLayout.Tab?) = Unit
            })
        }
    }

    private fun trackFilterChangeEvent(filter: HomeRoomFilter) {
        val interactionName = when (filter) {
            HomeRoomFilter.ALL -> Interaction.Name.MobileAllChatsFilterAll
            HomeRoomFilter.UNREADS -> Interaction.Name.MobileAllChatsFilterUnreads
            HomeRoomFilter.FAVOURITES -> Interaction.Name.MobileAllChatsFilterFavourites
            HomeRoomFilter.PEOPlE -> Interaction.Name.MobileAllChatsFilterPeople
        }

        analyticsTracker?.capture(
                Interaction(
                        index = null,
                        interactionType = null,
                        name = interactionName
                )
        )
    }

    override fun unbind(holder: Holder) {
        holder.tabLayout.clearOnTabSelectedListeners()
        super.unbind(holder)
    }

    class Holder : VectorEpoxyHolder() {
        val tabLayout by bind<TabLayout>(R.id.home_filter_tabs_tabs)
    }
}
