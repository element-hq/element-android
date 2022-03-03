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

package im.vector.lib.multipicker

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import im.vector.lib.multipicker.entity.MultiPickerContactType
import im.vector.lib.multipicker.utils.getColumnIndexOrNull

/**
 * Contact Picker implementation
 */
class ContactPicker : Picker<MultiPickerContactType>() {

    /**
     * Call this function from onActivityResult(int, int, Intent).
     * Returns selected contact or empty list if user did not select any contacts.
     */
    override fun getSelectedFiles(context: Context, data: Intent?): List<MultiPickerContactType> {
        val contactList = mutableListOf<MultiPickerContactType>()

        data?.data?.let { selectedUri ->
            val projection = arrayOf(
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.PHOTO_URI,
                    ContactsContract.Contacts._ID
            )

            context.contentResolver.query(
                    selectedUri,
                    projection,
                    null,
                    null,
                    null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrNull(ContactsContract.Contacts._ID) ?: return@use
                    val nameColumn = cursor.getColumnIndexOrNull(ContactsContract.Contacts.DISPLAY_NAME) ?: return@use
                    val photoUriColumn = cursor.getColumnIndexOrNull(ContactsContract.Contacts.PHOTO_URI) ?: return@use

                    val contactId = cursor.getIntOrNull(idColumn) ?: return@use
                    var name = cursor.getStringOrNull(nameColumn) ?: return@use
                    val photoUri = cursor.getStringOrNull(photoUriColumn)
                    val phoneNumberList = mutableListOf<String>()
                    val emailList = mutableListOf<String>()

                    getRawContactId(context.contentResolver, contactId)?.let { rawContactId ->
                        val selection = ContactsContract.Data.RAW_CONTACT_ID + " = ?"
                        val selectionArgs = arrayOf(rawContactId.toString())

                        context.contentResolver.query(
                                ContactsContract.Data.CONTENT_URI,
                                arrayOf(
                                        ContactsContract.Data.MIMETYPE,
                                        ContactsContract.Data.DATA1
                                ),
                                selection,
                                selectionArgs,
                                null
                        )?.use inner@{ innerCursor ->
                            val mimeTypeColumnIndex = innerCursor.getColumnIndexOrNull(ContactsContract.Data.MIMETYPE) ?: return@inner
                            val data1ColumnIndex = innerCursor.getColumnIndexOrNull(ContactsContract.Data.DATA1) ?: return@inner

                            while (innerCursor.moveToNext()) {
                                val mimeType = innerCursor.getStringOrNull(mimeTypeColumnIndex)
                                val contactData = innerCursor.getStringOrNull(data1ColumnIndex) ?: continue

                                if (mimeType == ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE) {
                                    name = contactData
                                }
                                if (mimeType == ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE) {
                                    phoneNumberList.add(contactData)
                                }
                                if (mimeType == ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE) {
                                    emailList.add(contactData)
                                }
                            }
                        }
                    }
                    contactList.add(
                            MultiPickerContactType(
                                    name,
                                    photoUri,
                                    phoneNumberList,
                                    emailList
                            )
                    )
                }
            }
        }

        return contactList
    }

    private fun getRawContactId(contentResolver: ContentResolver, contactId: Int): Int? {
        val projection = arrayOf(ContactsContract.RawContacts._ID)
        val selection = ContactsContract.RawContacts.CONTACT_ID + " = ?"
        val selectionArgs = arrayOf(contactId.toString() + "")
        return contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        )?.use { cursor ->
            return if (cursor.moveToFirst()) {
                cursor.getColumnIndexOrNull(ContactsContract.RawContacts._ID)
                        ?.let { cursor.getIntOrNull(it) }
            } else null
        }
    }

    override fun createIntent(): Intent {
        return Intent(Intent.ACTION_PICK).apply {
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, !single)
            type = ContactsContract.Contacts.CONTENT_TYPE
        }
    }
}
