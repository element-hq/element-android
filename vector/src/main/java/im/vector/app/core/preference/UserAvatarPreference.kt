/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.app.core.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import im.vector.app.R
import im.vector.app.core.extensions.vectorComponent
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem

class UserAvatarPreference : Preference {
    private var mAvatarView: ImageView? = null
    private var mLoadingProgressBar: ProgressBar? = null

    private var avatarRenderer: AvatarRenderer = context.vectorComponent().avatarRenderer()

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    init {
        widgetLayoutResource = R.layout.vector_settings_round_avatar
        // Set to false to remove the space when there is no icon
        isIconSpaceReserved = true
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        mAvatarView = holder.itemView.findViewById(R.id.settings_avatar)
        mLoadingProgressBar = holder.itemView.findViewById(R.id.avatar_update_progress_bar)
    }

    fun refreshAvatar(user: User) {
        mAvatarView?.let { avatarRenderer.render(user.toMatrixItem(), it) }
    }
}
