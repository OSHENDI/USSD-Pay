package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.PaymentScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var permissionsRequested = false
    private var appUpdateManager: AppUpdateManager? = null
    private val updateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            appUpdateManager?.completeUpdate()
        }
    }

    private val updateLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                // User declined or update failed
            }
        }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results[Manifest.permission.READ_PHONE_STATE] == true) {
                viewModel.loadSims(this)
            }
            if (results[Manifest.permission.CALL_PHONE] == true) {
                viewModel.checkBalanceOnColdStart(this)
            }
        }

    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val payload = result.data?.getStringExtra(QrScannerActivity.EXTRA_RESULT)
                if (payload != null) {
                    viewModel.setRecipientFromQr(payload, this)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.initialize(this)
        checkForAppUpdates()

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = state.isDarkMode) {
                PaymentScreen(
                    viewModel = viewModel,
                    onOpenScanner = {
                        val intent = Intent(this, QrScannerActivity::class.java)
                        scanLauncher.launch(intent)
                    }
                )
            }
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val hasPhoneStatePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

        if (hasCallPermission && hasPhoneStatePermission) {
            viewModel.loadSims(this)
            viewModel.checkBalanceOnColdStart(this)
        } else if (!permissionsRequested) {
            permissionsRequested = true
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    requestPermissionsLauncher.launch(
                        arrayOf(
                            Manifest.permission.CALL_PHONE,
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.READ_PHONE_NUMBERS
                        )
                    )
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }, 300)
        }
    }

    private fun checkForAppUpdates() {
        try {
            val manager = AppUpdateManagerFactory.create(this)
            appUpdateManager = manager
            manager.registerListener(updateListener)
            manager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    manager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                    )
                }
            }.addOnFailureListener {
                // Silently handle if Play Store is unavailable on debug/sideload builds
            }
        } catch (_: Throwable) {
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager?.appUpdateInfo?.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                appUpdateManager?.completeUpdate()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager?.unregisterListener(updateListener)
    }
}
