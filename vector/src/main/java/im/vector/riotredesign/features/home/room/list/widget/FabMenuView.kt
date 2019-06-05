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

package im.vector.riotredesign.features.home.room.list.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.transition.ChangeTransform
import androidx.transition.Transition
import androidx.transition.TransitionManager
import im.vector.riotredesign.R
import im.vector.riotredesign.core.animations.ANIMATION_DURATION_SHORT
import im.vector.riotredesign.core.animations.SimpleTransitionListener
import im.vector.riotredesign.core.animations.VectorFullTransitionSet
import kotlinx.android.synthetic.main.merge_fab_menu_view.view.*

class FabMenuView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                            defStyleAttr: Int = 0) : ConstraintLayout(context, attrs, defStyleAttr) {

    var listener: Listener? = null

    private var isFabMenuOpened = false

    init {
        inflate(context, R.layout.merge_fab_menu_view, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        // Collapse
        ConstraintSet().also {
            it.clone(context, R.layout.constraint_set_fab_menu_close)
            it.applyTo(this)
        }

        createRoomItemChat.isVisible = false
        createRoomItemChatLabel.isVisible = false
        createRoomItemGroup.isVisible = false
        createRoomItemGroupLabel.isVisible = false
        // Collapse end


        createRoomButton.setOnClickListener {
            toggleFabMenu()
        }

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

    fun show() {
        createRoomButton.show()
    }

    fun hide() {
        createRoomButton.hide()
    }

    private fun openFabMenu() {
        if (isFabMenuOpened) {
            return
        }

        toggleFabMenu()
    }

    private fun closeFabMenu() {
        if (!isFabMenuOpened) {
            return
        }

        toggleFabMenu()
    }

    private fun toggleFabMenu() {
        isFabMenuOpened = !isFabMenuOpened

        TransitionManager.beginDelayedTransition(parent as? ViewGroup ?: this,
                VectorFullTransitionSet().apply {
                    duration = ANIMATION_DURATION_SHORT
                    ChangeTransform()
                    addListener(object : SimpleTransitionListener() {
                        override fun onTransitionEnd(transition: Transition) {
                            // Hide the view after the transition for a better visual effect
                            createRoomItemChat.isVisible = isFabMenuOpened
                            createRoomItemChatLabel.isVisible = isFabMenuOpened
                            createRoomItemGroup.isVisible = isFabMenuOpened
                            createRoomItemGroupLabel.isVisible = isFabMenuOpened
                        }
                    })
                })

        if (isFabMenuOpened) {
            // Animate manually the rotation for a better effect
            createRoomButton.animate().setDuration(ANIMATION_DURATION_SHORT).rotation(135f)


            ConstraintSet().also {
                it.clone(context, R.layout.constraint_set_fab_menu_open)
                it.applyTo(this)
            }
        } else {
            createRoomButton.animate().setDuration(ANIMATION_DURATION_SHORT).rotation(0f)

            ConstraintSet().also {
                it.clone(context, R.layout.constraint_set_fab_menu_close)
                it.applyTo(this)
            }
        }
    }

    fun onBackPressed(): Boolean {
        if (isFabMenuOpened) {
            closeFabMenu()
            return true
        }

        return false
    }

    interface Listener {
        fun createDirectChat()
        fun openRoomDirectory()
    }

}