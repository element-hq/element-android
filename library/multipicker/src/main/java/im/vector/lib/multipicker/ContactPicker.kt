/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.multipicker

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import im.vector.lib.multipicker.entity.MultiPickerContactType
import im.vector.lib.multipicker.utils.getColumnIndexOrNull

/**
 * Contact Picker implementation.
 */
class ContactPicker : Picker<MultiPickerContactType>() {

    /**
     * Call this function from onActivityResult(int, int, Intent).
     * Returns selected contact or empty list if user did not select any contacts.
     */
    @SuppressLint("Recycle")
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
