package com.gosms.gateway.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import com.gosms.gateway.data.db.AppDatabase
import com.gosms.gateway.data.db.SmsJob
import com.gosms.gateway.data.sim.SimManager
import java.util.concurrent.atomic.AtomicInteger

class SmsSender(
    private val context: Context,
    private val simManager: SimManager,
    private val appDatabase: AppDatabase,
    private val rateLimiter: RateLimiter
) {
    private val requestCodeGenerator = AtomicInteger(100)

    suspend fun sendJob(job: SmsJob): Boolean {
        rateLimiter.acquire()

        val targetSlot = job.simSlot ?: 1
        val subscriptionId = simManager.getSubscriptionIdForSlot(targetSlot)
        if (subscriptionId == null) {
            appDatabase.smsJobDao().update(
                job.copy(
                    state = "FAILED",
                    errorCode = "SIM_NOT_AVAILABLE",
                    errorDetail = "Target SIM slot $targetSlot not available",
                    reportPending = 1,
                    updatedAt = System.currentTimeMillis()
                )
            )
            return false
        }

        appDatabase.smsJobDao().update(
            job.copy(
                state = "SENDING",
                updatedAt = System.currentTimeMillis()
            )
        )

        return try {
            val baseSmsManager = context.getSystemService(SmsManager::class.java)
            val smsManager = baseSmsManager.createForSubscriptionId(subscriptionId)

            val parts = smsManager.divideMessage(job.body)
            val partsCount = parts.size

            val sentIntents = ArrayList<PendingIntent>(partsCount)
            val deliveryIntents = ArrayList<PendingIntent>(partsCount)

            for (i in 0 until partsCount) {
                val sentReq = requestCodeGenerator.incrementAndGet()
                val sentIntent = Intent(context, SmsStatusReceiver::class.java).apply {
                    action = SmsStatusReceiver.ACTION_SMS_SENT
                    putExtra(SmsStatusReceiver.EXTRA_MESSAGE_ID, job.messageId)
                    putExtra(SmsStatusReceiver.EXTRA_LEASE_ID, job.leaseId)
                    putExtra(SmsStatusReceiver.EXTRA_PARTS, partsCount)
                    putExtra(SmsStatusReceiver.EXTRA_SIM_SLOT, targetSlot)
                }
                sentIntents.add(
                    PendingIntent.getBroadcast(
                        context,
                        sentReq,
                        sentIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )

                val delivReq = requestCodeGenerator.incrementAndGet()
                val delivIntent = Intent(context, SmsStatusReceiver::class.java).apply {
                    action = SmsStatusReceiver.ACTION_SMS_DELIVERED
                    putExtra(SmsStatusReceiver.EXTRA_MESSAGE_ID, job.messageId)
                    putExtra(SmsStatusReceiver.EXTRA_LEASE_ID, job.leaseId)
                    putExtra(SmsStatusReceiver.EXTRA_PARTS, partsCount)
                    putExtra(SmsStatusReceiver.EXTRA_SIM_SLOT, targetSlot)
                }
                deliveryIntents.add(
                    PendingIntent.getBroadcast(
                        context,
                        delivReq,
                        delivIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }

            smsManager.sendMultipartTextMessage(
                job.recipient,
                null,
                parts,
                sentIntents,
                deliveryIntents
            )
            true
        } catch (e: Exception) {
            appDatabase.smsJobDao().update(
                job.copy(
                    state = "FAILED",
                    errorCode = "SEND_EXCEPTION",
                    errorDetail = e.message ?: "Unknown error",
                    reportPending = 1,
                    updatedAt = System.currentTimeMillis()
                )
            )
            false
        }
    }

    suspend fun sendDirectTestSms(
        recipient: String,
        body: String,
        slot: Int
    ): Boolean {
        rateLimiter.acquire()
        val subscriptionId = simManager.getSubscriptionIdForSlot(slot) ?: return false
        return try {
            val baseSmsManager = context.getSystemService(SmsManager::class.java)
            val smsManager = baseSmsManager.createForSubscriptionId(subscriptionId)
            val parts = smsManager.divideMessage(body)
            smsManager.sendMultipartTextMessage(recipient, null, parts, null, null)
            true
        } catch (_: Exception) {
            false
        }
    }
}
