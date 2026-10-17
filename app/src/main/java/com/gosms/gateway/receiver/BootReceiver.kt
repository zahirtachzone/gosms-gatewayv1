package com.gosms.gateway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gosms.gateway.data.storage.SecureStorage
import com.gosms.gateway.service.GatewayService
import com.gosms.gateway.worker.WatchdogWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val storage = SecureStorage(context)
            if (storage.isPaired() && storage.getAutoStart()) {
                GatewayService.start(context)
                WatchdogWorker.schedule(context)
            }
        }
    }
}
