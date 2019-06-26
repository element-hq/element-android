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

package im.vector.riotredesign.features.settings

import android.os.Bundle
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.withArgs
import im.vector.riotredesign.core.platform.VectorPreferenceFragment

class VectorSettingsPreferencesFragmentV2 : VectorPreferenceFragment() {

    override var titleRes: Int = R.string.title_activity_settings

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.vector_settings_preferences_root)
    }


    companion object {
        fun newInstance() = VectorSettingsPreferencesFragmentV2()
                .withArgs {
                    //putString(ARG_MATRIX_ID, matrixId)
                }
    }

}