package com.example

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactsHelper {

    fun getPhoneFromUri(context: Context, uri: Uri): String? {
        var phone: String? = null
        var contactId: String? = null
        
        // Step 1: Query Contact ID
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                if (idIndex != -1) {
                    contactId = cursor.getString(idIndex)
                }
            }
        }

        // Step 2: Query Phone number using Contact ID
        if (contactId != null) {
            val selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                selection,
                arrayOf(contactId),
                null
            )?.use { phoneCursor ->
                if (phoneCursor.moveToFirst()) {
                    val numIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numIndex != -1) {
                        phone = phoneCursor.getString(numIndex)
                    }
                }
            }
        }
        
        return phone
    }

    fun lookupName(context: Context, normalizedNumber: String): String? {
        if (normalizedNumber.isEmpty()) return null
        var displayName: String? = null
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(normalizedNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (index != -1) {
                        displayName = cursor.getString(index)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return displayName
    }

    fun getNameFromUri(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    if (index != -1) {
                        name = cursor.getString(index)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return name
    }
}
