package com.nowaste.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.nowaste.app.ui.FoodViewModel
import com.nowaste.app.ui.NoWasteApp
import com.nowaste.app.ui.theme.NoWasteTheme

class MainActivity : ComponentActivity() {
    private val foodViewModel: FoodViewModel by viewModels {
        FoodViewModel.Factory(
            repository = ServiceLocator.foodRepository(applicationContext),
            settings = ServiceLocator.appSettings(applicationContext),
            productLookupService = ServiceLocator.productLookupService(),
            appContext = applicationContext,
        )
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        setContent {
            NoWasteTheme {
                NoWasteApp(viewModel = foodViewModel)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
