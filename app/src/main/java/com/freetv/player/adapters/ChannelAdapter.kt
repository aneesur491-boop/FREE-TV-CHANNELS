package com.freetv.player.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.freetv.player.R
import com.freetv.player.databinding.ItemChannelBinding
import com.freetv.player.models.Channel

class ChannelAdapter(
    private val onChannelClick: (Channel) -> Unit,
    private val onFavoriteClick: (Channel) -> Unit
) : ListAdapter<Channel, ChannelAdapter.ChannelViewHolder>(ChannelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChannelViewHolder(private val binding: ItemChannelBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: Channel) {
            binding.apply {
                tvChannelName.text = channel.name
                tvChannelCategory.text = channel.category.ifBlank { "General" }
                tvChannelCountry.text = channel.country.ifBlank { "International" }

                // Load logo with Glide
                Glide.with(ivChannelLogo.context)
                    .load(channel.logoUrl.ifBlank { null })
                    .placeholder(R.drawable.ic_tv_placeholder)
                    .error(R.drawable.ic_tv_placeholder)
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .circleCrop()
                    .into(ivChannelLogo)

                // Favorite icon
                val favIcon = if (channel.isFavorite) {
                    R.drawable.ic_favorite_filled
                } else {
                    R.drawable.ic_favorite_border
                }
                ibFavorite.setImageResource(favIcon)

                // Category chip color
                val categoryColor = getCategoryColor(channel.category)
                tvChannelCategory.setBackgroundColor(categoryColor)

                // Clicks
                root.setOnClickListener { onChannelClick(channel) }
                ibFavorite.setOnClickListener { onFavoriteClick(channel) }
            }
        }

        private fun getCategoryColor(category: String): Int {
            val context = binding.root.context
            return when (category.lowercase()) {
                "news" -> context.getColor(R.color.category_news)
                "sports" -> context.getColor(R.color.category_sports)
                "movies" -> context.getColor(R.color.category_movies)
                "kids" -> context.getColor(R.color.category_kids)
                "music" -> context.getColor(R.color.category_music)
                "documentary" -> context.getColor(R.color.category_documentary)
                else -> context.getColor(R.color.category_general)
            }
        }
    }

    class ChannelDiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(oldItem: Channel, newItem: Channel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Channel, newItem: Channel) = oldItem == newItem
    }
}
