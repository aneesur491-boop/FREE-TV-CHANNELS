package com.freetv.player.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.freetv.player.activities.PlayerActivity
import com.freetv.player.adapters.ChannelAdapter
import com.freetv.player.databinding.FragmentChannelListBinding
import com.freetv.player.models.Channel
import com.freetv.player.models.PREDEFINED_CATEGORIES
import com.freetv.player.viewmodels.ChannelViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar

class ChannelListFragment : Fragment() {

    private var _binding: FragmentChannelListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChannelViewModel by activityViewModels()

    private lateinit var channelAdapter: ChannelAdapter

    companion object {
        const val MODE_ALL = "all"
        const val MODE_FAVORITES = "favorites"
        const val MODE_RECENT = "recent"
        private const val ARG_MODE = "mode"

        fun newInstance(mode: String = MODE_ALL): ChannelListFragment {
            return ChannelListFragment().apply {
                arguments = Bundle().apply { putString(ARG_MODE, mode) }
            }
        }
    }

    private val mode: String
        get() = arguments?.getString(ARG_MODE) ?: MODE_ALL

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChannelListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupCategoryChips()
        setupObservers()
        setupSwipeRefresh()

        // Show/hide filters based on mode
        binding.filterContainer.isVisible = mode == MODE_ALL
    }

    private fun setupRecyclerView() {
        channelAdapter = ChannelAdapter(
            onChannelClick = ::openPlayer,
            onFavoriteClick = ::toggleFavorite
        )

        binding.rvChannels.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = channelAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupCategoryChips() {
        if (mode != MODE_ALL) return

        binding.chipGroupCategories.removeAllViews()

        PREDEFINED_CATEGORIES.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category
                isCheckable = true
                isChecked = category == "All"
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) viewModel.filterByCategory(category)
                }
            }
            binding.chipGroupCategories.addView(chip)
        }
    }

    private fun setupObservers() {
        when (mode) {
            MODE_ALL -> {
                viewModel.channels.observe(viewLifecycleOwner) { channels ->
                    updateChannelList(channels)
                }
            }
            MODE_FAVORITES -> {
                viewModel.favoriteChannels.observe(viewLifecycleOwner) { channels ->
                    updateChannelList(channels)
                    binding.tvEmptyState.text = "No favorite channels yet.\nTap ♥ on any channel to add it here."
                }
                viewModel.loadFavorites()
            }
            MODE_RECENT -> {
                viewModel.recentlyWatched.observe(viewLifecycleOwner) { channels ->
                    updateChannelList(channels)
                    binding.tvEmptyState.text = "No recently watched channels."
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
            binding.progressBar.isVisible = isLoading && channelAdapter.currentList.isEmpty()
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction("Retry") { viewModel.refresh() }
                    .show()
                viewModel.clearError()
            }
        }

        viewModel.countries.observe(viewLifecycleOwner) { countries ->
            if (mode == MODE_ALL) setupCountrySpinner(countries)
        }
    }

    private fun updateChannelList(channels: List<Channel>) {
        channelAdapter.submitList(channels)
        val isEmpty = channels.isEmpty()
        binding.tvEmptyState.isVisible = isEmpty && viewModel.isLoading.value == false
        binding.rvChannels.isVisible = !isEmpty
    }

    private fun setupCountrySpinner(countries: List<String>) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            countries
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerCountry.adapter = adapter
        binding.spinnerCountry.setOnItemSelectedListener(object :
            android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                viewModel.filterByCountry(countries[pos])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            if (mode == MODE_ALL) {
                viewModel.refresh()
            } else {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun openPlayer(channel: Channel) {
        viewModel.onChannelWatched(channel)
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_CHANNEL, channel)
        }
        startActivity(intent)
    }

    private fun toggleFavorite(channel: Channel) {
        viewModel.toggleFavorite(channel)
        val msg = if (!channel.isFavorite) "Added to favorites" else "Removed from favorites"
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
