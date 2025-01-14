/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.widget

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import im.vector.app.R
import im.vector.app.databinding.MotionNotifsFabMenuMergeBinding
import im.vector.lib.strings.CommonStrings

class NotifsFabMenuView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : MotionLayout(context, attrs, defStyleAttr) {

    private val views: MotionNotifsFabMenuMergeBinding

    var listener: Listener? = null

    init {
        inflate(context, R.layout.motion_notifs_fab_menu_merge, this)
        views = MotionNotifsFabMenuMergeBinding.bind(this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        listOf(views.createRoomItemChat, views.createRoomItemChatLabel)
                .forEach {
                    it.setOnClickListener {
                        closeFabMenu()
                        listener?.fabCreateDirectChat()
                    }
                }
        listOf(views.createRoomItemGroup, views.createRoomItemGroupLabel)
                .forEach {
                    it.setOnClickListener {
                        closeFabMenu()
                        listener?.fabOpenRoomDirectory()
                    }
                }

        views.createRoomTouchGuard.setOnClickListener {
            closeFabMenu()
        }
    }

    override fun transitionToEnd() {
        super.transitionToEnd()

        views.createRoomButton.contentDescription = context.getString(CommonStrings.a11y_create_menu_close)
    }

    override fun transitionToStart() {
        super.transitionToStart()

        views.createRoomButton.contentDescription = context.getString(CommonStrings.a11y_create_menu_open)
    }

    fun show() {
        isVisible = true
        views.createRoomButton.show()
    }

    fun hide() {
        views.createRoomButton.hide(object : FloatingActionButton.OnVisibilityChangedListener() {
            override fun onHidden(fab: FloatingActionButton?) {
                super.onHidden(fab)
                isVisible = false
            }
        })
    }

    private fun closeFabMenu() {
        transitionToStart()
    }

    fun onBackPressed(): Boolean {
        if (currentState == R.id.constraint_set_fab_menu_open) {
            closeFabMenu()
            return true
        }

        return false
    }

    interface Listener {
        fun fabCreateDirectChat()
        fun fabOpenRoomDirectory()
    }
}
