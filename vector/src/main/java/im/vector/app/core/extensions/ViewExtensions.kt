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

package im.vector.app.core.extensions

import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import im.vector.app.R

/**
 * Remove left margin of a SearchView
 */
fun SearchView.withoutLeftMargin() {
    (findViewById<View>(R.id.search_edit_frame))?.let {
        val searchEditFrameParams = it.layoutParams as ViewGroup.MarginLayoutParams
        searchEditFrameParams.leftMargin = 0
        it.layoutParams = searchEditFrameParams
    }

    (findViewById<View>(R.id.search_mag_icon))?.let {
        val searchIconParams = it.layoutParams as ViewGroup.MarginLayoutParams
        searchIconParams.leftMargin = 0
        it.layoutParams = searchIconParams
    }
}

fun EditText.hidePassword() {
    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
}

fun View.getMeasurements(): Pair<Int, Int> {
    measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    val width = measuredWidth
    val height = measuredHeight
    return width to height
}
