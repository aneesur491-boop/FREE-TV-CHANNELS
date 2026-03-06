package com.freetv.player

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.freetv.player.databinding.ActivityMainBinding
import com.freetv.player.fragments.ChannelListFragment
import com.freetv.player.viewmodels.ChannelViewModel
import com.google.android.material.navigation.NavigationBarView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ChannelViewModel by viewModels()

    private val fragmentAll = ChannelListFragment.newInstance(ChannelListFragment.MODE_ALL)
    private val fragmentFavorites = ChannelListFragment.newInstance(ChannelListFragment.MODE_FAVORITES)
    private val fragmentRecent = ChannelListFragment.newInstance(ChannelListFragment.MODE_RECENT)

    private var activeFragment: Fragment = fragmentAll

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        setupFragments()
        setupBottomNavigation()
        setupSearch()
    }

    private fun setupFragments() {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragmentContainer, fragmentRecent, ChannelListFragment.MODE_RECENT)
            add(R.id.fragmentContainer, fragmentFavorites, ChannelListFragment.MODE_FAVORITES)
            add(R.id.fragmentContainer, fragmentAll, ChannelListFragment.MODE_ALL)
            hide(fragmentRecent)
            hide(fragmentFavorites)
        }.commit()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(NavigationBarView.OnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_channels -> showFragment(fragmentAll)
                R.id.nav_favorites -> {
                    viewModel.loadFavorites()
                    showFragment(fragmentFavorites)
                }
                R.id.nav_recent -> showFragment(fragmentRecent)
                else -> return@OnItemSelectedListener false
            }
            true
        })
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(fragment)
            .commit()
        activeFragment = fragment
    }

    private fun setupSearch() {
        binding.searchView.apply {
            queryHint = getString(R.string.search_channels)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false
                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.search(newText ?: "")
                    return true
                }
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_dark_mode -> {
                toggleDarkMode()
                true
            }
            R.id.action_refresh -> {
                viewModel.refresh()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleDarkMode() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val newMode = if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(newMode)
    }
}
