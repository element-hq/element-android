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

package im.vector.app.features.roomprofile.uploads.files

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.epoxy.VisibilityState
import org.matrix.android.sdk.api.session.room.uploads.UploadEvent
import im.vector.app.R
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.roomprofile.uploads.RoomUploadsViewState
import javax.inject.Inject

class UploadsFileController @Inject constructor(
        private val stringProvider: StringProvider,
        private val dateFormatter: VectorDateFormatter
) : TypedEpoxyController<RoomUploadsViewState>() {

    interface Listener {
        fun loadMore()
        fun onOpenClicked(uploadEvent: UploadEvent)
        fun onDownloadClicked(uploadEvent: UploadEvent)
        fun onShareClicked(uploadEvent: UploadEvent)
    }

    var listener: Listener? = null

    private var idx = 0

    init {
        setData(null)
    }

    override fun buildModels(data: RoomUploadsViewState?) {
        data ?: return

        buildFileItems(data.fileEvents)

        if (data.hasMore) {
            loadingItem {
                // Always use a different id, because we can be notified several times of visibility state changed
                id("loadMore${idx++}")
                onVisibilityStateChanged { _, _, visibilityState ->
                    if (visibilityState == VisibilityState.VISIBLE) {
                        listener?.loadMore()
                    }
                }
            }
        }
    }

    private fun buildFileItems(fileEvents: List<UploadEvent>) {
        fileEvents.forEach { uploadEvent ->
            uploadsFileItem {
                id(uploadEvent.eventId)
                title(uploadEvent.contentWithAttachmentContent.body)
                subtitle(stringProvider.getString(R.string.uploads_files_subtitle,
                        uploadEvent.senderInfo.disambiguatedDisplayName,
                        dateFormatter.formatRelativeDateTime(uploadEvent.root.originServerTs)))
                listener(object : UploadsFileItem.Listener {
                    override fun onItemClicked() {
                        listener?.onOpenClicked(uploadEvent)
                    }

                    override fun onDownloadClicked() {
                        listener?.onDownloadClicked(uploadEvent)
                    }

                    override fun onShareClicked() {
                        listener?.onShareClicked(uploadEvent)
                    }
                })
            }
        }
    }
}
