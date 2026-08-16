package com.example.domain.model

enum class UserRole(val key: String) {
    UNSET("unset"),
    HOST("host"),
    CLIENT("client");

    companion object {
        fun fromKey(key: String?): UserRole {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: UNSET
        }
    }
}
