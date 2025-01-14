/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.popup

import android.app.Activity
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import java.lang.ref.WeakReference

interface VectorAlert {
    val uid: String
    val title: String
    val description: String
    val iconId: Int?
    val priority: Int
    val dismissOnClick: Boolean
    val isLight: Boolean
    val shouldBeDisplayedIn: ((Activity) -> Boolean)

    data class Button(val title: String, val action: Runnable, val autoClose: Boolean)

    // will be set by manager, and accessible by actions at runtime
    var weakCurrentActivity: WeakReference<Activity>?

    val actions: MutableList<Button>

    var contentAction: Runnable?
    var dismissedAction: Runnable?

    /** If this timestamp is after current time, this alert will be skipped. */
    var expirationTimestamp: Long?

    fun addButton(title: String, action: Runnable, autoClose: Boolean = true) {
        actions.add(Button(title, action, autoClose))
    }

    var viewBinder: ViewBinder?

    val layoutRes: Int

    var colorRes: Int?

    var colorInt: Int?

    var colorAttribute: Int?

    interface ViewBinder {
        fun bind(view: View)
    }
}

/**
 * Dataclass to describe an important alert with actions.
 */
open class DefaultVectorAlert(
        override val uid: String,
        override val title: String,
        override val description: String,
        @DrawableRes override val iconId: Int?,
        /**
         * Alert are displayed by default, but let this lambda return false to prevent displaying.
         */
        override val shouldBeDisplayedIn: ((Activity) -> Boolean) = { true }
) : VectorAlert {

    // will be set by manager, and accessible by actions at runtime
    override var weakCurrentActivity: WeakReference<Activity>? = null

    override val actions = ArrayList<VectorAlert.Button>()

    override var contentAction: Runnable? = null
    override var dismissedAction: Runnable? = null

    /** If this timestamp is after current time, this alert will be skipped. */
    override var expirationTimestamp: Long? = null

    @LayoutRes
    override val layoutRes = com.tapadoo.alerter.R.layout.alerter_alert_default_layout

    override val dismissOnClick: Boolean = true

    override val priority: Int = PopupAlertManager.DEFAULT_PRIORITY

    override val isLight: Boolean = false

    @ColorRes
    override var colorRes: Int? = null

    @ColorInt
    override var colorInt: Int? = null

    @AttrRes
    override var colorAttribute: Int? = null

    override var viewBinder: VectorAlert.ViewBinder? = null
}
