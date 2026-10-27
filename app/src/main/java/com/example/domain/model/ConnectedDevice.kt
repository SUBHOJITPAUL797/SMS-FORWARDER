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
    val lastSeenAt: Long,
    val lastLocation: Map<String, Any>? = null,
    val locationRequestedAt: Long = 0L
) {
    val mapsUrl: String?
        get() = lastLocation?.get("mapsUrl") as? String

    val accuracy: Float?
        get() = (lastLocation?.get("accuracy") as? Number)?.toFloat()

    val batteryPercentage: Int?
        get() = (lastLocation?.get("battery") as? Number)?.toInt()

    val isCharging: Boolean?
        get() = lastLocation?.get("isCharging") as? Boolean

    val locationTimestamp: Long?
        get() = (lastLocation?.get("timestamp") as? Number)?.toLong()

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
        @Suppress("UNCHECKED_CAST")
        fun fromMap(data: Map<String, Any>): ConnectedDevice? {
            val clientUid = data["clientUid"] as? String ?: return null
            return ConnectedDevice(
                clientUid = clientUid,
                clientDeviceName = data["clientDeviceName"] as? String ?: "Unknown Device",
                pairedAt = (data["pairedAt"] as? Number)?.toLong() ?: 0L,
                active = data["active"] as? Boolean ?: true,
                lastSeenAt = (data["lastSeenAt"] as? Number)?.toLong() ?: 0L,
                lastLocation = data["lastLocation"] as? Map<String, Any>,
                locationRequestedAt = (data["locationRequestedAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}
