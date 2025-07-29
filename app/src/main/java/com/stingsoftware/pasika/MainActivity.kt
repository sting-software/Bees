package com.stingsoftware.pasika

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
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

        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            binding.toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val topLevelDestinations = setOf(R.id.homeFragment, R.id.queenRearingFragment, R.id.todoListFragment)
        appBarConfiguration = AppBarConfiguration(topLevelDestinations, drawerLayout)
        NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration)

        // --- FIX: Replace the standard setup with a more explicit and robust implementation ---
        setupBottomNav()

        setupDrawerContent(binding.navigationView)
        applyWindowInsets()
    }

    private fun setupBottomNav() {
        // This listener handles the navigation logic when a bottom navigation item is selected.
        // The onNavDestinationSelected helper correctly navigates to the destination and,
        // importantly, pops the back stack to the start destination if the item is re-selected.
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            NavigationUI.onNavDestinationSelected(item, navController)
            true
        }

        // This listener ensures the correct bottom navigation item is highlighted when
        // navigating via other means (e.g., the back button or other in-app actions).
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val menu = binding.bottomNavigation.menu
            for (i in 0 until menu.size()) {
                val item = menu.getItem(i)
                if (item.itemId == destination.id) {
                    item.isChecked = true
                    break
                }
            }
        }
    }

    private fun showExitConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_exit_app))
            .setMessage(getString(R.string.dialog_message_exit_app))
            .setPositiveButton(getString(R.string.action_exit)) { _, _ ->
                finishAffinity()
            }
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

                R.id.drawer_email -> {
                    sendEmail()
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

                val appLocale = LocaleListCompat.forLanguageTags(selectedLanguage)
                AppCompatDelegate.setApplicationLocales(appLocale)

                val sharedPrefs = getSharedPreferences("app_settings", MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    putString("language_code", selectedLanguage)
                    apply()
                }
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish() // Close the old activity
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun sendEmail() {
        val recipient = getString(R.string.contact_email_address)
        val message = getString(R.string.email_message)

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri() // Only email apps should handle this
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_TEXT, message)
        }

        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.error_no_email_app), Toast.LENGTH_SHORT).show()
        }
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
}