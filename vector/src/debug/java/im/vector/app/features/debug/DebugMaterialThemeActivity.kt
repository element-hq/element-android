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

package im.vector.app.features.debug

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import im.vector.app.R
import im.vector.app.core.utils.toast
import kotlinx.android.synthetic.debug.activity_test_material_theme.*

// Rendering is not the same with VectorBaseActivity
abstract class DebugMaterialThemeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_material_theme)

        debugShowSnackbar.setOnClickListener {
            Snackbar.make(debugMaterialCoordinator, "Snackbar!", Snackbar.LENGTH_SHORT)
                    .setAction("Action") { }
                    .show()
        }

        debugShowToast.setOnClickListener {
            toast("Toast")
        }

        debugShowDialog.setOnClickListener {
            AlertDialog.Builder(this)
                    .setMessage("Dialog content")
                    .setIcon(R.drawable.ic_settings_x)
                    .setPositiveButton("Positive", null)
                    .setNegativeButton("Negative", null)
                    .setNeutralButton("Neutral", null)
                    .show()
        }

        debugShowBottomSheet.setOnClickListener {
            BottomSheetDialogFragment().show(supportFragmentManager, "TAG")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home, menu)
        return true
    }
}
