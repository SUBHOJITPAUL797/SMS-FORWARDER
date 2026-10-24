package com.example.walkie.model

/**
 * Represents a P2P walkie-talkie signaling session stored in Firestore.
 * Audio is transmitted directly between devices via DTLS-SRTP (WebRTC).
 * Only SDP and ICE candidate text passes through Firestore.
 */
data class WtSession(
    val sessionId: String = "",
    val initiatorUid: String = "",
    val receiverUid: String = "",
    val hostCode: String = "",
    val status: WtSessionStatus = WtSessionStatus.CALLING,
    val offer: String = "",
    val answer: String = "",
    val iceCandidatesA: List<WtIceCandidate> = emptyList(),
    val iceCandidatesB: List<WtIceCandidate> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "sessionId" to sessionId,
        "initiatorUid" to initiatorUid,
        "receiverUid" to receiverUid,
        "hostCode" to hostCode,
        "status" to status.key,
        "offer" to offer,
        "answer" to answer,
        "iceCandidatesA" to iceCandidatesA.map { it.toMap() },
        "iceCandidatesB" to iceCandidatesB.map { it.toMap() },
        "createdAt" to createdAt
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any>): WtSession {
            val rawIceA = map["iceCandidatesA"] as? List<Map<String, Any>> ?: emptyList()
            val rawIceB = map["iceCandidatesB"] as? List<Map<String, Any>> ?: emptyList()
            return WtSession(
                sessionId = map["sessionId"] as? String ?: "",
                initiatorUid = map["initiatorUid"] as? String ?: "",
                receiverUid = map["receiverUid"] as? String ?: "",
                hostCode = map["hostCode"] as? String ?: "",
                status = WtSessionStatus.fromKey(map["status"] as? String ?: ""),
                offer = map["offer"] as? String ?: "",
                answer = map["answer"] as? String ?: "",
                iceCandidatesA = rawIceA.map { WtIceCandidate.fromMap(it) },
                iceCandidatesB = rawIceB.map { WtIceCandidate.fromMap(it) },
                createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis()
            )
        }
    }
}

enum class WtSessionStatus(val key: String) {
    CALLING("calling"),
    ACTIVE("active"),
    ENDED("ended"),
    DECLINED("declined");

    companion object {
        fun fromKey(key: String): WtSessionStatus =
            entries.firstOrNull { it.key == key } ?: ENDED
    }
}
