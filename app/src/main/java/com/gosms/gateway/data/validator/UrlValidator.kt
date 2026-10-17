package com.gosms.gateway.data.validator

import java.net.InetAddress
import java.net.URI

object UrlValidator {
    fun normalize(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return null
        }
        val uri = try {
            URI(trimmed)
        } catch (_: Exception) {
            return null
        }

        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "https") {
            return null
        }

        val host = uri.host?.lowercase() ?: return null
        if (isBlockedHost(host)) {
            return null
        }

        val portPart = if (uri.port != -1 && uri.port != 443) ":${uri.port}" else ""
        val pathPart = uri.rawPath?.trimEnd('/') ?: ""
        return "https://$host$portPart$pathPart"
    }

    private fun isBlockedHost(host: String): Boolean {
        if (host == "localhost" || host == "127.0.0.1" || host == "::1" || host == "0.0.0.0") {
            return true
        }

        val address = try {
            InetAddress.getByName(host)
        } catch (_: Exception) {
            return false
        }

        if (address.isLoopbackAddress || address.isAnyLocalAddress || address.isLinkLocalAddress || address.isSiteLocalAddress) {
            return true
        }

        val bytes = address.address
        if (bytes.size == 4) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            if (b0 == 10) return true
            if (b0 == 172 && b1 in 16..31) return true
            if (b0 == 192 && b1 == 168) return true
            if (b0 == 169 && b1 == 254) return true
        } else if (bytes.size == 16) {
            val b0 = bytes[0].toInt() and 0xFF
            if ((b0 and 0xFE) == 0xFC) return true
            if (b0 == 0xFE && (bytes[1].toInt() and 0xC0) == 0x80) return true
        }
        return false
    }
}
