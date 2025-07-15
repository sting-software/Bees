package com.stingsoftware.pasika

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.stingsoftware.pasika.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        drawerLayout = binding.drawerLayout

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.todoListFragment),
            drawerLayout
        )
        NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration)

        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            binding.toolbar,
            R.string.action_open,
            R.string.action_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            var title = destination.label.toString()
            if (arguments != null) {
                val regex = "\\{(.*?)\\}".toRegex()
                title = regex.replace(title) {
                    val argName = it.groupValues[1]
                    arguments.get(argName)?.toString() ?: ""
                }
            }
            supportActionBar?.title = title
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.homeFragment) {
                navController.popBackStack(navController.graph.startDestinationId, false)
                return@setOnItemSelectedListener true
            }
            NavigationUI.onNavDestinationSelected(item, navController)
            true
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.menu.findItem(destination.id)?.isChecked = true
        }

        setupDrawerContent(binding.navigationView)
        applyWindowInsets()
    }

    private fun showExitConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_exit_app))
            .setMessage(getString(R.string.dialog_message_exit_app))
            .setPositiveButton(getString(R.string.action_exit)) { _, _ -> finishAffinity() }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            windowInsets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.navigationView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            windowInsets
        }
    }

    private fun setupDrawerContent(navigationView: NavigationView) {
        val darkModeSwitch =
            navigationView.menu.findItem(R.id.drawer_dark_mode).actionView as SwitchCompat
        val sharedPrefs = getSharedPreferences("app_settings", MODE_PRIVATE)

        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        darkModeSwitch.isChecked = when (currentNightMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val newMode =
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(newMode)
            with(sharedPrefs.edit()) {
                putInt("theme_mode", newMode)
                apply()
            }
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.drawer_language -> {
                    showLanguageSelectionDialog()
                    drawerLayout.closeDrawers()
                    true
                }

                R.id.drawer_exit_app -> {
                    showExitConfirmationDialog()
                    drawerLayout.closeDrawers()
                    true
                }

                else -> false
            }
        }
    }

    private fun showLanguageSelectionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_language_dialog))
            .setItems(arrayOf("English", "Українська")) { _, which ->
                val selectedLanguage = if (which == 0) "en" else "uk"
                setLocale(selectedLanguage)
                val sharedPrefs = getSharedPreferences("app_settings", MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    putString("language", selectedLanguage)
                    apply()
                }
                recreate()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(
            navController,
            appBarConfiguration
        ) || super.onSupportNavigateUp()
    }

    private fun setLocale(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}

