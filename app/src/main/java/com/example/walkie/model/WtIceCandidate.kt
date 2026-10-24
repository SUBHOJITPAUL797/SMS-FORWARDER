package com.example.walkie.model

/**
 * Serializable wrapper around a WebRTC ICE candidate.
 * Stored in Firestore as part of a WtSession signaling document.
 */
data class WtIceCandidate(
    val sdp: String = "",
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "sdp" to sdp,
        "sdpMid" to sdpMid,
        "sdpMLineIndex" to sdpMLineIndex
    )

    companion object {
        fun fromMap(map: Map<String, Any>): WtIceCandidate = WtIceCandidate(
            sdp = map["sdp"] as? String ?: "",
            sdpMid = map["sdpMid"] as? String ?: "",
            sdpMLineIndex = (map["sdpMLineIndex"] as? Long)?.toInt() ?: 0
        )
    }
}
