package com.gosms.gateway.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsJobDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(job: SmsJob): Long

    @Update
    suspend fun update(job: SmsJob)

    @Query("SELECT * FROM sms_jobs WHERE message_id = :messageId LIMIT 1")
    suspend fun getByMessageId(messageId: String): SmsJob?

    @Query("SELECT * FROM sms_jobs WHERE state = 'RECEIVED' ORDER BY id ASC")
    suspend fun getQueuedJobs(): List<SmsJob>

    @Query("SELECT * FROM sms_jobs WHERE report_pending = 1 ORDER BY id ASC")
    suspend fun getPendingReports(): List<SmsJob>

    @Query("SELECT * FROM sms_jobs ORDER BY id DESC LIMIT :limit")
    fun observeRecentJobs(limit: Int = 200): Flow<List<SmsJob>>

    @Query("SELECT COUNT(*) FROM sms_jobs WHERE state IN ('SENT', 'DELIVERED') AND updated_at >= :startOfDayMs")
    fun observeSentTodayCount(startOfDayMs: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM sms_jobs WHERE state = 'FAILED' AND updated_at >= :startOfDayMs")
    fun observeFailedTodayCount(startOfDayMs: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM sms_jobs WHERE state IN ('RECEIVED', 'SENDING')")
    fun observePendingCount(): Flow<Int>

    @Query("DELETE FROM sms_jobs WHERE report_pending = 0 AND updated_at < :thresholdMs")
    suspend fun deleteOldReportedJobs(thresholdMs: Long): Int

    @Query("DELETE FROM sms_jobs")
    suspend fun deleteAll()
}
