package com.gosms.gateway.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sms_jobs",
    indices = [
        Index(value = ["message_id"], unique = true),
        Index(value = ["state"]),
        Index(value = ["report_pending"])
    ]
)
data class SmsJob(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "message_id")
    val messageId: String,

    @ColumnInfo(name = "lease_id")
    val leaseId: String,

    @ColumnInfo(name = "recipient")
    val recipient: String,

    @ColumnInfo(name = "body")
    val body: String,

    @ColumnInfo(name = "sim_slot")
    val simSlot: Int?,

    @ColumnInfo(name = "state")
    val state: String,

    @ColumnInfo(name = "error_code")
    val errorCode: String? = null,

    @ColumnInfo(name = "error_detail")
    val errorDetail: String? = null,

    @ColumnInfo(name = "parts", defaultValue = "1")
    val parts: Int = 1,

    @ColumnInfo(name = "report_pending", defaultValue = "0")
    val reportPending: Int = 0,

    @ColumnInfo(name = "last_reported_state")
    val lastReportedState: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
