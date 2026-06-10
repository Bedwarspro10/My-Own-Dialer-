package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CallLog
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.database.CallLogDao
import com.example.data.database.CallLogEntity
import com.example.data.database.ContactDao
import com.example.data.database.ContactEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class DialerRepository(
    private val context: Context,
    private val contactDao: ContactDao,
    private val callLogDao: CallLogDao
) {
    val allContacts: Flow<List<ContactEntity>> = contactDao.getAllContacts()
    val favoriteContacts: Flow<List<ContactEntity>> = contactDao.getFavoriteContacts()
    val allCallLogs: Flow<List<CallLogEntity>> = callLogDao.getAllCallLogs()
    val missedCallLogs: Flow<List<CallLogEntity>> = callLogDao.getMissedCallLogs()

    fun searchContacts(query: String): Flow<List<ContactEntity>> {
        return contactDao.searchContacts("%$query%")
    }

    suspend fun insertContact(contact: ContactEntity) = withContext(Dispatchers.IO) {
        contactDao.insertContact(contact)
    }

    suspend fun updateContact(contact: ContactEntity) = withContext(Dispatchers.IO) {
        contactDao.updateContact(contact)
    }

    suspend fun insertCallLog(callLog: CallLogEntity) = withContext(Dispatchers.IO) {
        callLogDao.insertCallLog(callLog)
    }

    suspend fun deleteContact(contact: ContactEntity) = withContext(Dispatchers.IO) {
        contactDao.deleteContact(contact)
    }

    suspend fun deleteCallLog(callLog: CallLogEntity) = withContext(Dispatchers.IO) {
        callLogDao.deleteCallLog(callLog)
    }

    /**
     * Prepopulates beautiful default contacts/call logs matching the target designs if the database is empty.
     */
    suspend fun prepopulateDefaultDataIfNeeded() = withContext(Dispatchers.IO) {
        // REMOVED ALL MOCK DATA AND SIMULATIVE CONTACTS AS REQUESTED IN AUDIT REQUIREMENTS
    }

    /**
     * Synergizes with Android Contact Book system providers if permission in context is available.
     */
    suspend fun trySyncWithAndroidSystemContacts() = withContext(Dispatchers.IO) {
        val readContactsPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS)
        if (readContactsPermission != PackageManager.PERMISSION_GRANTED) {
            Log.d("DialerRepository", "No contact read permission.")
            return@withContext
        }

        val contentResolver: ContentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val labelIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                val photoUriIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                val newContacts = mutableListOf<ContactEntity>()
                do {
                    val name = cursor.getString(nameIndex) ?: "Unknown"
                    val number = cursor.getString(numberIndex) ?: ""
                    val type = cursor.getInt(typeIndex)
                    val label = ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.resources, type, cursor.getString(labelIndex) ?: "mobile").toString()
                    val photoUrl = if (photoUriIndex >= 0) cursor.getString(photoUriIndex) else null

                    if (number.isNotEmpty()) {
                        // Avoid duplicates inside DB
                        newContacts.add(
                            ContactEntity(
                                name = name,
                                phoneNumber = number,
                                label = label,
                                isFavorite = false,
                                avatarColorHex = getRandomHexColorForName(name),
                                photoUrl = photoUrl
                            )
                        )
                    }
                } while (cursor.moveToNext())

                if (newContacts.isNotEmpty()) {
                    // Update contacts database
                    contactDao.deleteAllContacts()
                    contactDao.insertContacts(newContacts)
                    Log.d("DialerRepository", "Synced ${newContacts.size} contacts from Android system.")
                }
            }
        } catch (e: Exception) {
            Log.e("DialerRepository", "Error syncing contacts", e)
        } finally {
            cursor?.close()
        }
    }

    /**
     * Synergizes with Android Call Log provider if permission in context is available.
     */
    suspend fun trySyncWithAndroidSystemCallLogs() = withContext(Dispatchers.IO) {
        val readLogsPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG)
        if (readLogsPermission != PackageManager.PERMISSION_GRANTED) {
            Log.d("DialerRepository", "No call log read permission.")
            return@withContext
        }

        val contentResolver: ContentResolver = context.contentResolver
        val uri = CallLog.Calls.CONTENT_URI
        val projection = arrayOf(
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, projection, null, null, "${CallLog.Calls.DATE} DESC LIMIT 50")
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION)

                val newLogs = mutableListOf<CallLogEntity>()
                do {
                    val name = cursor.getString(nameIndex)
                    val number = cursor.getString(numberIndex) ?: ""
                    val type = cursor.getInt(typeIndex)
                    val date = cursor.getLong(dateIndex)
                    val duration = cursor.getInt(durationIndex)

                    val typeStr = when (type) {
                        CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                        CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                        CallLog.Calls.MISSED_TYPE -> "MISSED"
                        else -> "INCOMING"
                    }

                    if (number.isNotEmpty()) {
                        newLogs.add(
                            CallLogEntity(
                                callerName = name,
                                phoneNumber = number,
                                callType = typeStr,
                                timestamp = date,
                                durationSeconds = duration,
                                label = "phone"
                            )
                        )
                    }
                } while (cursor.moveToNext())

                if (newLogs.isNotEmpty()) {
                    callLogDao.deleteAllCallLogs()
                    callLogDao.insertCallLogs(newLogs)
                    Log.d("DialerRepository", "Synced ${newLogs.size} logs from Android system.")
                }
            }
        } catch (e: Exception) {
            Log.e("DialerRepository", "Error syncing call logs", e)
        } finally {
            cursor?.close()
        }
    }

    private fun getRandomHexColorForName(name: String): String {
        val colors = listOf("#7A8AFF", "#FF7A8A", "#8AFF7A", "#FFE57A", "#7AFFD8", "#D87AFF", "#FF9A7A", "#7AD8FF", "#B5FF7A", "#FF7AE3")
        val index = Math.abs(name.hashCode()) % colors.size
        return colors[index]
    }
}
