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

package im.vector.riotx.multipicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment

abstract class Picker<T>(open val requestCode: Int) {

    protected var single = false

    abstract fun startWith(activity: Activity)

    abstract fun startWith(fragment: Fragment)

    abstract fun getSelectedFiles(context: Context, requestCode: Int, resultCode: Int, data: Intent?): List<T>

    fun single(): Picker<T> {
        single = true
        return this
    }
}
