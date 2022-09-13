/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.core.epoxy

import android.text.TextWatcher
import android.widget.CompoundButton
import android.widget.TextView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

fun VectorEpoxyHolder.setValueOnce(textInputEditText: TextInputEditText, value: String?) {
    if (view.isAttachedToWindow) {
        // the view is attached to the window
        // So it is a rebind of new data and you could ignore it assuming this is text that was already inputted into the view.
        // Downside is if you ever wanted to programmatically change the content of the edit text while it is on screen you would not be able to
    } else {
        textInputEditText.setText(value)
    }
}

fun VectorEpoxyHolder.setValueOnce(switchView: SwitchMaterial, switchChecked: Boolean, listener: CompoundButton.OnCheckedChangeListener) {
    if (view.isAttachedToWindow) {
        // the view is attached to the window
        // So it is a rebind of new data and you could ignore it assuming this is value that was already inputted into the view.
    } else {
        switchView.isChecked = switchChecked
        switchView.setOnCheckedChangeListener(listener)
    }
}

fun TextView.addTextChangedListenerOnce(textWatcher: TextWatcher) {
    // Ensure the watcher is not added multiple times
    removeTextChangedListener(textWatcher)
    addTextChangedListener(textWatcher)
}
