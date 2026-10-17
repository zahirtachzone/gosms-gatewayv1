package com.gosms.gateway.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

class SecureStorage(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "gosms_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getDeviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    fun getServerUrl(): String? {
        return prefs.getString(KEY_SERVER_URL, null)
    }

    fun setServerUrl(url: String?) {
        prefs.edit().putString(KEY_SERVER_URL, url).apply()
    }

    fun getDeviceToken(): String? {
        return prefs.getString(KEY_DEVICE_TOKEN, null)
    }

    fun isPaired(): Boolean {
        return prefs.getBoolean(KEY_IS_PAIRED, false) && !getDeviceToken().isNullOrBlank()
    }

    fun saveRegistration(
        serverUrl: String,
        token: String,
        deviceId: String,
        pollIntervalMs: Long,
        heartbeatIntervalMs: Long,
        maxPerMinute: Int
    ) {
        prefs.edit()
            .putString(KEY_SERVER_URL, serverUrl)
            .putString(KEY_DEVICE_TOKEN, token)
            .putString(KEY_DEVICE_ID, deviceId)
            .putLong(KEY_POLL_INTERVAL_MS, pollIntervalMs)
            .putLong(KEY_HEARTBEAT_INTERVAL_MS, heartbeatIntervalMs)
            .putInt(KEY_MAX_PER_MINUTE, maxPerMinute)
            .putBoolean(KEY_IS_PAIRED, true)
            .apply()
    }

    fun getAutoStart(): Boolean {
        return prefs.getBoolean(KEY_AUTO_START, true)
    }

    fun setAutoStart(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_START, enabled).apply()
    }

    fun getDefaultSimSlot(): Int? {
        val slot = prefs.getInt(KEY_DEFAULT_SIM_SLOT, -1)
        return if (slot == -1) null else slot
    }

    fun setDefaultSimSlot(slot: Int?) {
        if (slot == null) {
            prefs.edit().remove(KEY_DEFAULT_SIM_SLOT).apply()
        } else {
            prefs.edit().putInt(KEY_DEFAULT_SIM_SLOT, slot).apply()
        }
    }

    fun getPollIntervalMs(): Long {
        return prefs.getLong(KEY_POLL_INTERVAL_MS, 5000L)
    }

    fun setPollIntervalMs(ms: Long) {
        prefs.edit().putLong(KEY_POLL_INTERVAL_MS, ms).apply()
    }

    fun getHeartbeatIntervalMs(): Long {
        return prefs.getLong(KEY_HEARTBEAT_INTERVAL_MS, 60000L)
    }

    fun setHeartbeatIntervalMs(ms: Long) {
        prefs.edit().putLong(KEY_HEARTBEAT_INTERVAL_MS, ms).apply()
    }

    fun getMaxPerMinute(): Int {
        return prefs.getInt(KEY_MAX_PER_MINUTE, 20)
    }

    fun setMaxPerMinute(limit: Int) {
        prefs.edit().putInt(KEY_MAX_PER_MINUTE, limit).apply()
    }

    fun clearSession() {
        val deviceId = getDeviceId()
        prefs.edit()
            .remove(KEY_SERVER_URL)
            .remove(KEY_DEVICE_TOKEN)
            .putBoolean(KEY_IS_PAIRED, false)
            .putString(KEY_DEVICE_ID, deviceId)
            .apply()
    }

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_DEVICE_TOKEN = "device_token"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_AUTO_START = "auto_start"
        private const val KEY_DEFAULT_SIM_SLOT = "default_sim_slot"
        private const val KEY_POLL_INTERVAL_MS = "poll_interval_ms"
        private const val KEY_HEARTBEAT_INTERVAL_MS = "heartbeat_interval_ms"
        private const val KEY_MAX_PER_MINUTE = "max_per_minute"
        private const val KEY_IS_PAIRED = "is_paired"
    }
}
