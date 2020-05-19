/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.riotx.features.roomprofile.uploads.files

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.epoxy.VisibilityState
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.errorWithRetryItem
import im.vector.riotx.core.epoxy.loadingItem
import im.vector.riotx.core.epoxy.noResultItem
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.roomprofile.uploads.RoomUploadsViewState
import javax.inject.Inject

class UploadsFileController @Inject constructor(
        private val errorFormatter: ErrorFormatter,
        private val stringProvider: StringProvider
) : TypedEpoxyController<RoomUploadsViewState>() {

    interface Listener {
        fun onRetry()
        fun loadMore()
        fun onOpenClicked(event: Event)
        fun onDownloadClicked(event: Event)
        fun onShareClicked(event: Event)
    }

    var listener: Listener? = null

    init {
        setData(null)
    }

    override fun buildModels(data: RoomUploadsViewState?) {
        data ?: return

        if (data.fileEvents.isEmpty()) {
            when (data.asyncEventsRequest) {
                is Loading -> {
                    loadingItem {
                        id("loading")
                    }
                }
                is Fail    -> {
                    errorWithRetryItem {
                        id("error")
                        text(errorFormatter.toHumanReadable(data.asyncEventsRequest.error))
                        listener { listener?.onRetry() }
                    }
                }
                is Success -> {
                    if (data.hasMore) {
                        // We need to load more items
                        listener?.loadMore()
                        loadingItem {
                            id("loading")
                        }
                    } else {
                        noResultItem {
                            id("noResult")
                            text(stringProvider.getString(R.string.uploads_files_no_result))
                        }
                    }
                }
            }
        } else {
            buildFileItems(data.fileEvents)

            if (data.hasMore) {
                loadingItem {
                    id("loadMore")
                    onVisibilityStateChanged { _, _, visibilityState ->
                        if (visibilityState == VisibilityState.VISIBLE) {
                            listener?.loadMore()
                        }
                    }
                }
            }
        }
    }

    private fun buildFileItems(fileEvents: List<Event>) {
        fileEvents.forEach {
            uploadsFileItem {
                id(it.eventId ?: "")
                title(it.getClearType())
                subtitle(it.getSenderKey())
                listener(object : UploadsFileItem.Listener {
                    override fun onItemClicked() {
                        listener?.onOpenClicked(it)
                    }

                    override fun onDownloadClicked() {
                        listener?.onDownloadClicked(it)
                    }

                    override fun onShareClicked() {
                        listener?.onShareClicked(it)
                    }
                })
            }
        }
    }
}
