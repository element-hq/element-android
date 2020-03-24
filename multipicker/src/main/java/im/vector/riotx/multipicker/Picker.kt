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
import android.net.Uri
import androidx.fragment.app.Fragment

abstract class Picker<T>(open val requestCode: Int) {

    protected var single = false

    abstract fun startWith(activity: Activity)

    abstract fun startWith(fragment: Fragment)

    open fun startWithExpectingFile(activity: Activity): Uri? = null

    open fun startWithExpectingFile(fragment: Fragment): Uri? = null

    abstract fun getSelectedFiles(context: Context, requestCode: Int, resultCode: Int, data: Intent?): List<T>

    fun getIncomingFiles(context: Context, data: Intent?): List<T> {
        return getSelectedFiles(context, requestCode, Activity.RESULT_OK, data)
    }

    fun single(): Picker<T> {
        single = true
        return this
    }

    protected fun getSelectedUriList(data: Intent?): List<Uri> {
        val selectedUriList = mutableListOf<Uri>()
        val dataUri = data?.data
        val clipData = data?.clipData

        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                selectedUriList.add(clipData.getItemAt(i).uri)
            }
        } else if (dataUri != null) {
            selectedUriList.add(dataUri)
        } else {
            data?.extras?.get(Intent.EXTRA_STREAM)?.let {
                @Suppress("UNCHECKED_CAST")
                when (it) {
                    is List<*> -> selectedUriList.addAll(it as List<Uri>)
                    else     -> selectedUriList.add(it as Uri)
                }
            }
        }
        return selectedUriList
    }
}
