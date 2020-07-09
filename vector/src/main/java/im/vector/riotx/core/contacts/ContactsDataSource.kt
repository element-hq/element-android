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

package im.vector.riotx.core.contacts

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.annotation.WorkerThread
import javax.inject.Inject

class ContactsDataSource @Inject constructor(
        private val context: Context
) {

    @WorkerThread
    fun getContacts(): List<MappedContact> {
        val result = mutableListOf<MappedContact>()
        val contentResolver = context.contentResolver

        contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                /* TODO
                arrayOf(
                        ContactsContract.Contacts._ID,
                        ContactsContract.Data.DISPLAY_NAME,
                        ContactsContract.Data.PHOTO_URI,
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Email.ADDRESS
                ),
                 */
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

                            val currentContact = MappedContactBuilder(
                                    id = id,
                                    displayName = displayName
                            )

                            cursor.getString(ContactsContract.Data.PHOTO_URI)
                                    ?.let { Uri.parse(it) }
                                    ?.let { currentContact.photoURI = it }

                            // Get the phone numbers
                            contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                    arrayOf(id.toString()),
                                    null)
                                    ?.use { innerCursor ->
                                        while (innerCursor.moveToNext()) {
                                            innerCursor.getString(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                                    ?.let {
                                                        currentContact.msisdns.add(
                                                                MappedMsisdn(
                                                                        phoneNumber = it,
                                                                        matrixId = null
                                                                )
                                                        )
                                                    }
                                        }
                                    }

                            // Get Emails
                            contentResolver.query(
                                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                                    arrayOf(id.toString()),
                                    null)
                                    ?.use { innerCursor ->
                                        while (innerCursor.moveToNext()) {
                                            // This would allow you get several email addresses
                                            // if the email addresses were stored in an array
                                            innerCursor.getString(ContactsContract.CommonDataKinds.Email.DATA)
                                                    ?.let {
                                                        currentContact.emails.add(
                                                                MappedEmail(
                                                                        email = it,
                                                                        matrixId = null
                                                                )
                                                        )
                                                    }
                                        }
                                    }

                            result.add(currentContact.build())
                        }
                    }
                }

        return result
                .filter { it.emails.isNotEmpty() || it.msisdns.isNotEmpty() }
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
