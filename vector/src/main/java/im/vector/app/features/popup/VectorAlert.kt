/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.popup

import android.app.Activity
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import org.matrix.android.sdk.api.util.MatrixItem
import java.lang.ref.WeakReference

interface VectorAlert {
    val uid: String
    val title: String
    val description: String
    val iconId: Int?
    val shouldBeDisplayedIn: ((Activity) -> Boolean)?

    data class Button(val title: String, val action: Runnable, val autoClose: Boolean)

    // will be set by manager, and accessible by actions at runtime
    var weakCurrentActivity: WeakReference<Activity>?

    val actions: MutableList<Button>

    var contentAction: Runnable?
    var dismissedAction: Runnable?

    /** If this timestamp is after current time, this alert will be skipped */
    var expirationTimestamp: Long?

    fun addButton(title: String, action: Runnable, autoClose: Boolean = true) {
        actions.add(Button(title, action, autoClose))
    }

    var colorRes: Int?

    var colorInt: Int?
}

/**
 * Dataclass to describe an important alert with actions.
 */
open class DefaultVectorAlert(override val uid: String,
                              override val title: String,
                              override val description: String,
                              @DrawableRes override val iconId: Int?,
                              override val shouldBeDisplayedIn: ((Activity) -> Boolean)? = null) : VectorAlert {

    // will be set by manager, and accessible by actions at runtime
    override var weakCurrentActivity: WeakReference<Activity>? = null

    override val actions = ArrayList<VectorAlert.Button>()

    override var contentAction: Runnable? = null
    override var dismissedAction: Runnable? = null

    /** If this timestamp is after current time, this alert will be skipped */
    override var expirationTimestamp: Long? = null

    override fun addButton(title: String, action: Runnable, autoClose: Boolean) {
        actions.add(VectorAlert.Button(title, action, autoClose))
    }

    @ColorRes
    override var colorRes: Int? = null

    @ColorInt
    override var colorInt: Int? = null
}

class VerificationVectorAlert(uid: String,
                              title: String,
                              override val description: String,
                              @DrawableRes override val iconId: Int?,
                              override val shouldBeDisplayedIn: ((Activity) -> Boolean)? = null
) : DefaultVectorAlert(
        uid, title, description, iconId, shouldBeDisplayedIn
) {
    var matrixItem: MatrixItem? = null
}
