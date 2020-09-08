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

package im.vector.app.features.settings

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import im.vector.app.R

class BackgroundSyncModeChooserDialog : DialogFragment() {

    var interactionListener: InteractionListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { activity: FragmentActivity ->
            val builder = AlertDialog.Builder(activity)
            // Get the layout inflater
            val inflater = activity.layoutInflater

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val view = inflater.inflate(R.layout.dialog_background_sync_mode, null)
            view.findViewById<ListView>(R.id.dialog_background_sync_list)?.let {
                it.adapter = Adapter(
                        activity,
                        BackgroundSyncMode.fromString(arguments?.getString(ARG_INITIAL_MODE))
                )
            }
            builder.setView(view)
                    // Add action buttons
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        val mode = getSelectedOption()
                        if (mode.name == arguments?.getString(ARG_INITIAL_MODE)) {
                            // it's like a cancel, no changes
                            dialog.cancel()
                        } else {
                            interactionListener?.onOptionSelected(mode)
                            dialog.dismiss()
                        }
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        interactionListener?.onCancel()
                        dialog.cancel()
                    }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun getSelectedOption(): BackgroundSyncMode {
        options.forEach {
            if (it.isSelected) return it.mode
        }
        // an item is always selected, should not happen
        return options[0].mode
    }

    data class SyncMode(val mode: BackgroundSyncMode, val title: Int, val description: Int, var isSelected: Boolean)

    private class Adapter(context: Context, val initialMode: BackgroundSyncMode) : ArrayAdapter<SyncMode>(context, 0, options) {
        init {
            // mark the currently selected option
            var initialModeFound = false
            options.forEach {
                it.isSelected = initialMode == it.mode
                initialModeFound = true
            }
            if (!initialModeFound) {
                options[0].isSelected = true
            }
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val syncMode = getItem(position)!!

            // Only 3 items, let's keep it like that
            val itemView = convertView
                    ?: LayoutInflater.from(context).inflate(R.layout.item_custom_dialog_radio_line, parent, false)

            // Lookup view for data population
            itemView?.findViewById<TextView>(R.id.item_generic_title_text)?.let {
                it.text = context.getString(syncMode.title)
            }
            itemView?.findViewById<TextView>(R.id.item_generic_description_text)?.let {
                it.text = context.getString(syncMode.description)
                it.isVisible = true
            }
            itemView?.findViewById<RadioButton>(R.id.item_generic_radio)?.let {
                it.isChecked = syncMode.isSelected
                it.isVisible = true
                // let the item click handle that
                it.setOnClickListener {
                    toggleChangeAtPosition(position)
                }
            }

            itemView?.setOnClickListener {
                toggleChangeAtPosition(position)
            }

            // Populate the data into the template view using the data object

            return itemView
        }

        private fun toggleChangeAtPosition(position: Int) {
            if (getItem(position)?.isSelected == true) {
                // nop
            } else {
                for (i in 0 until count) {
                    // we change the single selection
                    getItem(i)?.isSelected = i == position
                }
                notifyDataSetChanged()
            }
        }
    }

    interface InteractionListener {
        fun onCancel() {}
        fun onOptionSelected(mode: BackgroundSyncMode) {}
    }

    companion object {
        private val options = listOf(
                SyncMode(BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_FOR_BATTERY,
                        R.string.settings_background_fdroid_sync_mode_battery,
                        R.string.settings_background_fdroid_sync_mode_battery_description, false),
                SyncMode(BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_FOR_REALTIME,
                        R.string.settings_background_fdroid_sync_mode_real_time,
                        R.string.settings_background_fdroid_sync_mode_real_time_description, false),
                SyncMode(BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_DISABLED,
                        R.string.settings_background_fdroid_sync_mode_disabled,
                        R.string.settings_background_fdroid_sync_mode_disabled_description, false)
        )

        private const val ARG_INITIAL_MODE = "ARG_INITIAL_MODE"

        fun newInstance(selectedMode: BackgroundSyncMode, interactionListener: InteractionListener): BackgroundSyncModeChooserDialog {
            val frag = BackgroundSyncModeChooserDialog()
            frag.interactionListener = interactionListener
            val args = Bundle()
            args.putString(ARG_INITIAL_MODE, selectedMode.name)
            frag.arguments = args
            return frag
        }
    }
}
