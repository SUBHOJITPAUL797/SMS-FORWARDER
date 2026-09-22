package com.example.domain.model

/**
 * Represents a Client device that is linked to a Host.
 * Sourced from the `sms_forwarder_links` Firestore collection.
 */
data class ConnectedDevice(
    val clientUid: String,
    val clientDeviceName: String,
    val pairedAt: Long,
    val active: Boolean,
    val lastSeenAt: Long
) {
    /** Human-readable "last seen" string */
    fun lastSeenLabel(): String {
        if (lastSeenAt <= 0L) return "Never"
        val diffMs = System.currentTimeMillis() - lastSeenAt
        return when {
            diffMs < 60_000L -> "Just now"
            diffMs < 3_600_000L -> "${diffMs / 60_000}m ago"
            diffMs < 86_400_000L -> "${diffMs / 3_600_000}h ago"
            else -> "${diffMs / 86_400_000}d ago"
        }
    }

    companion object {
        fun fromMap(data: Map<String, Any>): ConnectedDevice? {
            val clientUid = data["clientUid"] as? String ?: return null
            return ConnectedDevice(
                clientUid = clientUid,
                clientDeviceName = data["clientDeviceName"] as? String ?: "Unknown Device",
                pairedAt = (data["pairedAt"] as? Number)?.toLong() ?: 0L,
                active = data["active"] as? Boolean ?: true,
                lastSeenAt = (data["lastSeenAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}
