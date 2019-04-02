/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotredesign.features.home.room.detail

import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.RiotFragment
import kotlinx.android.synthetic.main.fragment_loading_room_detail.*

class LoadingRoomDetailFragment : RiotFragment() {

    companion object {

        fun newInstance(): LoadingRoomDetailFragment {
            return LoadingRoomDetailFragment()
        }
    }

    override fun getLayoutResId() = R.layout.fragment_loading_room_detail

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Glide.with(this)
                .load(R.drawable.riot_splash)
                .into(animatedLogoImageView)
    }


}