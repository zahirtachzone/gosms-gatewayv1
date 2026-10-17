package com.gosms.gateway.data.sim

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.gosms.gateway.data.model.SimInfo

class SimManager(private val context: Context) {
    private val subscriptionManager: SubscriptionManager? =
        context.getSystemService(SubscriptionManager::class.java)

    fun hasPhonePermission(): Boolean {
        val hasState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val hasNumbers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_NUMBERS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return hasState && hasNumbers
    }

    fun getActiveSims(): List<SimInfo> {
        if (!hasPhonePermission() || subscriptionManager == null) {
            return emptyList()
        }

        val list = try {
            subscriptionManager.activeSubscriptionInfoList ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }

        return list.mapIndexed { index, info ->
            val slot = info.simSlotIndex + 1
            val fallbackSlot = if (slot > 0) slot else index + 1
            val carrier = resolveCarrierName(info)
            val number = resolvePhoneNumber(info)
            SimInfo(
                subscriptionId = info.subscriptionId,
                simSlot = fallbackSlot,
                carrierName = carrier,
                phoneNumber = number
            )
        }
    }

    fun getSubscriptionIdForSlot(slot: Int): Int? {
        if (!hasPhonePermission() || subscriptionManager == null) {
            return null
        }
        val list = try {
            subscriptionManager.activeSubscriptionInfoList ?: emptyList()
        } catch (_: SecurityException) {
            return null
        }
        val match = list.firstOrNull { (it.simSlotIndex + 1) == slot }
        return match?.subscriptionId
    }

    private fun resolveCarrierName(info: SubscriptionInfo): String {
        val name = info.displayName?.toString()?.trim()
        if (!name.isNullOrBlank() && !name.contains("SIM", ignoreCase = true)) {
            return name
        }
        val carrierName = info.carrierName?.toString()?.trim()
        if (!carrierName.isNullOrBlank()) {
            return carrierName
        }
        val mccString = info.mccString ?: ""
        val mncString = info.mncString ?: ""
        val mccMnc = "$mccString$mncString"
        return when (mccMnc) {
            "47001" -> "Grameenphone"
            "47002" -> "Robi"
            "47003" -> "Banglalink"
            "47004" -> "Teletalk"
            "47007" -> "Airtel"
            else -> name?.ifBlank { "Carrier" } ?: "Carrier"
        }
    }

    private fun resolvePhoneNumber(info: SubscriptionInfo): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && subscriptionManager != null) {
            try {
                val num = subscriptionManager.getPhoneNumber(info.subscriptionId)
                if (!num.isNullOrBlank()) return num
            } catch (_: SecurityException) {}
        }
        @Suppress("DEPRECATION")
        val legacy = info.number
        return if (!legacy.isNullOrBlank()) legacy else null
    }
}
