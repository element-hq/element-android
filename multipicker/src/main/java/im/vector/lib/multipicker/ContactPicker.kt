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

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import im.vector.lib.multipicker.entity.MultiPickerContactType

/**
 * Contact Picker implementation
 */
class ContactPicker(override val requestCode: Int) : Picker<MultiPickerContactType>(requestCode) {

    /**
     * Call this function from onActivityResult(int, int, Intent).
     * Returns selected contact or empty list if request code is wrong
     * or result code is not Activity.RESULT_OK
     * or user did not select any files.
     */
    override fun getSelectedFiles(context: Context, requestCode: Int, resultCode: Int, data: Intent?): List<MultiPickerContactType> {
        if (requestCode != this.requestCode && resultCode != Activity.RESULT_OK) {
            return emptyList()
        }

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
                    val idColumn = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameColumn = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val photoUriColumn = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

                    val contactId = cursor.getInt(idColumn)
                    var name = cursor.getString(nameColumn)
                    val photoUri = cursor.getString(photoUriColumn)
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
                        )?.use { cursor ->
                            while (cursor.moveToNext()) {
                                val mimeType = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE))
                                val contactData = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1))

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
            return if (cursor.moveToFirst()) cursor.getInt(cursor.getColumnIndex(ContactsContract.RawContacts._ID)) else null
        }
    }

    override fun createIntent(): Intent {
        return Intent(Intent.ACTION_PICK).apply {
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, !single)
            type = ContactsContract.Contacts.CONTENT_TYPE
        }
    }
}
