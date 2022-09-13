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

package im.vector.lib.ui.styles.dialogs

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.lib.ui.styles.R
import im.vector.lib.ui.styles.databinding.DialogProgressMaterialBinding

class MaterialProgressDialog(val context: Context) {
    fun show(message: CharSequence, cancellable: Boolean = false): AlertDialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_progress_material, null)
        val views = DialogProgressMaterialBinding.bind(view)
        views.message.text = message

        return MaterialAlertDialogBuilder(context)
                .setCancelable(cancellable)
                .setView(view)
                .show()
    }
}
