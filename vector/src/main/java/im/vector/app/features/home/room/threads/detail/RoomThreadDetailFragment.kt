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

package im.vector.app.features.home.room.threads.detail

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.args
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentRoomThreadDetailBinding
import im.vector.app.features.home.room.threads.detail.arguments.RoomThreadDetailArgs
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

class RoomThreadDetailFragment @Inject constructor(
        private val session: Session
) : VectorBaseFragment<FragmentRoomThreadDetailBinding>() {

    private val roomThreadDetailArgs: RoomThreadDetailArgs by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRoomThreadDetailBinding {
        return FragmentRoomThreadDetailBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.testTextVeiwddasda.text = "${roomThreadDetailArgs.eventId}  --  ${roomThreadDetailArgs.roomId}"
    }
}
