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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreference
import im.vector.app.R
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.group.Group

class VectorGroupPreference : SwitchPreference {

    private var mAvatarView: ImageView? = null

    private var mGroup: Group? = null
    private var mSession: Session? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val createdView = holder.itemView

        if (mAvatarView == null) {
            try {
                // insert the group avatar to the left
                val iconView = createdView.findViewById<ImageView>(android.R.id.icon)

                var iconViewParent = iconView.parent

                while (null != iconViewParent.parent) {
                    iconViewParent = iconViewParent.parent
                }

                val inflater = LayoutInflater.from(context)
                val layout = inflater.inflate(R.layout.vector_settings_round_group_avatar, (iconViewParent as LinearLayout), false) as FrameLayout
                mAvatarView = layout.findViewById(R.id.settings_round_group_avatar)

                val params = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.gravity = Gravity.CENTER
                layout.layoutParams = params
                iconViewParent.addView(layout, 0)
            } catch (e: Exception) {
                mAvatarView = null
            }
        }

        refreshAvatar()
    }

    /**
     * Init the group information
     *
     * @param group   the group
     * @param session the session
     */
    fun setGroup(group: Group, session: Session) {
        mGroup = group
        mSession = session

        refreshAvatar()
    }

    /**
     * Refresh the avatar
     */
    private fun refreshAvatar() {
        if (null != mAvatarView && null != mSession && null != mGroup) {
            // TODO
            // VectorUtils.loadGroupAvatar(context, session, mAvatarView, mGroup)
        }
    }
}
