package com.gosms.gateway.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.gosms.gateway.BuildConfig
import com.gosms.gateway.MainActivity
import com.gosms.gateway.R
import com.gosms.gateway.data.api.ApiClient
import com.gosms.gateway.data.db.AppDatabase
import com.gosms.gateway.data.db.SmsJob
import com.gosms.gateway.data.model.HeartbeatRequest
import com.gosms.gateway.data.model.StatusReportRequest
import com.gosms.gateway.data.sim.SimManager
import com.gosms.gateway.data.storage.SecureStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class GatewayConnectionState {
    ACTIVE,
    RECONNECTING,
    DISCONNECTED
}

class GatewayService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var secureStorage: SecureStorage
    private lateinit var appDatabase: AppDatabase
    private lateinit var simManager: SimManager
    private lateinit var rateLimiter: RateLimiter
    private lateinit var smsSender: SmsSender
    private lateinit var apiClient: ApiClient

    private var loopJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        secureStorage = SecureStorage(this)
        appDatabase = AppDatabase.getInstance(this)
        simManager = SimManager(this)
        rateLimiter = RateLimiter(secureStorage.getMaxPerMinute())
        smsSender = SmsSender(this, simManager, appDatabase, rateLimiter)
        apiClient = ApiClient(secureStorage) {
            handleRevocation()
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "GoSMSGateway:SendingWakeLock"
        )

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!secureStorage.isPaired()) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification(0))
        startGatewayLoop()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        loopJob?.cancel()
        serviceScope.cancel()
        _connectionState.value = GatewayConnectionState.DISCONNECTED
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    private fun startGatewayLoop() {
        if (loopJob?.isActive == true) return

        loopJob = serviceScope.launch {
            var lastHeartbeatTime = 0L
            var currentBackoffMs = 0L

            while (isActive) {
                if (!secureStorage.isPaired()) {
                    handleRevocation()
                    break
                }

                val now = System.currentTimeMillis()
                val heartbeatInterval = secureStorage.getHeartbeatIntervalMs()
                val pollInterval = secureStorage.getPollIntervalMs()

                var loopSuccess = true

                try {
                    if (now - lastHeartbeatTime >= heartbeatInterval) {
                        sendHeartbeat()
                        lastHeartbeatTime = System.currentTimeMillis()
                        _lastHeartbeatTimestamp.value = lastHeartbeatTime
                    }

                    flushPendingReports()
                    pollAndProcessMessages()
                    processQueuedJobs()

                    val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
                    appDatabase.smsJobDao().deleteOldReportedJobs(thirtyDaysAgo)

                    _connectionState.value = GatewayConnectionState.ACTIVE
                    currentBackoffMs = 0L
                } catch (_: Exception) {
                    loopSuccess = false
                    _connectionState.value = GatewayConnectionState.RECONNECTING
                    currentBackoffMs = if (currentBackoffMs == 0L) 5000L else (currentBackoffMs * 2).coerceAtMost(60000L)
                }

                val delayTime = if (loopSuccess) pollInterval else currentBackoffMs
                delay(delayTime)
            }
        }
    }

    private suspend fun sendHeartbeat() {
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) ((level * 100) / scale) else 0

        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: 0
        val isCharging = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS

        val pendingJobs = appDatabase.smsJobDao().getQueuedJobs()
        val sims = simManager.getActiveSims()

        val req = HeartbeatRequest(
            timestamp = System.currentTimeMillis(),
            batteryPct = batteryPct,
            charging = isCharging,
            networkType = "mobile",
            appVersion = BuildConfig.VERSION_NAME,
            pendingCount = pendingJobs.size,
            sims = sims
        )

        val resp = apiClient.getService().heartbeat(req)
        if (resp.isSuccessful) {
            val body = resp.body()
            if (body != null && body.ok) {
                body.pollIntervalMs?.let {
                    secureStorage.setPollIntervalMs(it)
                }
                body.maxPerMinute?.let {
                    secureStorage.setMaxPerMinute(it)
                    rateLimiter.updateMaxPerMinute(it)
                }
            }
        } else if (resp.code() == 401 || resp.code() == 403) {
            handleRevocation()
        } else {
            throw Exception("Heartbeat failed with code: ${resp.code()}")
        }
    }

    private suspend fun flushPendingReports() {
        val pendingList = appDatabase.smsJobDao().getPendingReports()
        val service = apiClient.getService()

        for (job in pendingList) {
            val req = StatusReportRequest(
                messageId = job.messageId,
                leaseId = job.leaseId,
                status = job.state,
                errorCode = job.errorCode,
                errorDetail = job.errorDetail,
                parts = job.parts,
                timestamp = job.updatedAt
            )
            val resp = service.reportStatus(req)
            if (resp.isSuccessful && resp.body()?.ok == true) {
                appDatabase.smsJobDao().update(
                    job.copy(
                        reportPending = 0,
                        lastReportedState = job.state,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else if (resp.code() == 401 || resp.code() == 403) {
                handleRevocation()
                return
            }
        }
    }

    private suspend fun pollAndProcessMessages() {
        val resp = apiClient.getService().getMessages(10)
        if (!resp.isSuccessful) {
            if (resp.code() == 401 || resp.code() == 403) {
                handleRevocation()
            } else {
                throw Exception("Poll failed with code: ${resp.code()}")
            }
            return
        }

        val messages = resp.body()?.messages ?: emptyList()
        val currentDeviceId = secureStorage.getDeviceId()
        val activeSims = simManager.getActiveSims()
        val defaultSimSlot = secureStorage.getDefaultSimSlot()

        for (msg in messages) {
            if (msg.deviceId != currentDeviceId) {
                val reportReq = StatusReportRequest(
                    messageId = msg.messageId,
                    leaseId = msg.leaseId,
                    status = "FAILED",
                    errorCode = "DEVICE_MISMATCH",
                    errorDetail = "Target device id mismatch",
                    parts = 1,
                    timestamp = System.currentTimeMillis()
                )
                apiClient.getService().reportStatus(reportReq)
                continue
            }

            val existing = appDatabase.smsJobDao().getByMessageId(msg.messageId)
            if (existing != null) {
                val reportReq = StatusReportRequest(
                    messageId = existing.messageId,
                    leaseId = existing.leaseId,
                    status = existing.state,
                    errorCode = existing.errorCode,
                    errorDetail = existing.errorDetail,
                    parts = existing.parts,
                    timestamp = existing.updatedAt
                )
                apiClient.getService().reportStatus(reportReq)
                continue
            }

            val chosenSlot = msg.simSlot ?: defaultSimSlot ?: activeSims.firstOrNull()?.simSlot
            if (msg.simSlot != null && activeSims.none { it.simSlot == msg.simSlot }) {
                val newJob = SmsJob(
                    messageId = msg.messageId,
                    leaseId = msg.leaseId,
                    recipient = msg.to,
                    body = msg.body,
                    simSlot = msg.simSlot,
                    state = "FAILED",
                    errorCode = "SIM_NOT_AVAILABLE",
                    errorDetail = "Specified SIM slot ${msg.simSlot} is not available",
                    parts = 1,
                    reportPending = 1,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                appDatabase.smsJobDao().insert(newJob)
                continue
            }

            val newJob = SmsJob(
                messageId = msg.messageId,
                leaseId = msg.leaseId,
                recipient = msg.to,
                body = msg.body,
                simSlot = chosenSlot,
                state = "RECEIVED",
                parts = 1,
                reportPending = 0,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            appDatabase.smsJobDao().insert(newJob)
        }
    }

    private suspend fun processQueuedJobs() {
        val queuedJobs = appDatabase.smsJobDao().getQueuedJobs()
        updateNotification(queuedJobs.size)

        for (job in queuedJobs) {
            try {
                wakeLock?.acquire(15000L)
                smsSender.sendJob(job)
            } finally {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
            }
        }
    }

    private fun handleRevocation() {
        serviceScope.launch {
            secureStorage.clearSession()
            appDatabase.smsJobDao().deleteAll()
            _connectionState.value = GatewayConnectionState.DISCONNECTED
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GoSMS Gateway Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps GoSMS Gateway active in background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(pendingCount: Int): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pendingText = getString(R.string.service_notification_pending, pendingCount)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(pendingText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(pendingCount: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(pendingCount))
    }

    companion object {
        const val ACTION_START = "com.gosms.gateway.START"
        const val ACTION_STOP = "com.gosms.gateway.STOP"
        private const val CHANNEL_ID = "gosms_gateway_channel"
        private const val NOTIFICATION_ID = 1001

        private val _connectionState = MutableStateFlow(GatewayConnectionState.DISCONNECTED)
        val connectionState: StateFlow<GatewayConnectionState> = _connectionState.asStateFlow()

        private val _lastHeartbeatTimestamp = MutableStateFlow(0L)
        val lastHeartbeatTimestamp: StateFlow<Long> = _lastHeartbeatTimestamp.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, GatewayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, GatewayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
