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

package im.vector.riotx.core.intent

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import androidx.core.util.PatternsCompat.WEB_URL
import java.util.*

/**
 * Inspired from Riot code: RoomMediaMessage.java
 */
sealed class ExternalIntentData {
    /**
     * Constructor for a text message.
     *
     * @param text     the text
     * @param htmlText the HTML text
     * @param format   the formatted text format
     */
    data class IntentDataText(
            val text: CharSequence? = null,
            val htmlText: String? = null,
            val format: String? = null,
            val clipDataItem: ClipData.Item = ClipData.Item(text, htmlText),
            val mimeType: String? = if (null == htmlText) ClipDescription.MIMETYPE_TEXT_PLAIN else format
    ) : ExternalIntentData()

    /**
     * Clip data
     */
    data class IntentDataClipData(
            val clipDataItem: ClipData.Item,
            val mimeType: String?
    ) : ExternalIntentData()

    /**
     * Constructor from a media Uri/
     *
     * @param uri      the media uri
     * @param filename the media file name
     */
    data class IntentDataUri(
            val uri: Uri,
            val filename: String? = null
    ) : ExternalIntentData()
}


fun analyseIntent(intent: Intent): List<ExternalIntentData> {
    val externalIntentDataList = ArrayList<ExternalIntentData>()


    // chrome adds many items when sharing an web page link
    // so, test first the type
    if (TextUtils.equals(intent.type, ClipDescription.MIMETYPE_TEXT_PLAIN)) {
        var message: String? = intent.getStringExtra(Intent.EXTRA_TEXT)

        if (null == message) {
            val sequence = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            if (null != sequence) {
                message = sequence.toString()
            }
        }

        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)

        if (!TextUtils.isEmpty(subject)) {
            if (TextUtils.isEmpty(message)) {
                message = subject
            } else if (WEB_URL.matcher(message!!).matches()) {
                message = subject + "\n" + message
            }
        }

        if (!TextUtils.isEmpty(message)) {
            externalIntentDataList.add(ExternalIntentData.IntentDataText(message!!, null, intent.type))
            return externalIntentDataList
        }
    }

    var clipData: ClipData? = null
    var mimetypes: MutableList<String>? = null

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        clipData = intent.clipData
    }

    // multiple data
    if (null != clipData) {
        if (null != clipData.description) {
            if (0 != clipData.description.mimeTypeCount) {
                mimetypes = ArrayList()

                for (i in 0 until clipData.description.mimeTypeCount) {
                    mimetypes.add(clipData.description.getMimeType(i))
                }

                // if the filter is "accept anything" the mimetype does not make sense
                if (1 == mimetypes.size) {
                    if (mimetypes[0].endsWith("/*")) {
                        mimetypes = null
                    }
                }
            }
        }

        val count = clipData.itemCount

        for (i in 0 until count) {
            val item = clipData.getItemAt(i)
            var mimetype: String? = null

            if (null != mimetypes) {
                if (i < mimetypes.size) {
                    mimetype = mimetypes[i]
                } else {
                    mimetype = mimetypes[0]
                }

                // uris list is not a valid mimetype
                if (TextUtils.equals(mimetype, ClipDescription.MIMETYPE_TEXT_URILIST)) {
                    mimetype = null
                }
            }

            externalIntentDataList.add(ExternalIntentData.IntentDataClipData(item, mimetype))
        }
    } else if (null != intent.data) {
        externalIntentDataList.add(ExternalIntentData.IntentDataUri(intent.data!!))
    }

    return externalIntentDataList
}