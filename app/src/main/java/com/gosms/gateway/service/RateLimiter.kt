package com.gosms.gateway.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RateLimiter(private var maxPerMinute: Int = 20) {
    private val mutex = Mutex()
    private val timestamps = mutableListOf<Long>()
    private var lastSendTime: Long = 0L

    fun updateMaxPerMinute(limit: Int) {
        if (limit > 0) {
            maxPerMinute = limit
        }
    }

    suspend fun acquire() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val windowStart = now - 60000L
            timestamps.removeAll { it < windowStart }

            if (timestamps.size >= maxPerMinute) {
                val oldestInWindow = timestamps.first()
                val waitTime = (oldestInWindow + 60000L) - now
                if (waitTime > 0) {
                    delay(waitTime)
                }
            }

            val elapsedSinceLast = System.currentTimeMillis() - lastSendTime
            val minSpacingMs = 2000L
            if (elapsedSinceLast < minSpacingMs) {
                delay(minSpacingMs - elapsedSinceLast)
            }

            val actualNow = System.currentTimeMillis()
            timestamps.add(actualNow)
            lastSendTime = actualNow
        }
    }
}
