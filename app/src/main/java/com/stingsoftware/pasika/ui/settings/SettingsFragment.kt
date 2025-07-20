package com.stingsoftware.pasika.ui.settings

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.title = getString(R.string.settings)

        val sharedPrefs =
            requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        // Set initial state of the switch based on the current theme
        setInitialSwitchState()

        // Set listener for theme switch
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            val newMode =
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(newMode)

            // Save the preference
            with(sharedPrefs.edit()) {
                putInt("theme_mode", newMode)
                apply()
            }
        }

        // Set listener for Exit App button
        binding.buttonExitApp.setOnClickListener {
            showExitConfirmationDialog()
        }
    }

    private fun setInitialSwitchState() {
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        binding.switchDarkMode.isChecked = when (currentNightMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> { // Handles MODE_NIGHT_FOLLOW_SYSTEM or MODE_NIGHT_AUTO_BATTERY
                val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                uiMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    private fun showExitConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.title_exit_app))
            .setMessage(getString(R.string.dialog_message_exit_app))
            .setPositiveButton(getString(R.string.action_exit)) { _, _ ->
                requireActivity().finishAffinity()
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}