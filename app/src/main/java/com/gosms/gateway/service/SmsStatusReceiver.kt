package com.gosms.gateway.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import com.gosms.gateway.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID) ?: return
        val leaseId = intent.getStringExtra(EXTRA_LEASE_ID) ?: ""
        val parts = intent.getIntExtra(EXTRA_PARTS, 1)
        val resultCode = resultCode

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val dao = db.smsJobDao()
                val existing = dao.getByMessageId(messageId)

                if (action == ACTION_SMS_SENT) {
                    val isSuccess = resultCode == Activity.RESULT_OK
                    val state = if (isSuccess) "SENT" else "FAILED"
                    val errorCode = if (isSuccess) null else mapResultCodeToError(resultCode)
                    val errorDetail = if (isSuccess) null else "Result code: $resultCode"

                    if (existing != null) {
                        dao.update(
                            existing.copy(
                                state = state,
                                errorCode = errorCode,
                                errorDetail = errorDetail,
                                parts = parts,
                                reportPending = 1,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                } else if (action == ACTION_SMS_DELIVERED) {
                    if (resultCode == Activity.RESULT_OK && existing != null) {
                        dao.update(
                            existing.copy(
                                state = "DELIVERED",
                                reportPending = 1,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun mapResultCodeToError(code: Int): String {
        return when (code) {
            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "GENERIC_FAILURE"
            SmsManager.RESULT_ERROR_RADIO_OFF -> "RADIO_OFF"
            SmsManager.RESULT_ERROR_NULL_PDU -> "NULL_PDU"
            SmsManager.RESULT_ERROR_NO_SERVICE -> "NO_SERVICE"
            SmsManager.RESULT_ERROR_LIMIT_EXCEEDED -> "LIMIT_EXCEEDED"
            else -> "ERROR_CODE_$code"
        }
    }

    companion object {
        const val ACTION_SMS_SENT = "com.gosms.gateway.SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.gosms.gateway.SMS_DELIVERED"
        const val EXTRA_MESSAGE_ID = "extra_message_id"
        const val EXTRA_LEASE_ID = "extra_lease_id"
        const val EXTRA_PARTS = "extra_parts"
        const val EXTRA_SIM_SLOT = "extra_sim_slot"
    }
}
