package com.freetv.player.repository

import android.content.Context
import android.util.Log
import com.freetv.player.models.Channel
import com.freetv.player.parsers.M3UParser
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class ChannelRepository(private val context: Context) {

    companion object {
        private const val TAG = "ChannelRepository"
        private const val M3U_URL = "https://iptv-org.github.io/iptv/index.m3u"
        private const val CACHE_FILE = "channels_cache.json"
        private const val FAVORITES_FILE = "favorites.json"
        private const val RECENTLY_WATCHED_FILE = "recently_watched.json"
        private const val CACHE_EXPIRY_MS = 3_600_000L // 1 hour
        private const val MAX_RECENT = 20
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private var cachedChannels: List<Channel>? = null

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
        object Loading : Result<Nothing>()
    }

    suspend fun fetchChannels(forceRefresh: Boolean = false): Result<List<Channel>> {
        return withContext(Dispatchers.IO) {
            try {
                // Return memory cache if available and not forcing refresh
                if (!forceRefresh && cachedChannels != null) {
                    return@withContext Result.Success(cachedChannels!!)
                }

                // Check disk cache
                if (!forceRefresh) {
                    val diskCached = loadFromDiskCache()
                    if (diskCached != null) {
                        cachedChannels = diskCached
                        return@withContext Result.Success(diskCached)
                    }
                }

                // Fetch from network
                Log.d(TAG, "Fetching channels from network...")
                val request = Request.Builder()
                    .url(M3U_URL)
                    .header("User-Agent", "FreeTV-Player/1.0")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    // Try to return stale cache on network error
                    val staleCache = loadFromDiskCache(ignoreExpiry = true)
                    if (staleCache != null) {
                        cachedChannels = staleCache
                        return@withContext Result.Success(staleCache)
                    }
                    return@withContext Result.Error("Network error: ${response.code}")
                }

                val body = response.body?.string()
                    ?: return@withContext Result.Error("Empty response body")

                val channels = M3UParser.parse(body)
                if (channels.isEmpty()) {
                    return@withContext Result.Error("No channels found in playlist")
                }

                // Merge with favorites
                val favorites = loadFavoriteIds()
                val merged = channels.map { ch ->
                    ch.copy(isFavorite = favorites.contains(ch.tvgId.ifBlank { ch.name }))
                }

                // Save to disk cache
                saveToDiskCache(merged)
                cachedChannels = merged

                Log.d(TAG, "Loaded ${merged.size} channels")
                Result.Success(merged)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch channels", e)
                val staleCache = loadFromDiskCache(ignoreExpiry = true)
                if (staleCache != null) {
                    Result.Success(staleCache)
                } else {
                    Result.Error("Failed to load channels: ${e.localizedMessage}", e)
                }
            }
        }
    }

    suspend fun toggleFavorite(channel: Channel): Boolean {
        return withContext(Dispatchers.IO) {
            val favorites = loadFavoriteIds().toMutableSet()
            val key = channel.tvgId.ifBlank { channel.name }
            val newState = if (favorites.contains(key)) {
                favorites.remove(key)
                false
            } else {
                favorites.add(key)
                true
            }
            saveFavoriteIds(favorites)

            // Update memory cache
            cachedChannels = cachedChannels?.map { ch ->
                val chKey = ch.tvgId.ifBlank { ch.name }
                if (chKey == key) ch.copy(isFavorite = newState) else ch
            }
            newState
        }
    }

    suspend fun addToRecentlyWatched(channel: Channel) {
        withContext(Dispatchers.IO) {
            val recent = loadRecentlyWatched().toMutableList()
            recent.removeAll { it.name == channel.name }
            recent.add(0, channel.copy(lastWatched = System.currentTimeMillis()))
            if (recent.size > MAX_RECENT) {
                recent.subList(MAX_RECENT, recent.size).clear()
            }
            saveRecentlyWatched(recent)
        }
    }

    suspend fun getRecentlyWatched(): List<Channel> {
        return withContext(Dispatchers.IO) {
            loadRecentlyWatched()
        }
    }

    suspend fun getFavoriteChannels(): List<Channel> {
        return withContext(Dispatchers.IO) {
            cachedChannels?.filter { it.isFavorite } ?: run {
                fetchChannels()
                cachedChannels?.filter { it.isFavorite } ?: emptyList()
            }
        }
    }

    fun getAvailableCountries(): List<String> {
        return cachedChannels
            ?.map { it.country }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?.sorted()
            ?.let { listOf("All") + it }
            ?: listOf("All")
    }

    fun getAvailableCategories(): List<String> {
        return cachedChannels
            ?.map { it.category }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?.sorted()
            ?.let { listOf("All") + it }
            ?: listOf("All")
    }

    // ──────────────── Cache Helpers ────────────────

    private fun loadFromDiskCache(ignoreExpiry: Boolean = false): List<Channel>? {
        return try {
            val file = File(context.cacheDir, CACHE_FILE)
            if (!file.exists()) return null

            if (!ignoreExpiry) {
                val age = System.currentTimeMillis() - file.lastModified()
                if (age > CACHE_EXPIRY_MS) return null
            }

            val json = file.readText()
            val type = object : TypeToken<List<Channel>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load disk cache: ${e.message}")
            null
        }
    }

    private fun saveToDiskCache(channels: List<Channel>) {
        try {
            val file = File(context.cacheDir, CACHE_FILE)
            file.writeText(gson.toJson(channels))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save disk cache: ${e.message}")
        }
    }

    private fun loadFavoriteIds(): Set<String> {
        return try {
            val file = File(context.filesDir, FAVORITES_FILE)
            if (!file.exists()) return emptySet()
            val type = object : TypeToken<Set<String>>() {}.type
            gson.fromJson(file.readText(), type) ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun saveFavoriteIds(ids: Set<String>) {
        try {
            val file = File(context.filesDir, FAVORITES_FILE)
            file.writeText(gson.toJson(ids))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save favorites: ${e.message}")
        }
    }

    private fun loadRecentlyWatched(): List<Channel> {
        return try {
            val file = File(context.filesDir, RECENTLY_WATCHED_FILE)
            if (!file.exists()) return emptyList()
            val type = object : TypeToken<List<Channel>>() {}.type
            gson.fromJson(file.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveRecentlyWatched(channels: List<Channel>) {
        try {
            val file = File(context.filesDir, RECENTLY_WATCHED_FILE)
            file.writeText(gson.toJson(channels))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save recently watched: ${e.message}")
        }
    }
}
