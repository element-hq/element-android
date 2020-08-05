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

package im.vector.app.features.home.room.list.widget

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import im.vector.app.R
import kotlinx.android.synthetic.main.motion_fab_menu_merge.view.*

class FabMenuView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                            defStyleAttr: Int = 0) : MotionLayout(context, attrs, defStyleAttr) {

    var listener: Listener? = null

    init {
        inflate(context, R.layout.motion_fab_menu_merge, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        listOf(createRoomItemChat, createRoomItemChatLabel)
                .forEach {
                    it.setOnClickListener {
                        closeFabMenu()
                        listener?.createDirectChat()
                    }
                }
        listOf(createRoomItemGroup, createRoomItemGroupLabel)
                .forEach {
                    it.setOnClickListener {
                        closeFabMenu()
                        listener?.openRoomDirectory()
                    }
                }

        createRoomTouchGuard.setOnClickListener {
            closeFabMenu()
        }
    }

    override fun transitionToEnd() {
        super.transitionToEnd()

        createRoomButton.contentDescription = context.getString(R.string.a11y_create_menu_close)
    }

    override fun transitionToStart() {
        super.transitionToStart()

        createRoomButton.contentDescription = context.getString(R.string.a11y_create_menu_open)
    }

    fun show() {
        isVisible = true
        createRoomButton.show()
    }

    fun hide() {
        createRoomButton.hide(object : FloatingActionButton.OnVisibilityChangedListener() {
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
        fun createDirectChat()
        fun openRoomDirectory(initialFilter: String = "")
    }
}
