package com.freetv.player.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Channel(
    val id: String,
    val name: String,
    val logoUrl: String = "",
    val streamUrl: String,
    val country: String = "",
    val countryCode: String = "",
    val category: String = "",
    val language: String = "",
    val tvgId: String = "",
    var isFavorite: Boolean = false,
    var lastWatched: Long = 0L
) : Parcelable {

    fun matchesQuery(query: String): Boolean {
        val q = query.lowercase()
        return name.lowercase().contains(q) ||
               country.lowercase().contains(q) ||
               category.lowercase().contains(q)
    }

    fun matchesCategory(category: String): Boolean {
        if (category == "All") return true
        return this.category.lowercase().contains(category.lowercase())
    }

    fun matchesCountry(country: String): Boolean {
        if (country == "All") return true
        return this.country.lowercase() == country.lowercase() ||
               this.countryCode.lowercase() == country.lowercase()
    }
}

data class FilterState(
    val query: String = "",
    val category: String = "All",
    val country: String = "All"
)

val PREDEFINED_CATEGORIES = listOf("All", "News", "Sports", "Movies", "Kids", "Music", "Entertainment", "Documentary")
