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
        val existingContacts = contactDao.getAllContacts().first()
        if (existingContacts.isEmpty()) {
            val defaultContacts = listOf(
                ContactEntity(name = "Mahboud Z.", phoneNumber = "+49 176 1234567", label = "mobile", isFavorite = true, avatarColorHex = "#7A8AFF"),
                ContactEntity(name = "Sharon Z.", phoneNumber = "0151 7654321", label = "home", isFavorite = true, avatarColorHex = "#FF7A8A"),
                ContactEntity(name = "Eric Z.", phoneNumber = "0172 9876543", label = "mobile", isFavorite = true, avatarColorHex = "#8AFF7A"),
                ContactEntity(name = "Ken Z.", phoneNumber = "+49 160 95620427", label = "work", isFavorite = false, avatarColorHex = "#FFE57A"),
                ContactEntity(name = "Dan Z.", phoneNumber = "0160 1111111", label = "phone", isFavorite = false, avatarColorHex = "#7AFFD8"),
                ContactEntity(name = "Bree Z.", phoneNumber = "0176 2222222", label = "mobile", isFavorite = false, avatarColorHex = "#D87AFF"),
                ContactEntity(name = "Richard Z.", phoneNumber = "0152 3333333", label = "mobile", isFavorite = false, avatarColorHex = "#FF9A7A"),
                ContactEntity(name = "Miriam Z.", phoneNumber = "0173 4444444", label = "mobile", isFavorite = false, avatarColorHex = "#7AD8FF"),
                ContactEntity(name = "Brian Z.", phoneNumber = "0171 5555555", label = "work", isFavorite = false, avatarColorHex = "#B5FF7A"),
                ContactEntity(name = "Tristan Engst", phoneNumber = "0150 1234567", label = "FaceTime Video", isFavorite = true, avatarColorHex = "#FF7AE3"),
                ContactEntity(name = "Aaron P.", phoneNumber = "0159 9999999", label = "iPhone", isFavorite = false, avatarColorHex = "#7AFFB0"),
                ContactEntity(name = "Walgreens (East Hill)", phoneNumber = "(555) 867-5309", label = "work", isFavorite = false, avatarColorHex = "#A0A0FF"),
                ContactEntity(name = "Village Taqueria", phoneNumber = "(555) 123-4567", label = "phone", isFavorite = false, avatarColorHex = "#FFA0A0"),
                ContactEntity(name = "Ian G.", phoneNumber = "0157 7777777", label = "mobile", isFavorite = false, avatarColorHex = "#A0FFA0"),
                ContactEntity(name = "Moose", phoneNumber = "(405) 555-0145", label = "phone", isFavorite = true, avatarColorHex = "#E0FFA0"),
                ContactEntity(name = "Thayer Appliance Center", phoneNumber = "(555) 987-6543", label = "work", isFavorite = false, avatarColorHex = "#FFA0E0")
            )
            contactDao.insertContacts(defaultContacts)
        }

        val existingLogs = callLogDao.getAllCallLogs().first()
        if (existingLogs.isEmpty()) {
            val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
            val yesterday = System.currentTimeMillis() - 24 * 60 * 60 * 1000
            val defaultLogs = listOf(
                CallLogEntity(callerName = "Walgreens (East Hill)", phoneNumber = "(555) 867-5309", callType = "MISSED", timestamp = yesterday + 3 * 3600 * 1000, label = "work"),
                CallLogEntity(callerName = "Village Taqueria", phoneNumber = "(555) 123-4567", callType = "OUTGOING", timestamp = yesterday - 2 * 3600 * 1000, label = "phone"),
                CallLogEntity(callerName = "Ian G.", phoneNumber = "0157 7777777", callType = "INCOMING", timestamp = yesterday - 5 * 3600 * 1000, label = "mobile"),
                CallLogEntity(callerName = "Moose", phoneNumber = "(405) 555-0145", callType = "INCOMING", timestamp = yesterday - 12 * 3600 * 1000, label = "phone"),
                CallLogEntity(callerName = "Tristan Engst", phoneNumber = "0150 1234567", callType = "MISSED", timestamp = yesterday - 20 * 3600 * 1000, label = "FaceTime Video"),
                CallLogEntity(callerName = "Aaron P.", phoneNumber = "0159 9999999", callType = "INCOMING", timestamp = yesterday - 30 * 3600 * 1000, label = "iPhone"),
                CallLogEntity(callerName = "Moose", phoneNumber = "(405) 555-0145", callType = "OUTGOING", timestamp = yesterday - 40 * 3600 * 1000, label = "phone"),
                CallLogEntity(callerName = "Thayer Appliance Center", phoneNumber = "(555) 987-6543", callType = "MISSED", timestamp = yesterday - 50 * 3600 * 1000, label = "work")
            )
            callLogDao.insertCallLogs(defaultLogs)
        }
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
            ContactsContract.CommonDataKinds.Phone.LABEL
        )

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val labelIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)

                val newContacts = mutableListOf<ContactEntity>()
                do {
                    val name = cursor.getString(nameIndex) ?: "Unknown"
                    val number = cursor.getString(numberIndex) ?: ""
                    val type = cursor.getInt(typeIndex)
                    val label = ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.resources, type, cursor.getString(labelIndex) ?: "mobile").toString()

                    if (number.isNotEmpty()) {
                        // Avoid duplicates inside DB
                        newContacts.add(
                            ContactEntity(
                                name = name,
                                phoneNumber = number,
                                label = label,
                                isFavorite = false,
                                avatarColorHex = getRandomHexColorForName(name)
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
