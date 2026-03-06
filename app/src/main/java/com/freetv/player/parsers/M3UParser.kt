package com.freetv.player.parsers

import android.util.Log
import com.freetv.player.models.Channel
import java.io.BufferedReader
import java.io.StringReader
import java.util.UUID

object M3UParser {

    private const val TAG = "M3UParser"
    private const val EXTINF = "#EXTINF:"
    private const val TVG_ID = "tvg-id=\""
    private const val TVG_NAME = "tvg-name=\""
    private const val TVG_LOGO = "tvg-logo=\""
    private const val TVG_COUNTRY = "tvg-country=\""
    private const val TVG_LANGUAGE = "tvg-language=\""
    private const val GROUP_TITLE = "group-title=\""

    fun parse(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        try {
            val reader = BufferedReader(StringReader(content))
            var line: String?
            var currentInfo: String? = null

            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                when {
                    trimmed.startsWith(EXTINF) -> {
                        currentInfo = trimmed
                    }
                    trimmed.isNotEmpty() && !trimmed.startsWith("#") && currentInfo != null -> {
                        val channel = parseChannel(currentInfo!!, trimmed)
                        if (channel != null && isValidStreamUrl(trimmed)) {
                            channels.add(channel)
                        }
                        currentInfo = null
                    }
                    trimmed.isEmpty() -> {
                        // skip blank lines
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing M3U: ${e.message}")
        }

        Log.d(TAG, "Parsed ${channels.size} channels")
        return channels
    }

    private fun parseChannel(info: String, streamUrl: String): Channel? {
        return try {
            val tvgId = extractAttribute(info, TVG_ID)
            val tvgName = extractAttribute(info, TVG_NAME)
            val logo = extractAttribute(info, TVG_LOGO)
            val country = extractAttribute(info, TVG_COUNTRY)
            val language = extractAttribute(info, TVG_LANGUAGE)
            val groupTitle = extractAttribute(info, GROUP_TITLE)

            // Extract display name (after the last comma)
            val displayName = extractDisplayName(info, tvgName)

            if (displayName.isBlank()) return null

            Channel(
                id = UUID.randomUUID().toString(),
                name = displayName,
                logoUrl = logo,
                streamUrl = streamUrl,
                country = countryCodeToName(country),
                countryCode = country.uppercase(),
                category = normalizeCategory(groupTitle),
                language = language,
                tvgId = tvgId
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse channel: ${e.message}")
            null
        }
    }

    private fun extractAttribute(info: String, key: String): String {
        val start = info.indexOf(key)
        if (start == -1) return ""
        val valueStart = start + key.length
        val end = info.indexOf("\"", valueStart)
        if (end == -1) return ""
        return info.substring(valueStart, end).trim()
    }

    private fun extractDisplayName(info: String, tvgName: String): String {
        // Try getting name after last comma (standard M3U format)
        val lastComma = info.lastIndexOf(",")
        val nameFromInfo = if (lastComma != -1) info.substring(lastComma + 1).trim() else ""

        return when {
            nameFromInfo.isNotBlank() -> nameFromInfo
            tvgName.isNotBlank() -> tvgName
            else -> ""
        }
    }

    private fun isValidStreamUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://") || url.startsWith("rtmp://")
    }

    private fun normalizeCategory(raw: String): String {
        if (raw.isBlank()) return "General"
        val lower = raw.lowercase()
        return when {
            lower.contains("news") -> "News"
            lower.contains("sport") -> "Sports"
            lower.contains("movie") || lower.contains("film") || lower.contains("cinema") -> "Movies"
            lower.contains("kid") || lower.contains("child") || lower.contains("cartoon") -> "Kids"
            lower.contains("music") -> "Music"
            lower.contains("entertain") -> "Entertainment"
            lower.contains("doc") -> "Documentary"
            lower.contains("cook") || lower.contains("food") -> "Lifestyle"
            lower.contains("business") || lower.contains("finance") -> "Business"
            else -> raw.replaceFirstChar { it.uppercase() }.trim()
        }
    }

    private fun countryCodeToName(code: String): String {
        return when (code.uppercase()) {
            "US" -> "United States"
            "GB", "UK" -> "United Kingdom"
            "CA" -> "Canada"
            "AU" -> "Australia"
            "DE" -> "Germany"
            "FR" -> "France"
            "IT" -> "Italy"
            "ES" -> "Spain"
            "BR" -> "Brazil"
            "MX" -> "Mexico"
            "IN" -> "India"
            "JP" -> "Japan"
            "CN" -> "China"
            "RU" -> "Russia"
            "KR" -> "South Korea"
            "AR" -> "Argentina"
            "TR" -> "Turkey"
            "SA" -> "Saudi Arabia"
            "EG" -> "Egypt"
            "NG" -> "Nigeria"
            "ZA" -> "South Africa"
            "PK" -> "Pakistan"
            "BD" -> "Bangladesh"
            "ID" -> "Indonesia"
            "PH" -> "Philippines"
            "TH" -> "Thailand"
            "VN" -> "Vietnam"
            "UA" -> "Ukraine"
            "PL" -> "Poland"
            "NL" -> "Netherlands"
            "BE" -> "Belgium"
            "SE" -> "Sweden"
            "NO" -> "Norway"
            "DK" -> "Denmark"
            "FI" -> "Finland"
            "CH" -> "Switzerland"
            "AT" -> "Austria"
            "PT" -> "Portugal"
            "GR" -> "Greece"
            "CZ" -> "Czech Republic"
            "HU" -> "Hungary"
            "RO" -> "Romania"
            "IR" -> "Iran"
            "IQ" -> "Iraq"
            "IL" -> "Israel"
            "AE" -> "UAE"
            "CO" -> "Colombia"
            "CL" -> "Chile"
            "PE" -> "Peru"
            "VE" -> "Venezuela"
            else -> if (code.isBlank()) "International" else code.uppercase()
        }
    }
}
