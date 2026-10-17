package com.gosms.gateway.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QrPayload(
    @Json(name = "v") val version: Int = 1,
    @Json(name = "server_url") val serverUrl: String,
    @Json(name = "pairing_code") val pairingCode: String
)

@JsonClass(generateAdapter = true)
data class SimInfo(
    @Json(name = "subscription_id") val subscriptionId: Int,
    @Json(name = "sim_slot") val simSlot: Int,
    @Json(name = "carrier_name") val carrierName: String,
    @Json(name = "phone_number") val phoneNumber: String? = null
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "pairing_code") val pairingCode: String,
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "device_name") val deviceName: String,
    @Json(name = "model") val model: String,
    @Json(name = "android_version") val androidVersion: String,
    @Json(name = "app_version") val appVersion: String,
    @Json(name = "sims") val sims: List<SimInfo>
)

@JsonClass(generateAdapter = true)
data class RegisterResponse(
    @Json(name = "ok") val ok: Boolean,
    @Json(name = "device_token") val deviceToken: String? = null,
    @Json(name = "device_id") val deviceId: String? = null,
    @Json(name = "poll_interval_ms") val pollIntervalMs: Long? = null,
    @Json(name = "heartbeat_interval_ms") val heartbeatIntervalMs: Long? = null,
    @Json(name = "max_per_minute") val maxPerMinute: Int? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class HeartbeatRequest(
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "battery_pct") val batteryPct: Int,
    @Json(name = "charging") val charging: Boolean,
    @Json(name = "network_type") val networkType: String,
    @Json(name = "app_version") val appVersion: String,
    @Json(name = "pending_count") val pendingCount: Int,
    @Json(name = "sims") val sims: List<SimInfo>
)

@JsonClass(generateAdapter = true)
data class HeartbeatResponse(
    @Json(name = "ok") val ok: Boolean,
    @Json(name = "server_time") val serverTime: Long? = null,
    @Json(name = "poll_interval_ms") val pollIntervalMs: Long? = null,
    @Json(name = "max_per_minute") val maxPerMinute: Int? = null,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class SmsMessageItem(
    @Json(name = "message_id") val messageId: String,
    @Json(name = "lease_id") val leaseId: String,
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "to") val to: String,
    @Json(name = "body") val body: String,
    @Json(name = "sim_slot") val simSlot: Int? = null
)

@JsonClass(generateAdapter = true)
data class MessagesResponse(
    @Json(name = "ok") val ok: Boolean,
    @Json(name = "messages") val messages: List<SmsMessageItem> = emptyList(),
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class StatusReportRequest(
    @Json(name = "message_id") val messageId: String,
    @Json(name = "lease_id") val leaseId: String,
    @Json(name = "status") val status: String,
    @Json(name = "error_code") val errorCode: String? = null,
    @Json(name = "error_detail") val errorDetail: String? = null,
    @Json(name = "parts") val parts: Int = 1,
    @Json(name = "timestamp") val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class SimpleResponse(
    @Json(name = "ok") val ok: Boolean,
    @Json(name = "message") val message: String? = null
)
