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
package im.vector.riotx.features.media

import androidx.appcompat.app.AppCompatActivity
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.file.FileService
import im.vector.matrix.android.internal.crypto.attachments.ElementToDecrypt
import im.vector.riotx.R
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.core.utils.*
import java.io.File
import javax.inject.Inject

class MediaDownloadHelper @Inject constructor(private val activity: AppCompatActivity,
                                              private val session: Session,
                                              private val stringProvider: StringProvider,
                                              private val errorFormatter: ErrorFormatter) {

    private data class PendingData(
            val id: String,
            val filename: String,
            val url: String,
            val elementToDecrypt: ElementToDecrypt?
    )

    private var pendingData: PendingData? = null

    fun checkPermissionAndDownload(id: String, filename: String, url: String?, elementToDecrypt: ElementToDecrypt?) {
        if (url.isNullOrEmpty()) {
            activity.toast(stringProvider.getString(R.string.unexpected_error))
        } else if (checkPermissions(PERMISSIONS_FOR_WRITING_FILES, activity, PERMISSION_REQUEST_CODE_DOWNLOAD_FILE)) {
            downloadFile(id, filename, url, elementToDecrypt)
        } else {
            pendingData = PendingData(id, filename, url, elementToDecrypt)
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (allGranted(grantResults) && requestCode == PERMISSION_REQUEST_CODE_DOWNLOAD_FILE) {
            pendingData?.also {
                downloadFile(it.id, it.filename, it.url, it.elementToDecrypt)
            }
        }
    }

    private fun downloadFile(id: String, filename: String, url: String, elementToDecrypt: ElementToDecrypt?) {
        session.downloadFile(
                FileService.DownloadMode.TO_EXPORT,
                id,
                filename,
                url,
                elementToDecrypt,
                object : MatrixCallback<File> {
                    override fun onSuccess(data: File) {
                        activity.toast(stringProvider.getString(R.string.downloaded_file, data.path))
                    }

                    override fun onFailure(failure: Throwable) {
                        activity.toast(errorFormatter.toHumanReadable(failure))
                    }
                })

    }


}