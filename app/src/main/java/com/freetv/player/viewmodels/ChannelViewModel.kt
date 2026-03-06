package com.freetv.player.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.freetv.player.models.Channel
import com.freetv.player.models.FilterState
import com.freetv.player.repository.ChannelRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChannelViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChannelRepository(application)

    // All fetched channels (unfiltered)
    private val _allChannels = MutableLiveData<List<Channel>>(emptyList())

    // Filtered channels shown in UI
    private val _channels = MutableLiveData<List<Channel>>(emptyList())
    val channels: LiveData<List<Channel>> = _channels

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _countries = MutableLiveData<List<String>>(listOf("All"))
    val countries: LiveData<List<String>> = _countries

    private val _recentlyWatched = MutableLiveData<List<Channel>>(emptyList())
    val recentlyWatched: LiveData<List<Channel>> = _recentlyWatched

    private val _favoriteChannels = MutableLiveData<List<Channel>>(emptyList())
    val favoriteChannels: LiveData<List<Channel>> = _favoriteChannels

    private var filterState = FilterState()
    private var searchJob: Job? = null

    init {
        loadChannels()
        loadRecentlyWatched()
    }

    fun loadChannels(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = repository.fetchChannels(forceRefresh)) {
                is ChannelRepository.Result.Success -> {
                    _allChannels.value = result.data
                    _countries.value = repository.getAvailableCountries()
                    applyFilters()
                }
                is ChannelRepository.Result.Error -> {
                    _error.value = result.message
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun refresh() = loadChannels(forceRefresh = true)

    fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            filterState = filterState.copy(query = query)
            applyFilters()
        }
    }

    fun filterByCategory(category: String) {
        filterState = filterState.copy(category = category)
        applyFilters()
    }

    fun filterByCountry(country: String) {
        filterState = filterState.copy(country = country)
        applyFilters()
    }

    private fun applyFilters() {
        val all = _allChannels.value ?: return
        val filtered = all.filter { channel ->
            channel.matchesQuery(filterState.query) &&
            channel.matchesCategory(filterState.category) &&
            channel.matchesCountry(filterState.country)
        }
        _channels.value = filtered
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            val newState = repository.toggleFavorite(channel)
            // Update the lists
            _allChannels.value = _allChannels.value?.map { ch ->
                if (ch.id == channel.id) ch.copy(isFavorite = newState) else ch
            }
            applyFilters()
            loadFavorites()
        }
    }

    fun onChannelWatched(channel: Channel) {
        viewModelScope.launch {
            repository.addToRecentlyWatched(channel)
            loadRecentlyWatched()
        }
    }

    private fun loadRecentlyWatched() {
        viewModelScope.launch {
            _recentlyWatched.value = repository.getRecentlyWatched()
        }
    }

    fun loadFavorites() {
        viewModelScope.launch {
            _favoriteChannels.value = repository.getFavoriteChannels()
        }
    }

    fun clearError() {
        _error.value = null
    }
}
