package com.example.data.repository

import com.example.data.datastore.UserPreferencesRepository
import com.example.data.remote.AuthSource
import com.example.data.remote.FirestoreSource
import com.example.domain.model.UserRole
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

class AuthRepository(
    private val authSource: AuthSource,
    private val firestoreSource: FirestoreSource,
    private val preferencesRepository: UserPreferencesRepository
) {
    val currentUser: FirebaseUser?
        get() = authSource.currentUser

    val userRoleFlow: Flow<UserRole> = preferencesRepository.userRoleFlow
    val userEmailFlow: Flow<String?> = preferencesRepository.userEmailFlow
    val linkedUidFlow: Flow<String?> = preferencesRepository.linkedUidFlow
    val linkedDeviceNameFlow: Flow<String> = preferencesRepository.linkedDeviceNameFlow
    val hostCodeFlow: Flow<String?> = preferencesRepository.hostCodeFlow

    suspend fun getDeviceUid(): String {
        return preferencesRepository.getOrCreateDeviceUid()
    }

    suspend fun getHostCode(): String {
        return preferencesRepository.getOrCreateHostCode()
    }

    suspend fun setUserRoleLocal(role: UserRole) {
        preferencesRepository.setUserRole(role)
    }

    suspend fun syncUserProfile(role: UserRole): Result<Unit> {
        return try {
            val deviceUid = preferencesRepository.getOrCreateDeviceUid()
            val hostCode = if (role == UserRole.HOST) preferencesRepository.getOrCreateHostCode() else null
            firestoreSource.saveUserProfile(
                uid = deviceUid,
                email = "device@smsbridge.local",
                role = role,
                linkedUid = hostCode
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setUserRole(role: UserRole): Result<Unit> {
        setUserRoleLocal(role)
        return syncUserProfile(role)
    }

    suspend fun resetRole() {
        preferencesRepository.setUserRole(UserRole.UNSET)
    }

    suspend fun logout() {
        preferencesRepository.clearSession()
    }
}
