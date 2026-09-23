package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
        val KEY_FLOATING_OTP_ENABLED = booleanPreferencesKey("floating_otp_enabled")
        val KEY_CALL_FORWARDING_ENABLED = booleanPreferencesKey("call_forwarding_enabled")

        // Offline Cellular SMS Fallback Preferences
        val KEY_OFFLINE_SMS_FALLBACK_ENABLED = booleanPreferencesKey("offline_sms_fallback_enabled")
        val KEY_FALLBACK_DESTINATION_NUMBER = stringPreferencesKey("fallback_destination_number")
        val KEY_PREFERRED_SIM_SLOT = intPreferencesKey("preferred_sim_slot") // 0 = Auto/SIM 1, 1 = SIM 1, 2 = SIM 2
        val KEY_DAILY_SMS_LIMIT_SIM1 = intPreferencesKey("daily_sms_limit_sim1")
        val KEY_DAILY_SMS_LIMIT_SIM2 = intPreferencesKey("daily_sms_limit_sim2")
        val KEY_DAILY_SMS_SENT_COUNT_SIM1 = intPreferencesKey("daily_sms_sent_count_sim1")
        val KEY_DAILY_SMS_SENT_COUNT_SIM2 = intPreferencesKey("daily_sms_sent_count_sim2")
        val KEY_DAILY_SMS_RESET_DATE = stringPreferencesKey("daily_sms_reset_date")
        val KEY_DUAL_SIM_ROLLOVER_ENABLED = booleanPreferencesKey("dual_sim_rollover_enabled")
        val KEY_HOST_PHONE_NUMBER = stringPreferencesKey("host_phone_number")
    }

    val isCallForwardingEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_CALL_FORWARDING_ENABLED] ?: true
    }

    val isFloatingOtpEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_FLOATING_OTP_ENABLED] ?: false
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

    suspend fun clearLinkedDevice() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_LINKED_UID)
            preferences.remove(KEY_LINKED_DEVICE_NAME)
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

    suspend fun setFloatingOtpEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_FLOATING_OTP_ENABLED] = enabled
        }
    }

    suspend fun setCallForwardingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_CALL_FORWARDING_ENABLED] = enabled
        }
    }

    // --- Offline Cellular SMS Fallback Flows & Methods ---

    val isOfflineSmsFallbackEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_OFFLINE_SMS_FALLBACK_ENABLED] ?: true
    }

    val fallbackDestinationNumberFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_FALLBACK_DESTINATION_NUMBER] ?: ""
    }

    val preferredSimSlotFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_PREFERRED_SIM_SLOT] ?: 0
    }

    val dailySmsLimitSim1Flow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_DAILY_SMS_LIMIT_SIM1] ?: 100
    }

    val dailySmsLimitSim2Flow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_DAILY_SMS_LIMIT_SIM2] ?: 100
    }

    val dailySmsSentCountSim1Flow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_DAILY_SMS_SENT_COUNT_SIM1] ?: 0
    }

    val dailySmsSentCountSim2Flow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_DAILY_SMS_SENT_COUNT_SIM2] ?: 0
    }

    val isDualSimRolloverEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_DUAL_SIM_ROLLOVER_ENABLED] ?: true
    }

    val hostPhoneNumberFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_HOST_PHONE_NUMBER] ?: ""
    }

    suspend fun setOfflineSmsFallbackEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_OFFLINE_SMS_FALLBACK_ENABLED] = enabled
        }
    }

    suspend fun setFallbackDestinationNumber(number: String) {
        dataStore.edit { preferences ->
            preferences[KEY_FALLBACK_DESTINATION_NUMBER] = number.trim()
        }
    }

    suspend fun setPreferredSimSlot(slot: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_PREFERRED_SIM_SLOT] = slot
        }
    }

    suspend fun setDailySmsLimitSim1(limit: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_DAILY_SMS_LIMIT_SIM1] = limit.coerceAtLeast(1)
        }
    }

    suspend fun setDailySmsLimitSim2(limit: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_DAILY_SMS_LIMIT_SIM2] = limit.coerceAtLeast(1)
        }
    }

    suspend fun setDualSimRolloverEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DUAL_SIM_ROLLOVER_ENABLED] = enabled
        }
    }

    suspend fun setHostPhoneNumber(number: String) {
        dataStore.edit { preferences ->
            preferences[KEY_HOST_PHONE_NUMBER] = number.trim()
        }
    }

    /**
     * Checks if a new day has arrived (after midnight) and automatically resets daily counters.
     * Returns Pair(sim1Count, sim2Count) of current counts after reset check.
     */
    suspend fun checkAndResetDailyQuota(): Pair<Int, Int> {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        var sim1 = 0
        var sim2 = 0
        dataStore.edit { preferences ->
            val lastReset = preferences[KEY_DAILY_SMS_RESET_DATE] ?: ""
            if (lastReset != todayStr) {
                preferences[KEY_DAILY_SMS_RESET_DATE] = todayStr
                preferences[KEY_DAILY_SMS_SENT_COUNT_SIM1] = 0
                preferences[KEY_DAILY_SMS_SENT_COUNT_SIM2] = 0
                sim1 = 0
                sim2 = 0
            } else {
                sim1 = preferences[KEY_DAILY_SMS_SENT_COUNT_SIM1] ?: 0
                sim2 = preferences[KEY_DAILY_SMS_SENT_COUNT_SIM2] ?: 0
            }
        }
        return Pair(sim1, sim2)
    }

    /**
     * Increments the sent SMS count for the given SIM slot (0 for SIM 1, 1 for SIM 2).
     */
    suspend fun incrementSmsSentCount(slotIndex: Int, parts: Int = 1) {
        checkAndResetDailyQuota()
        dataStore.edit { preferences ->
            if (slotIndex == 0) {
                val current = preferences[KEY_DAILY_SMS_SENT_COUNT_SIM1] ?: 0
                preferences[KEY_DAILY_SMS_SENT_COUNT_SIM1] = current + parts
            } else {
                val current = preferences[KEY_DAILY_SMS_SENT_COUNT_SIM2] ?: 0
                preferences[KEY_DAILY_SMS_SENT_COUNT_SIM2] = current + parts
            }
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
