/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.roomdirectory.roompreview

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.roomdirectory.RoomDirectoryData
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.room.model.roomdirectory.PublicRoom
import org.matrix.android.sdk.api.util.MatrixItem
import timber.log.Timber

@Parcelize
data class RoomPreviewData(
        val roomId: String,
        val eventId: String? = null,
        val roomName: String? = null,
        val roomAlias: String? = null,
        val roomType: String? = null,
        val topic: String? = null,
        val numJoinedMembers: Int? = null,
        val worldReadable: Boolean = false,
        val avatarUrl: String? = null,
        val homeServers: List<String> = emptyList(),
        val peekFromServer: Boolean = false,
        val buildTask: Boolean = false,
        val fromEmailInvite: PermalinkData.RoomEmailInviteLink? = null
) : Parcelable {
    val matrixItem: MatrixItem
        get() = MatrixItem.RoomItem(roomId, roomName ?: roomAlias, avatarUrl)
}

@AndroidEntryPoint
class RoomPreviewActivity : VectorBaseActivity<ActivitySimpleBinding>() {

    companion object {
        private const val ARG = "ARG"

        fun newIntent(context: Context, roomPreviewData: RoomPreviewData): Intent {
            return Intent(context, RoomPreviewActivity::class.java).apply {
                putExtra(ARG, roomPreviewData)
            }
        }

        fun newIntent(context: Context, publicRoom: PublicRoom, roomDirectoryData: RoomDirectoryData): Intent {
            val roomPreviewData = RoomPreviewData(
                    roomId = publicRoom.roomId,
                    roomName = publicRoom.name,
                    roomAlias = publicRoom.getPrimaryAlias(),
                    topic = publicRoom.topic,
                    numJoinedMembers = publicRoom.numJoinedMembers,
                    worldReadable = publicRoom.worldReadable,
                    avatarUrl = publicRoom.avatarUrl,
                    homeServers = listOfNotNull(roomDirectoryData.homeServer)
            )
            return newIntent(context, roomPreviewData)
        }
    }

    override fun getBinding() = ActivitySimpleBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun initUiAndData() {
        if (isFirstCreation()) {
            val args = intent.getParcelableExtra<RoomPreviewData>(ARG)

            if (args?.worldReadable == true) {
                // TODO Room preview: Note: M does not recommend to use /events anymore, so for now we just display the room preview
                // TODO the same way if it was not world readable
                Timber.d("just display the room preview the same way if it was not world readable")
                addFragment(views.simpleFragmentContainer, RoomPreviewNoPreviewFragment::class.java, args)
            } else {
                addFragment(views.simpleFragmentContainer, RoomPreviewNoPreviewFragment::class.java, args)
            }
        }
    }
}
