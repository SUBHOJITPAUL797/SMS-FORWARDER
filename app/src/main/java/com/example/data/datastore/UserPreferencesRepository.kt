package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val KEY_USER_ROLE = stringPreferencesKey("user_role")
        val KEY_LINKED_UID = stringPreferencesKey("linked_uid")
        val KEY_LINKED_DEVICE_NAME = stringPreferencesKey("linked_device_name")
        val KEY_FCM_TOKEN = stringPreferencesKey("fcm_token")
        val KEY_SERVICE_ACTIVE = booleanPreferencesKey("service_active")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_USER_UID = stringPreferencesKey("user_uid")
        val KEY_HOST_CODE = stringPreferencesKey("host_code")
        val KEY_AUTOSTART_CONFIGURED = booleanPreferencesKey("autostart_configured")
    }

    val isAutoStartConfiguredFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_AUTOSTART_CONFIGURED] ?: false
    }

    val hostCodeFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_HOST_CODE]
    }

    val userUidFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_USER_UID]
    }

    suspend fun getOrCreateDeviceUid(): String {
        var uid = userUidFlow.firstOrNull()
        if (uid.isNullOrEmpty()) {
            uid = "dev_" + java.util.UUID.randomUUID().toString().take(12)
            dataStore.edit { preferences ->
                preferences[KEY_USER_UID] = uid
            }
        }
        return uid
    }

    suspend fun getOrCreateHostCode(): String {
        var code = hostCodeFlow.firstOrNull()
        if (code.isNullOrEmpty()) {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val random = java.security.SecureRandom()
            val sb = java.lang.StringBuilder(6)
            for (i in 0 until 6) {
                sb.append(chars[random.nextInt(chars.length)])
            }
            code = sb.toString()
            dataStore.edit { preferences ->
                preferences[KEY_HOST_CODE] = code
            }
        }
        return code
    }

    suspend fun setHostCode(code: String) {
        dataStore.edit { preferences ->
            preferences[KEY_HOST_CODE] = code
        }
    }

    val userRoleFlow: Flow<UserRole> = dataStore.data.map { preferences ->
        UserRole.fromKey(preferences[KEY_USER_ROLE])
    }

    val linkedUidFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_LINKED_UID]
    }

    val linkedDeviceNameFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_LINKED_DEVICE_NAME] ?: "Connected Device"
    }

    val fcmTokenFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_FCM_TOKEN]
    }

    val isServiceActiveFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_SERVICE_ACTIVE] ?: false
    }

    val userEmailFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_USER_EMAIL]
    }

    suspend fun setUserRole(role: UserRole) {
        dataStore.edit { preferences ->
            preferences[KEY_USER_ROLE] = role.key
        }
    }

    suspend fun setLinkedDevice(linkedUid: String, deviceName: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LINKED_UID] = linkedUid
            preferences[KEY_LINKED_DEVICE_NAME] = deviceName
        }
    }

    suspend fun setFcmToken(token: String) {
        dataStore.edit { preferences ->
            preferences[KEY_FCM_TOKEN] = token
        }
    }

    suspend fun setServiceActive(active: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_SERVICE_ACTIVE] = active
        }
    }

    suspend fun setUserProfile(uid: String, email: String) {
        dataStore.edit { preferences ->
            preferences[KEY_USER_UID] = uid
            preferences[KEY_USER_EMAIL] = email
        }
    }

    suspend fun setAutoStartConfigured(configured: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTOSTART_CONFIGURED] = configured
        }
    }

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_USER_ROLE)
            preferences.remove(KEY_LINKED_UID)
            preferences.remove(KEY_LINKED_DEVICE_NAME)
            preferences.remove(KEY_USER_EMAIL)
            preferences.remove(KEY_USER_UID)
            preferences[KEY_SERVICE_ACTIVE] = false
        }
    }
}
