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

package im.vector.app.core.resources

import android.content.res.Resources
import androidx.annotation.ArrayRes
import androidx.annotation.NonNull
import javax.inject.Inject

class StringArrayProvider @Inject constructor(private val resources: Resources) {

    /**
     * Returns a localized string array from the application's package's
     * default string array table.
     *
     * @param resId Resource id for the string array
     * @return The string array associated with the resource, stripped of styled
     * text information.
     */
    @NonNull
    fun getStringArray(@ArrayRes resId: Int): Array<String> {
        return resources.getStringArray(resId)
    }
}
