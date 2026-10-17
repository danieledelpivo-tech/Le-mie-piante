package com.lemiepiante

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lemiepiante.notifications.ReminderNotifications
import com.lemiepiante.ui.AppNavigation
import com.lemiepiante.ui.theme.LeMiePianteTheme

class MainActivity : ComponentActivity() {
    private var openPlantId by mutableStateOf<Long?>(null)

    private val requestNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReminderNotifications.canNotify(this)) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            LeMiePianteTheme {
                AppNavigation(openPlantId = openPlantId, onPlantOpened = { openPlantId = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_PLANT_ID, -1L) ?: -1L
        if (id > 0) openPlantId = id
    }

    companion object {
        const val EXTRA_PLANT_ID = "open_plant_id"
    }
}
