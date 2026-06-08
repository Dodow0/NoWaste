package com.nowaste.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nowaste.app.domain.AppTheme
import com.nowaste.app.notifications.ReminderScheduler
import com.nowaste.app.ui.FoodListUiState
import com.nowaste.app.ui.FoodViewModel
import com.nowaste.app.ui.NoWasteApp
import com.nowaste.app.ui.theme.NoWasteTheme

class MainActivity : ComponentActivity() {
    private var notificationPermissionGranted by mutableStateOf(false)

    private val foodViewModel: FoodViewModel by viewModels {
        FoodViewModel.Factory(
            repository = ServiceLocator.foodRepository(applicationContext),
            settings = ServiceLocator.appSettings(applicationContext),
            appContext = applicationContext,
        )
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationPermissionGranted = hasNotificationPermission()
            if (granted) {
                scheduleReminders()
                ReminderScheduler.runOnceNow(applicationContext)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationPermissionGranted = hasNotificationPermission()

        setContent {
            val uiState by foodViewModel.foodListUiState.collectAsStateWithLifecycle()
            val theme = (uiState as? FoodListUiState.Ready)?.settings?.theme ?: AppTheme.FOLLOW_SYSTEM

            NoWasteTheme(theme = theme) {
                NoWasteApp(
                    viewModel = foodViewModel,
                    notificationPermissionGranted = notificationPermissionGranted,
                    onRequestNotificationPermission = ::requestNotificationPermissionFromSettings,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        notificationPermissionGranted = hasNotificationPermission()
    }

    private fun requestNotificationPermissionFromSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionGranted = true
            scheduleReminders()
            ReminderScheduler.runOnceNow(applicationContext)
            return
        }

        if (hasNotificationPermission()) {
            notificationPermissionGranted = true
            scheduleReminders()
            ReminderScheduler.runOnceNow(applicationContext)
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun scheduleReminders() {
        ReminderScheduler.scheduleDaily(
            context = applicationContext,
            settings = ServiceLocator.appSettings(applicationContext),
        )
    }
}
