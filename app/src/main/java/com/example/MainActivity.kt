package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.PaymentScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var permissionsRequested = false

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results[Manifest.permission.READ_PHONE_STATE] == true) {
                viewModel.loadSims(this)
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

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            viewModel.initialize(this)
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

        // Request required runtime permissions safely after the activity window has fully settled and attached.
        // This avoids race conditions in the window/input dispatcher focus pipeline on startup.
        if (!permissionsRequested) {
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
}
