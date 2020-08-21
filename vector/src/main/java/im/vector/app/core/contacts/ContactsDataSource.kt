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

package im.vector.app.core.contacts

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.annotation.WorkerThread
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.measureTimeMillis

class ContactsDataSource @Inject constructor(
        private val context: Context
) {

    /**
     * Will return a list of contact from the contacts book of the device, with at least one email or phone.
     * If both param are false, you will get en empty list.
     * Note: The return list does not contain any matrixId.
     */
    @WorkerThread
    fun getContacts(
            withEmails: Boolean,
            withMsisdn: Boolean
    ): List<MappedContact> {
        val map = mutableMapOf<Long, MappedContactBuilder>()
        val contentResolver = context.contentResolver

        measureTimeMillis {
            contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    arrayOf(
                            ContactsContract.Contacts._ID,
                            ContactsContract.Data.DISPLAY_NAME,
                            ContactsContract.Data.PHOTO_URI
                    ),
                    null,
                    null,
                    // Sort by Display name
                    ContactsContract.Data.DISPLAY_NAME
            )
                    ?.use { cursor ->
                        if (cursor.count > 0) {
                            while (cursor.moveToNext()) {
                                val id = cursor.getLong(ContactsContract.Contacts._ID) ?: continue
                                val displayName = cursor.getString(ContactsContract.Contacts.DISPLAY_NAME) ?: continue

                                val mappedContactBuilder = MappedContactBuilder(
                                        id = id,
                                        displayName = displayName
                                )

                                cursor.getString(ContactsContract.Data.PHOTO_URI)
                                        ?.let { Uri.parse(it) }
                                        ?.let { mappedContactBuilder.photoURI = it }

                                map[id] = mappedContactBuilder
                            }
                        }
                    }

            // Get the phone numbers
            if (withMsisdn) {
                contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                        ),
                        null,
                        null,
                        null)
                        ?.use { innerCursor ->
                            while (innerCursor.moveToNext()) {
                                val mappedContactBuilder = innerCursor.getLong(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                                        ?.let { map[it] }
                                        ?: continue
                                innerCursor.getString(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                        ?.let {
                                            mappedContactBuilder.msisdns.add(
                                                    MappedMsisdn(
                                                            phoneNumber = it,
                                                            matrixId = null
                                                    )
                                            )
                                        }
                            }
                        }
            }

            // Get Emails
            if (withEmails) {
                contentResolver.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        arrayOf(
                                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                                ContactsContract.CommonDataKinds.Email.DATA
                        ),
                        null,
                        null,
                        null)
                        ?.use { innerCursor ->
                            while (innerCursor.moveToNext()) {
                                // This would allow you get several email addresses
                                // if the email addresses were stored in an array
                                val mappedContactBuilder = innerCursor.getLong(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
                                        ?.let { map[it] }
                                        ?: continue
                                innerCursor.getString(ContactsContract.CommonDataKinds.Email.DATA)
                                        ?.let {
                                            mappedContactBuilder.emails.add(
                                                    MappedEmail(
                                                            email = it,
                                                            matrixId = null
                                                    )
                                            )
                                        }
                            }
                        }
            }
        }.also { Timber.d("Took ${it}ms to fetch ${map.size} contact(s)") }

        return map
                .values
                .filter { it.emails.isNotEmpty() || it.msisdns.isNotEmpty() }
                .map { it.build() }
    }

    private fun Cursor.getString(column: String): String? {
        return getColumnIndex(column)
                .takeIf { it != -1 }
                ?.let { getString(it) }
    }

    private fun Cursor.getLong(column: String): Long? {
        return getColumnIndex(column)
                .takeIf { it != -1 }
                ?.let { getLong(it) }
    }
}
