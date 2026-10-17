package com.gosms.gateway.ui.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gosms.gateway.BuildConfig
import com.gosms.gateway.data.api.ApiClient
import com.gosms.gateway.data.db.AppDatabase
import com.gosms.gateway.data.db.SmsJob
import com.gosms.gateway.data.model.RegisterRequest
import com.gosms.gateway.data.model.SimInfo
import com.gosms.gateway.data.sim.SimManager
import com.gosms.gateway.data.storage.SecureStorage
import com.gosms.gateway.data.validator.UrlValidator
import com.gosms.gateway.service.GatewayConnectionState
import com.gosms.gateway.service.GatewayService
import com.gosms.gateway.service.RateLimiter
import com.gosms.gateway.service.SmsSender
import com.gosms.gateway.worker.WatchdogWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class ConnectUiState(
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val isManualSheetOpen: Boolean = false,
    val isScannerOpen: Boolean = false
)

class GatewayViewModel(application: Application) : AndroidViewModel(application) {
    val secureStorage = SecureStorage(application)
    val appDatabase = AppDatabase.getInstance(application)
    val simManager = SimManager(application)
    private val apiClient = ApiClient(secureStorage)
    private val rateLimiter = RateLimiter(secureStorage.getMaxPerMinute())
    private val smsSender = SmsSender(application, simManager, appDatabase, rateLimiter)

    private val _connectUiState = MutableStateFlow(ConnectUiState())
    val connectUiState: StateFlow<ConnectUiState> = _connectUiState.asStateFlow()

    private val _isPaired = MutableStateFlow(secureStorage.isPaired())
    val isPaired: StateFlow<Boolean> = _isPaired.asStateFlow()

    val connectionState: StateFlow<GatewayConnectionState> = GatewayService.connectionState
    val lastHeartbeatTimestamp: StateFlow<Long> = GatewayService.lastHeartbeatTimestamp

    private val startOfDayMs: Long
        get() {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

    val sentTodayCount: StateFlow<Int> = appDatabase.smsJobDao()
        .observeSentTodayCount(startOfDayMs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val failedTodayCount: StateFlow<Int> = appDatabase.smsJobDao()
        .observeFailedTodayCount(startOfDayMs)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingCount: StateFlow<Int> = appDatabase.smsJobDao()
        .observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentJobs: StateFlow<List<SmsJob>> = appDatabase.smsJobDao()
        .observeRecentJobs(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _sims = MutableStateFlow<List<SimInfo>>(emptyList())
    val sims: StateFlow<List<SimInfo>> = _sims.asStateFlow()

    private val _autoStart = MutableStateFlow(secureStorage.getAutoStart())
    val autoStart: StateFlow<Boolean> = _autoStart.asStateFlow()

    private val _defaultSimSlot = MutableStateFlow(secureStorage.getDefaultSimSlot())
    val defaultSimSlot: StateFlow<Int?> = _defaultSimSlot.asStateFlow()

    private val _pollIntervalMs = MutableStateFlow(secureStorage.getPollIntervalMs())
    val pollIntervalMs: StateFlow<Long> = _pollIntervalMs.asStateFlow()

    init {
        refreshSims()
        if (secureStorage.isPaired()) {
            GatewayService.start(application)
            WatchdogWorker.schedule(application)
        }
    }

    fun refreshSims() {
        _sims.value = simManager.getActiveSims()
    }

    fun setManualSheetOpen(isOpen: Boolean) {
        _connectUiState.value = _connectUiState.value.copy(
            isManualSheetOpen = isOpen,
            errorMessage = if (!isOpen) null else _connectUiState.value.errorMessage
        )
    }

    fun setScannerOpen(isOpen: Boolean) {
        _connectUiState.value = _connectUiState.value.copy(isScannerOpen = isOpen)
    }

    fun register(rawUrl: String, pairingCode: String) {
        val normalizedUrl = UrlValidator.normalize(rawUrl)
        if (normalizedUrl == null) {
            _connectUiState.value = _connectUiState.value.copy(
                errorMessage = "Only HTTPS URLs are allowed. Localhost and private IPs are blocked."
            )
            return
        }

        val cleanCode = pairingCode.trim()
        if (cleanCode.length != 6) {
            _connectUiState.value = _connectUiState.value.copy(
                errorMessage = "Pairing code must be 6 digits."
            )
            return
        }

        _connectUiState.value = _connectUiState.value.copy(
            isConnecting = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                refreshSims()
                val deviceId = secureStorage.getDeviceId()
                val request = RegisterRequest(
                    pairingCode = cleanCode,
                    deviceId = deviceId,
                    deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
                    model = Build.MODEL,
                    androidVersion = Build.VERSION.RELEASE,
                    appVersion = BuildConfig.VERSION_NAME,
                    sims = _sims.value
                )

                val response = apiClient.getService(normalizedUrl).register(request)
                if (response.isSuccessful && response.body()?.ok == true) {
                    val body = response.body()!!
                    val token = body.deviceToken ?: ""
                    val assignedDeviceId = body.deviceId ?: deviceId
                    val pollInterval = body.pollIntervalMs ?: 5000L
                    val heartbeatInterval = body.heartbeatIntervalMs ?: 60000L
                    val maxPerMinute = body.maxPerMinute ?: 20

                    secureStorage.saveRegistration(
                        serverUrl = normalizedUrl,
                        token = token,
                        deviceId = assignedDeviceId,
                        pollIntervalMs = pollInterval,
                        heartbeatIntervalMs = heartbeatInterval,
                        maxPerMinute = maxPerMinute
                    )

                    _isPaired.value = true
                    _connectUiState.value = ConnectUiState()

                    GatewayService.start(getApplication())
                    WatchdogWorker.schedule(getApplication())
                } else {
                    val errorMsg = response.body()?.message ?: "Pairing failed: ${response.code()}"
                    _connectUiState.value = _connectUiState.value.copy(
                        isConnecting = false,
                        errorMessage = errorMsg
                    )
                }
            } catch (e: Exception) {
                _connectUiState.value = _connectUiState.value.copy(
                    isConnecting = false,
                    errorMessage = e.message ?: "Network error occurred."
                )
            }
        }
    }

    fun updateAutoStart(enabled: Boolean) {
        secureStorage.setAutoStart(enabled)
        _autoStart.value = enabled
    }

    fun updateDefaultSimSlot(slot: Int?) {
        secureStorage.setDefaultSimSlot(slot)
        _defaultSimSlot.value = slot
    }

    fun updatePollInterval(ms: Long) {
        secureStorage.setPollIntervalMs(ms)
        _pollIntervalMs.value = ms
    }

    fun sendTestSms(recipient: String, message: String, slot: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = smsSender.sendDirectTestSms(recipient, message, slot)
            onResult(success)
        }
    }

    fun unpair() {
        viewModelScope.launch {
            try {
                apiClient.getService().unpair()
            } catch (_: Exception) {}

            secureStorage.clearSession()
            appDatabase.smsJobDao().deleteAll()
            GatewayService.stop(getApplication())
            WatchdogWorker.cancel(getApplication())
            _isPaired.value = false
        }
    }
}
