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

package im.vector.app.core.ui.views

import android.content.Context
import android.graphics.Color
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.text.italic
import im.vector.app.R
import im.vector.app.core.error.ResourceLimitErrorFormatter
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.themes.ThemeUtils
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.session.events.model.Event
import kotlinx.android.synthetic.main.view_notification_area.view.*
import me.gujun.android.span.span
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import timber.log.Timber

/**
 * The view used to show some information about the room
 * It does have a unique render method
 */
class NotificationAreaView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    var delegate: Delegate? = null
    private var state: State = State.Initial

    init {
        setupView()
    }

    /**
     * This methods is responsible for rendering the view according to the newState
     *
     * @param newState the newState representing the view
     */
    fun render(newState: State) {
        if (newState == state) {
            Timber.d("State unchanged")
            return
        }
        Timber.d("Rendering $newState")
        cleanUp()
        state = newState
        when (newState) {
            is State.Default                    -> renderDefault()
            is State.Hidden                     -> renderHidden()
            is State.NoPermissionToPost         -> renderNoPermissionToPost()
            is State.Tombstone                  -> renderTombstone(newState)
            is State.ResourceLimitExceededError -> renderResourceLimitExceededError(newState)
        }
    }

    // PRIVATE METHODS ****************************************************************************************************************************************

    private fun setupView() {
        inflate(context, R.layout.view_notification_area, this)
        minimumHeight = DimensionConverter(resources).dpToPx(48)
    }

    private fun cleanUp() {
        roomNotificationMessage.setOnClickListener(null)
        roomNotificationIcon.setOnClickListener(null)
        setBackgroundColor(Color.TRANSPARENT)
        roomNotificationMessage.text = null
        roomNotificationIcon.setImageResource(0)
    }

    private fun renderNoPermissionToPost() {
        visibility = View.VISIBLE
        roomNotificationIcon.setImageDrawable(null)
        val message = span {
            italic {
                +resources.getString(R.string.room_do_not_have_permission_to_post)
            }
        }
        roomNotificationMessage.text = message
        roomNotificationMessage.setTextColor(ThemeUtils.getColor(context, R.attr.riotx_text_secondary))
    }

    private fun renderResourceLimitExceededError(state: State.ResourceLimitExceededError) {
        visibility = View.VISIBLE
        val resourceLimitErrorFormatter = ResourceLimitErrorFormatter(context)
        val formatterMode: ResourceLimitErrorFormatter.Mode
        val backgroundColor: Int
        if (state.isSoft) {
            backgroundColor = R.color.soft_resource_limit_exceeded
            formatterMode = ResourceLimitErrorFormatter.Mode.Soft
        } else {
            backgroundColor = R.color.hard_resource_limit_exceeded
            formatterMode = ResourceLimitErrorFormatter.Mode.Hard
        }
        val message = resourceLimitErrorFormatter.format(state.matrixError, formatterMode, clickable = true)
        roomNotificationMessage.setTextColor(Color.WHITE)
        roomNotificationMessage.text = message
        roomNotificationMessage.movementMethod = LinkMovementMethod.getInstance()
        roomNotificationMessage.setLinkTextColor(Color.WHITE)
        setBackgroundColor(ContextCompat.getColor(context, backgroundColor))
    }

    private fun renderTombstone(state: State.Tombstone) {
        visibility = View.VISIBLE
        roomNotificationIcon.setImageResource(R.drawable.error)
        val message = span {
            +resources.getString(R.string.room_tombstone_versioned_description)
            +"\n"
            span(resources.getString(R.string.room_tombstone_continuation_link)) {
                textDecorationLine = "underline"
                onClick = { delegate?.onTombstoneEventClicked(state.tombstoneEvent) }
            }
        }
        roomNotificationMessage.movementMethod = BetterLinkMovementMethod.getInstance()
        roomNotificationMessage.text = message
    }

    private fun renderDefault() {
        visibility = View.GONE
    }

    private fun renderHidden() {
        visibility = View.GONE
    }

    /**
     * The state representing the view
     * It can take one state at a time
     */
    sealed class State {

        // Not yet rendered
        object Initial : State()

        // View will be Invisible
        object Default : State()

        // User can't post messages to room because his power level doesn't allow it.
        object NoPermissionToPost : State()

        // View will be Gone
        object Hidden : State()

        // The room is dead
        data class Tombstone(val tombstoneEvent: Event) : State()

        // Resource limit exceeded error will be displayed (only hard for the moment)
        data class ResourceLimitExceededError(val isSoft: Boolean, val matrixError: MatrixError) : State()
    }

    /**
     * An interface to delegate some actions to another object
     */
    interface Delegate {
        fun onTombstoneEventClicked(tombstoneEvent: Event)
    }
}
