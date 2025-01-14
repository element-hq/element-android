/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
