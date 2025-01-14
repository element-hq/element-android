/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import im.vector.app.R
import im.vector.app.core.extensions.singletonEntryPoint
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem

class UserAvatarPreference : Preference {
    private var mAvatarView: ImageView? = null
    private var mLoadingProgressBar: ProgressBar? = null

    private var avatarRenderer: AvatarRenderer = context.singletonEntryPoint().avatarRenderer()

    private var userItem: MatrixItem.UserItem? = null

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
        refreshUi()
    }

    fun refreshAvatar(user: User) {
        userItem = user.toMatrixItem()
        refreshUi()
    }

    private fun refreshUi() {
        val safeUserItem = userItem ?: return
        mAvatarView?.let { avatarRenderer.render(safeUserItem, it) }
    }
}
